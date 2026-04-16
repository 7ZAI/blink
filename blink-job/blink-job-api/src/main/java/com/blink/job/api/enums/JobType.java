package com.blink.job.api.enums;

/**
 * 任务类型枚举
 *
 * @author binblink
 */
public enum JobType {

    METHOD(1, "注解方法"),
    BEAN(2, "接口实现");

    private final int code;
    private final String desc;

    JobType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static JobType fromCode(int code) {
        for (JobType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
