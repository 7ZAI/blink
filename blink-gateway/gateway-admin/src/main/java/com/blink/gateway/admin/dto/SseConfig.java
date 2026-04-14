package com.blink.gateway.admin.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE 连接配置
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
@Component
@ConfigurationProperties(prefix = "blink.gateway.sse")
public class SseConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * SSE 连接超时时间（毫秒）
     * 默认 30 分钟，保持长连接
     */
    private long connectionTimeout = 30 * 60_000L;

    /**
     * 心跳间隔（毫秒）
     * 默认 30 秒，保持连接活跃
     */
    private long heartbeatInterval = 30_000L;

    /**
     * 心跳超时次数
     * 超过此次数未收到响应则断开连接
     */
    private int heartbeatTimeoutCount = 3;

    /**
     * Redis 注册表过期时间（秒）
     * 应大于心跳间隔的 2 倍
     */
    private long registryTtl = 120L;

    /**
     * 单用户最大连接数（多标签页/多设备限制）
     * 默认 5，生产环境建议 5，开发环境可设为 10
     */
    private int maxConnectionsPerUser = 5;

    /**
     * 实例最大总连接数
     */
    private int maxTotalConnections = 1000;

    // ==================== 静态常量（供旧代码兼容） ====================

    /**
     * SSE 连接超时时间（毫秒）- 静态常量
     */
    public static final long CONNECTION_TIMEOUT = 30 * 60_000L;

    /**
     * 心跳间隔（毫秒）- 静态常量
     */
    public static final long HEARTBEAT_INTERVAL = 30_000L;

    /**
     * 心跳超时次数 - 静态常量
     */
    public static final int HEARTBEAT_TIMEOUT_COUNT = 3;

    /**
     * Redis 注册表过期时间（秒）- 静态常量
     */
    public static final long REGISTRY_TTL = 120L;

    /**
     * 实例最大总连接数 - 静态常量
     */
    public static final int MAX_TOTAL_CONNECTIONS = 1000;
}
