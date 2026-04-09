package com.blink.framework.validate.constant;

/**
 * 约束数据类型枚举
 * 对应 sys_field_constraint 表中的 data_type 字段
 *
 * @author binblink
 * @since 2026-03-07
 */
public enum ConstraintDataType {

    /**
     * 字符串类型
     */
    STRING("S"),

    /**
     * 小数类型
     */
    DECIMAL("D"),

    /**
     * 数字类型
     */
    NUMBER("N");

    private final String type;

    public String getType() {
        return type;
    }

    ConstraintDataType(String type) {
        this.type = type;
    }
}