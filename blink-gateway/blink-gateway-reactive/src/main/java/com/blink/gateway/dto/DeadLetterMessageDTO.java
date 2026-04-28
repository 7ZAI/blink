package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 死信消息 DTO
 * 用于存储消费失败的消息信息
 *
 * @author binblink
 * @since 2026-04-28
 */
@Getter
@Setter
public class DeadLetterMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原始 Stream Key
     */
    private String originalStreamKey;

    /**
     * 消费者组名称
     */
    private String groupName;

    /**
     * 原始消息 ID
     */
    private String messageId;

    /**
     * 消息类型（消息体类名）
     */
    private String payloadClass;

    /**
     * 消息体 JSON
     */
    private String payloadJson;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 失败时间
     */
    private LocalDateTime failedTime;

    /**
     * 消费失败的实例标识
     */
    private String failedInstance;

    /**
     * 重试次数
     */
    private Integer retryCount;
}
