package com.blink.gateway.admin.sse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.SseConfig;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE连接池管理器
 *
 * 连接标识策略：
 * - connectionKey = userId + token（如 "21:abc123..."）
 * - 同一 connectionKey 只允许一个连接（替换而非累积）
 * - 多设备登录：同一 userId 不同 token = 不同 connectionKey = 多个连接
 * - 页面刷新/重连：相同 token = 相同 connectionKey = 替换旧连接
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Component
public class SseConnectionPool {

    /**
     * 连接存储：Map<connectionKey, ConnectionWrapper>
     */
    private final Map<String, ConnectionWrapper> connections = new ConcurrentHashMap<>();

    /**
     * 用户连接索引：用于广播时快速查找
     */
    private final Map<Integer, Set<String>> userConnectionIndex = new ConcurrentHashMap<>();

    /**
     * 总连接数计数
     */
    private final AtomicInteger totalConnectionCount = new AtomicInteger(0);

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseInstanceIdentifier instanceIdentifier;

    /**
     * 连接包装类
     */
    @Getter
    private static class ConnectionWrapper {
        private final Integer userId;
        private final String token;
        private final SseEmitter emitter;
        private final String instanceId;
        private final long createTime;

        public ConnectionWrapper(Integer userId, String token, SseEmitter emitter, String instanceId) {
            this.userId = userId;
            this.token = token;
            this.emitter = emitter;
            this.instanceId = instanceId;
            this.createTime = System.currentTimeMillis();
        }
    }

