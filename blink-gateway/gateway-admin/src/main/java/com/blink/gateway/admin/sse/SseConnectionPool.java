package com.blink.gateway.admin.sse;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.SseConfig;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE连接池管理器
 * 支持用户多标签页连接，支持多实例部署
 *
 * 功能特性：
 * - 单用户连接数限制（默认5个）
 * - 总连接数限制（默认1000个）
 * - 心跳保活机制
 * - 多实例部署支持
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
     * 总连接数计数器
     */
    private final AtomicInteger totalConnectionCount = new AtomicInteger(0);

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseInstanceIdentifier instanceIdentifier;

    /**
     * 创建SSE连接
     *
     * @return SSE连接对象，如果超过限制返回 null
     */
    public SseEmitter createConnection() {
        Integer userId = StpUtil.getLoginIdAsInt();
        String instanceId = instanceIdentifier.getInstanceId();

        // 检查单用户连接数限制
        int currentUserCount = getUserConnectionCount(userId);
        if (currentUserCount >= SseConfig.MAX_CONNECTIONS_PER_USER) {
            log.warn("[SSE] 单用户连接数超限，拒绝连接 | userId: {}, 当前连接数: {}, 上限: {}",
                    userId, currentUserCount, SseConfig.MAX_CONNECTIONS_PER_USER);
            return null;
        }

        // 检查总连接数限制
        int currentTotal = totalConnectionCount.get();
        if (currentTotal >= SseConfig.MAX_TOTAL_CONNECTIONS) {
            log.warn("[SSE] 总连接数超限，拒绝连接 | 当前总连接数: {}, 上限: {}",
                    currentTotal, SseConfig.MAX_TOTAL_CONNECTIONS);
            return null;
        }

        // 注册到Redis连接注册表
        registerConnection(userId, instanceId);

        SseEmitter emitter = new SseEmitter(SseConfig.CONNECTION_TIMEOUT);

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
        log.info("[SSE] 新连接建立 | userId: {}, instanceId: {}, 当前用户连接数: {}, 总连接数: {}",
            userId, instanceId, getUserConnectionCount(userId), getTotalConnectionCount());

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
        redisClient.expire(registryKey, SseConfig.REGISTRY_TTL);
    }

    /**
     * 添加连接到本地存储
     *
     * @param userId  用户ID
     * @param emitter SSE连接对象
     */
    private void add(Integer userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        totalConnectionCount.incrementAndGet();
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
            if (connections.remove(emitter)) {
                totalConnectionCount.decrementAndGet();
            }
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
     * 获取总连接数
     *
     * @return 总连接数
     */
    public int getTotalConnectionCount() {
        return totalConnectionCount.get();
    }

    /**
     * 推送消息给指定用户
     * 通过Redis注册表判断是否应由本实例处理
     *
     * @param userId 用户ID
     * @param msg    SSE消息
     */
    public <T> void sendToUser(Integer userId, SseMessage<T> msg) {
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
                    .name(msg.getType())
                    .data(msg));
            } catch (IOException e) {
                log.error("[SSE] 推送失败 | userId: {}", userId, e);
                remove(userId, emitter);
            }
        }
        log.debug("[SSE] 推送成功 | userId: {}, type: {}", userId, msg.getType());
    }

    /**
     * 推送广播消息给所有连接
     *
     * @param msg SSE消息
     */
    public <T> void broadcast(SseMessage<T> msg) {
        // 遍历本地连接进行推送
        userConnections.forEach((userId, connections) -> sendToUser(userId, msg));
        log.debug("[SSE] 广播完成 | type: {}, 本地用户数: {}", msg.getType(), userConnections.size());
    }

    /**
     * 推送通知消息给指定用户（兼容旧接口）
     *
     * @param userId 用户ID
     * @param msg    通知消息
     */
    public void sendToUser(Integer userId, NotificationMsg msg) {
        SseMessage<NotificationPayload> sseMsg = SseMessage.notification(NotificationPayload.from(msg));
        sendToUser(userId, sseMsg);
    }

    /**
     * 推送广播通知消息（兼容旧接口）
     *
     * @param msg 通知消息
     */
    public void broadcast(NotificationMsg msg) {
        SseMessage<NotificationPayload> sseMsg = SseMessage.notification(NotificationPayload.from(msg));
        broadcast(sseMsg);
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
     * 每30秒执行一次：发送心跳消息、刷新Redis注册表TTL
     */
    @Scheduled(fixedRate = SseConfig.HEARTBEAT_INTERVAL)
    public void heartbeat() {
        // 获取本地所有在线用户
        Set<Integer> onlineUsers = userConnections.keySet();
        if (CollUtil.isEmpty(onlineUsers)) {
            return;
        }

        String currentInstance = instanceIdentifier.getInstanceId();
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;

        // 向所有本地连接发送心跳
        SseMessage<String> heartbeatMsg = SseMessage.heartbeat();
        for (Map.Entry<Integer, CopyOnWriteArrayList<SseEmitter>> entry : userConnections.entrySet()) {
            Integer userId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name(SseMessageType.HEARTBEAT).data(heartbeatMsg));
                } catch (IOException e) {
                    log.warn("[SSE] 心跳发送失败，移除连接 | userId: {}", userId);
                    remove(userId, emitter);
                }
            }
        }

        // 刷新Redis注册表TTL
        redisClient.expire(registryKey, SseConfig.REGISTRY_TTL);

        // 更新实例心跳
        String heartbeatKey = RedisKeyConstant.SSE_INSTANCE_HEARTBEAT + currentInstance;
        redisClient.setEx(heartbeatKey, "alive", SseConfig.REGISTRY_TTL);

        log.debug("[SSE] 心跳完成 | instanceId: {}, 在线用户数: {}, 总连接数: {}",
                currentInstance, onlineUsers.size(), getTotalConnectionCount());
    }
}
