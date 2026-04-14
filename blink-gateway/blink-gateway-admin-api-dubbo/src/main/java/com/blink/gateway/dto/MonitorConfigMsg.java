package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监控配置同步消息 DTO
 *
 * 用于 gateway-admin 向所有 gateway-reactive 实例推送监控配置变更
 *
 * @author binblink
 * @since 2026-04-14
 */
@Getter
@Setter
public class MonitorConfigMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置键名
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型：0-字符串 1-数字 2-布尔 3-JSON
     */
    private Byte configType;

    /**
     * 操作类型：U-更新
     */
    private String operator;

    /**
     * 操作人用户ID
     */
    private Integer operatorUser;

    /**
     * 操作人用户名
     */
    private String operatorName;

    @Override
    public String toString() {
        return "MonitorConfigMsg{" +
                "configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configType=" + configType +
                ", operator='" + operator + '\'' +
                ", operatorUser=" + operatorUser +
                ", operatorName='" + operatorName + '\'' +
                '}';
    }
}
