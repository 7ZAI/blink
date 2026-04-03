package com.blink.framework.common.constrant;

/**
 * 响应类型
 *
 * @author binblink
 */
public enum ResponseMsgType {

    /**
     * 成功
     */
    SUCCESS("S"),

    /**
     * 业务错误
     */
    BUSINESS_ERR("BUSS_ERR"),
    /**
     * 系统错误
     */
    SYSTEM_ERR("SYS_ERR"),

    /**
     * 内部错误 不对外公开
     */
    INNER_ERR("INNER_ERR")
    ;


    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    ResponseMsgType(String type) {
        this.type = type;
    }
}
