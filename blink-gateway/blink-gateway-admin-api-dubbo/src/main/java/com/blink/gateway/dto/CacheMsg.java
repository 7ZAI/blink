package com.blink.gateway.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 缓存同步消息 DTO
 *
 * @Author binblink
 */
public class CacheMsg implements Serializable {

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
     * 乐观锁更新缓存
     */
    private Integer version;

    /**
     * 缓存操作 A增加 D删除  M修改
     */
    private String operator;

    /**
     * 操作人用户ID（用于 ACK 回调后通过 SSE 推送通知）
     */
    private Integer operatorUser;

    /**
     * 操作人用户名
     */
    private String operatorName;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getOperatorUser() {
        return operatorUser;
    }

    public void setOperatorUser(Integer operatorUser) {
        this.operatorUser = operatorUser;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    @Override
    public String toString() {
        return "CacheMsgDTO{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", version=" + version +
                ", operator='" + operator + '\'' +
                ", operatorUser=" + operatorUser +
                ", operatorName='" + operatorName + '\'' +
                '}';
    }
}
