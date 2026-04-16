package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例下线指令消息 DTO
 *
 * 用于 gateway-admin 向指定的 gateway-reactive 实例发送下线指令
 * 网关实例收到指令后，执行流量排空并优雅下线
 *
 * @author binblink
 * @since 2026-04-16
 */
@Getter
@Setter
public class InstanceOfflineMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标实例标识
     * 格式：host:port（如 10.141.92.120:8002）
     */
    private String targetInstance;

    /**
     * 下线类型：GRACEFUL（优雅下线）/ FORCE（强制下线）
     */
    private String offlineType;

    /**
     * 流量排空等待时间（秒）
     * 优雅下线时，网关将在此时间内停止接收新请求，等待现有请求处理完成
     */
    private Integer drainWaitSeconds;

    /**
     * 下线原因
     */
    private String reason;

    /**
     * 操作人用户ID
     */
    private Integer operatorUser;

    /**
     * 操作人用户名
     */
    private String operatorName;

    /**
     * 指令ID（用于幂等处理）
     */
    private String commandId;

    @Override
    public String toString() {
        return "InstanceOfflineMsg{" +
                "targetInstance='" + targetInstance + '\'' +
                ", offlineType='" + offlineType + '\'' +
                ", drainWaitSeconds=" + drainWaitSeconds +
                ", reason='" + reason + '\'' +
                ", commandId='" + commandId + '\'' +
                '}';
    }
}
