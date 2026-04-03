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
}