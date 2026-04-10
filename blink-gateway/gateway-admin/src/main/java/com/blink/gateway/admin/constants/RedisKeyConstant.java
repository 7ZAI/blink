package com.blink.gateway.admin.constants;

import com.blink.framework.common.constrant.RedisCacheKeyConstant;

/**
 * Redis Key 常量
 *
 * @author binblink
 */
public interface RedisKeyConstant {

    /**
     * blink前缀
     */
    String BLINK_PREFIX = "blink:gateway:admin:";

    /**
     * 渠道信息Redis Key前缀
     */
    String CHANNEL_INFO = BLINK_PREFIX + "channel:info:";

    /**
     * 渠道信息Redis Key
     */
    String CHANNEL_INFO_KEY = BLINK_PREFIX + "channel:";

    /**
     * 网关动态路由Redis Key
     */
    String GATEWAY_DYNAMIC_ROUTES = BLINK_PREFIX + "gateway:routes";

    /**
     * gateway同步 stream key（使用共享常量，确保与 gateway-reactive 一致）
     */
    String GATEWAY_STREAM_EVENT = RedisCacheKeyConstant.GATEWAY_STREAM_EVENT;

    // ==================== 网关监控指标 ====================

    /**
     * 网关实例指标 Redis Key 前缀
     */
    String GATEWAY_METRICS_PREFIX = "blink:gateway:metrics:";

    /**
     * 网关指标汇总 Redis Key
     */
    String GATEWAY_METRICS_SUMMARY = "blink:gateway:metrics:summary";

    /**
     * 网关指标采集分布式锁 Redis Key
     */
    String GATEWAY_METRICS_COLLECT_LOCK = "blink:gateway:metrics:collect:lock";

    // ==================== 消息通知相关 ====================

    /**
     * 消息通知 Pub/Sub Channel
     */
    String NOTIFICATION_CHANNEL = BLINK_PREFIX + ":notification:channel";

    /**
     * 用户未读消息计数 Redis Key 前缀
     */
    String NOTIFICATION_USER_UNREAD = BLINK_PREFIX + ":notification:unread:";

    // ==================== SSE 连接管理 ====================

    /**
     * SSE 连接注册表 - userId -> instanceId 映射
     */
    String SSE_CONNECTION_REGISTRY = BLINK_PREFIX + ":sse:connections";

    /**
     * SSE 实例心跳前缀 - 记录活跃实例
     */
    String SSE_INSTANCE_HEARTBEAT = BLINK_PREFIX + ":sse:instance:";

    // ==================== Stream 死信队列 ====================

    /**
     * Stream 死信队列 Key
     */
    String STREAM_DEAD_LETTER_QUEUE = BLINK_PREFIX + ":stream:dead-letter";
}