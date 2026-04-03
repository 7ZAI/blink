package com.blink.log.sensitive;

/**
 * 敏感数据脱敏类型枚举
 * 
 * @author binblink
 */
public enum SensitiveType {

    PHONE("手机号", 3, 4),

    ID_CARD("身份证号", 6, 4),

    BANK_CARD("银行卡号", 4, 4),

    EMAIL("邮箱", 3, 4),

    NAME("姓名", 1, 1),

    PASSWORD("密码", 0, 0),

    ADDRESS("地址", 6, 0),

    CUSTOM("自定义", 0, 0);

    private final String description;
    private final int prefixKeep;
    private final int suffixKeep;

    SensitiveType(String description, int prefixKeep, int suffixKeep) {
        this.description = description;
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
    }

    public String getDescription() {
        return description;
    }

    public int getPrefixKeep() {
        return prefixKeep;
    }

    public int getSuffixKeep() {
        return suffixKeep;
    }
}
