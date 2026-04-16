package com.blink.gateway.admin.constants;

/**
 * 熔断器状态常量
 *
 * 注意：Redis Key 常量请使用 RedisKeyConstant.CIRCUIT_BREAKER_KEY_PREFIX 等
 * TTL 配置值请使用 RedisKeyConstant.CIRCUIT_BREAKER_TTL_SECONDS 等
 *
 * @author binblink
 * @since 2026-04-16
 */
public interface CircuitBreakerConstant {

    /**
     * 状态：关闭（正常）
     */
    String STATE_CLOSED = "CLOSED";

    /**
     * 状态：开启（熔断）
     */
    String STATE_OPEN = "OPEN";

    /**
     * 状态：半开（探测）
     */
    String STATE_HALF_OPEN = "HALF_OPEN";

    /**
     * 默认历史记录查询数量
     */
    int DEFAULT_HISTORY_LIMIT = 20;
}
