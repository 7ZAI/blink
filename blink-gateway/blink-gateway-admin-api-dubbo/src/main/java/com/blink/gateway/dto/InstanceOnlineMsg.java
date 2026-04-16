package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例上线指令消息 DTO
 *
 * 用于 gateway-admin 向指定的 gateway-reactive 实例发送上线指令
 * 网关实例收到指令后，恢复接收新请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Getter
@Setter
public class InstanceOnlineMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标实例标识
     * 格式：host:port（如 10.141.92.120:8002）
     */
    private String targetInstance;

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
        return "InstanceOnlineMsg{" +
                "targetInstance='" + targetInstance + '\'' +
                ", commandId='" + commandId + '\'' +
                '}';
    }
}