    /**
     * 创建 SSE 连接
     *
     * @param connectionKey userId:token 组合
     * @param userId 用户ID
     * @return SSE连接对象
     */
    public SseEmitter createConnection(String connectionKey, Integer userId) {
        String instanceId = instanceIdentifier.getInstanceId();

        // 解析 token（用于日志脱敏）
        String token = extractToken(connectionKey);

        // 关键：相同 connectionKey 替换旧连接，而非累积
        ConnectionWrapper existing = connections.get(connectionKey);
        if (existing != null) {
            log.info("[SSE] 替换旧连接 | connectionKey: {}, userId: {}", maskKey(connectionKey), userId);
            forceRemove(connectionKey);
        }

        // 注册到 Redis
        registerConnection(userId, instanceId);

        // 创建 emitter（30分钟超时）
        SseEmitter emitter = new SseEmitter(SseConfig.CONNECTION_TIMEOUT);

        // 设置回调
        emitter.onCompletion(() -> {
            log.info("[SSE] 连接完成 | connectionKey: {}", maskKey(connectionKey));
            remove(connectionKey);
        });
        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时 | connectionKey: {}", maskKey(connectionKey));
            remove(connectionKey);
        });
        emitter.onError(e -> {
            log.error("[SSE] 连接异常 | connectionKey: {}", maskKey(connectionKey), e);
            remove(connectionKey);
        });

        // 存储
        ConnectionWrapper wrapper = new ConnectionWrapper(userId, token, emitter, instanceId);
        connections.put(connectionKey, wrapper);
        userConnectionIndex.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(connectionKey);
        totalConnectionCount.incrementAndGet();

        log.info("[SSE] 新连接建立 | userId: {}, 用户连接数: {}, 总连接数: {}",
                userId, getUserConnectionCount(userId), getTotalConnectionCount());

        return emitter;
    }

    /**
     * 从 connectionKey 提取 token
     */
    private String extractToken(String connectionKey) {
        if (StrUtil.isBlank(connectionKey)) return "";
        int idx = connectionKey.indexOf(':');
        return idx > 0 ? connectionKey.substring(idx + 1) : connectionKey;
    }

    /**
     * 连接标识脱敏
     */
    private String maskKey(String connectionKey) {
        if (connectionKey == null) return null;
        int idx = connectionKey.indexOf(':');
        if (idx > 0 && connectionKey.length() > idx + 8) {
            return connectionKey.substring(0, idx + 8) + "...";
        }
        return connectionKey;
    }

    /**
     * 强制移除连接（主动断开）
     */
    private void forceRemove(String connectionKey) {
        ConnectionWrapper wrapper = connections.remove(connectionKey);
        if (wrapper == null) return;

        Integer userId = wrapper.getUserId();
        userConnectionIndex.computeIfPresent(userId, (k, keys) -> {
            keys.remove(connectionKey);
            if (keys.isEmpty()) {
                unregisterConnection(userId);
                return null;
            }
            return keys;
        });
        totalConnectionCount.decrementAndGet();
    }

    /**
     * 正常移除连接（回调触发）
     */
    private void remove(String connectionKey) {
        forceRemove(connectionKey);
    }

    /**
     * 注册到 Redis
     */
    private void registerConnection(Integer userId, String instanceId) {
        String registryKey = RedisKeyConstant.SSE_CONNECTION_REGISTRY;
        redisClient.hPutField(registryKey, String.valueOf(userId), instanceId);
        redisClient.expire(registryKey, SseConfig.REGISTRY_TTL);
    }

    /**
     * 从 Redis 移除
     */
    private void unregisterConnection(Integer userId) {
        redisClient.hDeleteFields(RedisKeyConstant.SSE_CONNECTION_REGISTRY, String.valueOf(userId));
        log.info("[SSE] Redis 注册已移除 | userId: {}", userId);
    }

    /**
     * 获取用户连接数
     */
    public int getUserConnectionCount(Integer userId) {
        Set<String> keys = userConnectionIndex.get(userId);
        return CollUtil.isEmpty(keys) ? 0 : keys.size();
    }

    /**
     * 获取总连接数
     */
    public int getTotalConnectionCount() {
        return totalConnectionCount.get();
    }

    /**
     * 推送给指定用户
     */
    public <T> void sendToUser(Integer userId, SseMessage<T> msg) {
        Set<String> keys = userConnectionIndex.get(userId);
        if (CollUtil.isEmpty(keys)) return;

        for (String key : keys) {
            ConnectionWrapper wrapper = connections.get(key);
            if (wrapper == null) continue;

            try {
                wrapper.getEmitter().send(SseEmitter.event().name(msg.getType()).data(msg));
            } catch (IOException e) {
                log.error("[SSE] 推送失败 | connectionKey: {}", maskKey(key), e);
                forceRemove(key);
            }
        }
        log.debug("[SSE] 推送成功 | userId: {}, type: {}", userId, msg.getType());
    }

    /**
     * 广播消息
     */
    public <T> void broadcast(SseMessage<T> msg) {
        userConnectionIndex.keySet().forEach(userId -> sendToUser(userId, msg));
        log.debug("[SSE] 广播完成 | type: {}, 用户数: {}", msg.getType(), userConnectionIndex.size());
    }

    // ==================== 兼容旧接口 ====================

    public void sendToUser(Integer userId, NotificationMsg msg) {
        sendToUser(userId, SseMessage.notification(NotificationPayload.from(msg)));
    }

    public void broadcast(NotificationMsg msg) {
        broadcast(SseMessage.notification(NotificationPayload.from(msg)));
    }

    public boolean hasConnection(Integer userId) {
        return CollUtil.isNotEmpty(userConnectionIndex.get(userId));
    }

    // ==================== 心跳保活 ====================

    @Scheduled(fixedRate = SseConfig.HEARTBEAT_INTERVAL)
    public void heartbeat() {
        if (connections.isEmpty()) return;

        String currentInstance = instanceIdentifier.getInstanceId();
        SseMessage<String> heartbeatMsg = SseMessage.heartbeat();

        for (String key : connections.keySet()) {
            ConnectionWrapper wrapper = connections.get(key);
            if (wrapper == null) continue;

            try {
                wrapper.getEmitter().send(SseEmitter.event()
                    .name(SseMessageType.HEARTBEAT)
                    .data(heartbeatMsg));
            } catch (IOException e) {
                log.warn("[SSE] 心跳失败，移除连接 | connectionKey: {}", maskKey(key));
                forceRemove(key);
            }
        }

        // 刷新 Redis TTL
        redisClient.expire(RedisKeyConstant.SSE_CONNECTION_REGISTRY, SseConfig.REGISTRY_TTL);
        redisClient.setEx(RedisKeyConstant.SSE_INSTANCE_HEARTBEAT + currentInstance, "alive", SseConfig.REGISTRY_TTL);

        log.debug("[SSE] 心跳完成 | 用户数: {}, 连接数: {}", userConnectionIndex.size(), getTotalConnectionCount());
    }
}