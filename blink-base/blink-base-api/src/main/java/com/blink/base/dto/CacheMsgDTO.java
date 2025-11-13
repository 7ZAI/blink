package com.blink.base.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author binblink
 */
public class CacheMsgDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -458878613754743325L;
    /**
     * 缓存key值
     */
    private String key;

    /**
     * 当前缓存value
     */
    private Object value;

    /**
     * 缓存操作 A增加 D删除  M修改
     */
    private String operator;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }






}
