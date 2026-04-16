package com.blink.gateway.admin.constants;

/**
 * 熔断器相关常量
 *
 * @author binblink
 * @since 2026-04-16
 */
public interface CircuitBreakerConstant {

    /**
     * Redis Key 前缀：熔断器指标
     */
    String CB_KEY_PREFIX = "blink:gateway:circuitbreaker:";

    /**
     * Redis Key 前缀：状态转换历史
     */
    String CB_HISTORY_KEY_PREFIX = "blink:gateway:circuitbreaker:history:";

    /**
     * 熔断器指标 TTL（秒）
     */
    int CB_TTL_SECONDS = 90;

    /**
     * 历史记录 TTL（秒）- 7 天
     */
    int HISTORY_TTL_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 默认历史记录查询数量
     */
    int DEFAULT_HISTORY_LIMIT = 20;

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
     * 实例列表 Key（复用 RedisKeyConstant）
     */
    String INSTANCE_LIST_KEY = "blink:gateway:instance:list";
}
