package com.blink.gateway.admin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE 连接配置
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class SseConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * SSE 连接超时时间（毫秒）
     * 默认 30 分钟，保持长连接
     */
    public static final long CONNECTION_TIMEOUT = 30 * 60_000L;

    /**
     * 心跳间隔（毫秒）
     * 默认 30 秒，保持连接活跃
     */
    public static final long HEARTBEAT_INTERVAL = 30_000L;

    /**
     * 心跳超时次数
     * 超过此次数未收到响应则断开连接
     */
    public static final int HEARTBEAT_TIMEOUT_COUNT = 3;

    /**
     * Redis 注册表过期时间（秒）
     * 应大于心跳间隔的 2 倍
     */
    public static final long REGISTRY_TTL = 120L;

    /**
     * 单用户最大连接数（多标签页限制）
     * 开发环境设置为 10，避免频繁触发限制
     */
    public static final int MAX_CONNECTIONS_PER_USER = 10;

    /**
     * 实例最大总连接数
     */
    public static final int MAX_TOTAL_CONNECTIONS = 1000;
}
