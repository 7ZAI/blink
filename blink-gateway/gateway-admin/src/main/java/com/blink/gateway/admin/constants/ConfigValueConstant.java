package com.blink.gateway.admin.constants;

/**
 * 配置值常量
 *
 * 注意：开关状态常量请使用 CommonConstants.SWITCH_OPEN/SWITCH_CLOSE
 * 时间常量请使用 CommonConstants.LONG_MINUTES_15_OF_MILL/LONG_MINUTES_15
 *
 * @author binblink
 */
public interface ConfigValueConstant {

    // ============ 实例状态常量 ============

    /**
     * 实例状态 - 在线
     */
    Byte INSTANCE_STATUS_ONLINE = 0;

    /**
     * 实例状态 - 离线（注册中心无此实例）
     */
    Byte INSTANCE_STATUS_OFFLINE = 1;

    /**
     * 实例状态 - 下线（手动操作）
     */
    Byte INSTANCE_STATUS_SHUTDOWN = 2;

    /**
     * 实例状态 - 排空（优雅下线进行中）
     */
    Byte INSTANCE_STATUS_DRAINING = 3;

    // ============ 流量排空配置常量 ============

    /**
     * 默认流量排空等待时间（秒）
     */
    Integer DEFAULT_DRAIN_WAIT_SECONDS = 30;

    /**
     * 健康检查重试次数
     */
    Integer HEALTH_CHECK_RETRIES = 3;

    // ============ 超时时间常量 ============

    /**
     * 健康检查请求超时时间（秒）
     */
    Integer HEALTH_CHECK_TIMEOUT_SECONDS = 5;

    // ============ 健康状态常量 ============

    /**
     * 健康状态 - 正常
     */
    String HEALTH_STATUS_UP = "UP";

    /**
     * 健康状态 - 异常
     */
    String HEALTH_STATUS_DOWN = "DOWN";

    /**
     * 健康状态 - 离线
     */
    String HEALTH_STATUS_OFFLINE = "OFFLINE";

    // ============ 熔断器状态常量 ============

    /**
     * 熔断器状态 - 关闭（正常）
     */
    String CIRCUIT_BREAKER_CLOSED = "CLOSED";

    /**
     * 熔断器状态 - 打开（熔断）
     */
    String CIRCUIT_BREAKER_OPEN = "OPEN";

    /**
     * 熔断器状态 - 半开（试探）
     */
    String CIRCUIT_BREAKER_HALF_OPEN = "HALF_OPEN";

    // ============ Nacos 配置常量 ============

    /**
     * Nacos 配置获取超时时间（毫秒）
     */
    Long NACOS_CONFIG_TIMEOUT_MS = 5000L;

    // ============ 路由历史清理常量 ============

    /**
     * 路由历史记录保留天数
     */
    Integer ROUTE_HISTORY_KEEP_DAYS = 90;

    // ============ 指标采集配置常量 ============

    /**
     * 实例指标 TTL（秒）
     */
    Integer METRICS_TTL_SECONDS = 90;
}