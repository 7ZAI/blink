package com.blink.job.api.enums;

/**
 * 任务状态枚举
 *
 * @author binblink
 */
public enum JobStatus {

    PAUSED(0, "暂停"),
    NORMAL(1, "正常"),
    DELETED(2, "已删除");

    private final int code;
    private final String desc;

    JobStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static JobStatus fromCode(int code) {
        for (JobStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
