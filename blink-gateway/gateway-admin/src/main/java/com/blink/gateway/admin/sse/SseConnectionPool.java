package com.blink.gateway.admin.sse;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE连接池管理器
 * 支持用户多标签页连接
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Component
public class SseConnectionPool {

    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    /**
     * 创建SSE连接
     */
    public SseEmitter createConnection() {
        Integer userId = StpUtil.getLoginIdAsInt();

        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onCompletion(() -> {
            log.info("[SSE] 连接完成, userId: {}", userId);
            remove(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时, userId: {}", userId);
            remove(userId, emitter);
        });

        emitter.onError(e -> {
            log.error("[SSE] 连接异常, userId: {}", userId, e);
            remove(userId, emitter);
        });

        add(userId, emitter);
        log.info("[SSE] 新连接建立, userId: {}, 当前连接数: {}", userId, getUserConnectionCount(userId));

        return emitter;
    }

    /**
     * 添加连接
     */
    private void add(Integer userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /**
     * 移除连接
     * 使用 computeIfPresent 保证原子性操作
     */
    private void remove(Integer userId, SseEmitter emitter) {
        userConnections.computeIfPresent(userId, (k, connections) -> {
            connections.remove(emitter);
            return connections.isEmpty() ? null : connections;
        });
    }

    /**
     * 获取用户连接数
     */
    public int getUserConnectionCount(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * 推送消息给指定用户
     */
    public void sendToUser(Integer userId, NotificationMsg msg) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        if (connections == null || connections.isEmpty()) {
            log.debug("[SSE] 用户无连接, userId: {}", userId);
            return;
        }

        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(msg));
            } catch (IOException e) {
                log.error("[SSE] 推送失败, userId: {}", userId, e);
                remove(userId, emitter);
            }
        }
        log.info("[SSE] 推送成功, userId: {}, notificationId: {}", userId, msg.getNotificationId());
    }

    /**
     * 推送广播消息给所有连接
     */
    public void broadcast(NotificationMsg msg) {
        List<Integer> userIds = new ArrayList<>(userConnections.keySet());
        for (Integer userId : userIds) {
            sendToUser(userId, msg);
        }
        log.info("[SSE] 广播完成, notificationId: {}, 接收用户数: {}", msg.getNotificationId(), userIds.size());
    }

    /**
     * 检查用户是否有连接
     */
    public boolean hasConnection(Integer userId) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        return connections != null && !connections.isEmpty();
    }
}