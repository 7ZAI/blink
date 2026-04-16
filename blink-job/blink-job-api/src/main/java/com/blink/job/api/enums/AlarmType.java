package com.blink.job.api.enums;

/**
 * 告警类型枚举
 *
 * @author binblink
 */
public enum AlarmType {

    FAILURE("执行失败"),
    TIMEOUT("执行超时"),
    RETRY_EXHAUSTED("重试耗尽");

    private final String desc;

    AlarmType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
