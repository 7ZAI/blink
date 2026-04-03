package com.blink.log.constant;

import lombok.Getter;

/**
 * 日志类型枚举
 * <p>
 * 定义系统日志类型，用于日志分类
 *
 * @author binblink
 */
@Getter
public enum LogType {

    /**
     * 登入日志
     */
    LOGIN("LOGIN", "登入日志"),

    /**
     * 系统日志
     */
    SYSTEM("SYSTEM", "系统日志"),

    /**
     * 操作日志
     */
    OPERATION("OPERATION", "操作日志");

    private final String code;
    private final String description;

    LogType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 日志类型枚举
     */
    public static LogType getByCode(String code) {
        for (LogType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OPERATION;
    }
}