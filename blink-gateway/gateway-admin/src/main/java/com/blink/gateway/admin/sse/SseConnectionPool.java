package com.blink.gateway.admin.sse;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE连接池管理器
 * 支持用户多标签页连接，支持多实例部署
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Component
public class SseConnectionPool {

    /**
     * 本地连接存储
     */
    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    /**
     * SSE连接超时时间（5分钟）
     */
    private static final long SSE_TIMEOUT = 5 * 60_000L;

    /**
     * Redis注册表过期时间（秒）
     */
    private static final long REGISTRY_TTL = 300L;

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseInstanceIdentifier instanceIdentifier;

    /**
     * 创建SSE连接
     *
     * @return SSE连接对象
     */
    public SseEmitter createConnection() {
        Integer userId = StpUtil.getLoginIdAsInt();
        String instanceId = instanceIdentifier.getInstanceId();

        // 注册到Redis连接注册表
        registerConnection(userId, instanceId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.info("[SSE] 连接完成 | userId: {}", userId);
            remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时 | userId: {}", userId);
            remove(userId, emitter);
        });

        emitter.onError(e -> {
            log.error("[SSE] 连接异常 | userId: {}", userId, e);
            remove(userId, emitter);
        });

        add(userId, emitter);
        log.info("[SSE] 新连接建立 | userId: {}, instanceId: {}, 当前连接数: {}",
            userId, instanceId, getUserConnectionCount(userId));

        return emitter;
    }

    /**
     * 注册连接到Redis
     *
     * @param userId     用户ID
     * @param instanceId 实例ID
     */
    private void registerConnection(Integer userId, String instanceId) {
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;
        redisClient.hPutField(registryKey, String.valueOf(userId), instanceId);
        redisClient.expire(registryKey, REGISTRY_TTL);
    }

    /**
     * 添加连接到本地存储
     *
     * @param userId  用户ID
     * @param emitter SSE连接对象
     */
    private void add(Integer userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /**
     * 移除连接
     * 使用 computeIfPresent 保证原子性操作
     *
     * @param userId  用户ID
     * @param emitter SSE连接对象
     */
    private void remove(Integer userId, SseEmitter emitter) {
        userConnections.computeIfPresent(userId, (k, connections) -> {
            connections.remove(emitter);
            // 如果用户无任何连接，从Redis移除注册
            if (connections.isEmpty()) {
                unregisterConnection(userId);
                return null;
            }
            return connections;
        });
    }

    /**
     * 从Redis移除连接注册
     *
     * @param userId 用户ID
     */
    private void unregisterConnection(Integer userId) {
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;
        redisClient.hDeleteFields(registryKey, String.valueOf(userId));
        log.info("[SSE] 连接注册已移除 | userId: {}", userId);
    }

    /**
     * 获取用户连接数
     *
     * @param userId 用户ID
     * @return 连接数量
     */
    public int getUserConnectionCount(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return CollUtil.isEmpty(connections) ? 0 : connections.size();
    }

    /**
     * 推送消息给指定用户
     * 通过Redis注册表判断是否应由本实例处理
     *
     * @param userId 用户ID
     * @param msg    通知消息
     */
    public void sendToUser(Integer userId, NotificationMsg msg) {
        // 检查Redis注册表，判断消息是否应由本实例处理
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;
        Object registeredInstance = redisClient.hGetField(registryKey, String.valueOf(userId));

        if (ObjectUtil.isNull(registeredInstance)) {
            log.debug("[SSE] 用户无在线连接 | userId: {}", userId);
            return;
        }

        String targetInstance = registeredInstance.toString();
        String currentInstance = instanceIdentifier.getInstanceId();

        // 只有注册在本实例的用户才处理推送
        if (!targetInstance.equals(currentInstance)) {
            log.debug("[SSE] 用户连接在其他实例 | userId: {}, targetInstance: {}", userId, targetInstance);
            return;
        }

        // 执行本地推送
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        if (CollUtil.isEmpty(connections)) {
            log.warn("[SSE] 本地连接不存在但Redis有注册 | userId: {}", userId);
            // 清理脏数据
            unregisterConnection(userId);
            return;
        }

        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(msg));
            } catch (IOException e) {
                log.error("[SSE] 推送失败 | userId: {}", userId, e);
                remove(userId, emitter);
            }
        }
        log.info("[SSE] 推送成功 | userId: {}, notificationId: {}", userId, msg.getNotificationId());
    }

    /**
     * 推送广播消息给所有连接
     *
     * @param msg 通知消息
     */
    public void broadcast(NotificationMsg msg) {
        // 遍历本地连接进行推送
        userConnections.forEach((userId, connections) -> sendToUser(userId, msg));
        log.info("[SSE] 广播完成 | notificationId: {}, 本地用户数: {}",
            msg.getNotificationId(), userConnections.size());
    }

    /**
     * 检查用户是否有连接
     *
     * @param userId 用户ID
     * @return 是否有连接
     */
    public boolean hasConnection(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return CollUtil.isNotEmpty(connections);
    }

    /**
     * 心跳保活定时任务
     * 每60秒执行一次：发送心跳消息、刷新Redis注册表TTL
     */
    @Scheduled(fixedRate = 60_000)
    public void heartbeat() {
        // 获取本地所有在线用户
        Set<Integer> onlineUsers = userConnections.keySet();
        if (CollUtil.isEmpty(onlineUsers)) {
            return;
        }

        String currentInstance = instanceIdentifier.getInstanceId();
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;

        // 向所有本地连接发送心跳
        for (Map.Entry<Integer, CopyOnWriteArrayList<SseEmitter>> entry : userConnections.entrySet()) {
            Integer userId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException e) {
                    log.warn("[SSE] 心跳发送失败，移除连接 | userId: {}", userId);
                    remove(userId, emitter);
                }
            }
        }

        // 刷新Redis注册表TTL
        redisClient.expire(registryKey, REGISTRY_TTL);

        // 更新实例心跳
        String heartbeatKey = RedisKeyConstant.SSE_INSTANCE_HEARTBEAT + currentInstance;
        redisClient.setEx(heartbeatKey, "alive", REGISTRY_TTL);

        log.debug("[SSE] 心跳完成 | instanceId: {}, 在线用户数: {}", currentInstance, onlineUsers.size());
    }
}