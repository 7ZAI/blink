package com.blink.gateway.admin.constants;

/**
 * 配置值常量
 *
 * @author binblink
 */
public interface ConfigValueConstant {

    /**
     * 开关开启
     */
    Byte SWITCH_OPEN = 0;

    /**
     * 开关关闭
     */
    Byte SWITCH_CLOSE = 1;

    /**
     * 15分钟（毫秒）
     */
    Long LONG_MINUTES_15_OF_MILL = 15 * 60 * 1000L;

    /**
     * 15分钟
     */
    Long LONG_MINUTES_15 = 15L;

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
}