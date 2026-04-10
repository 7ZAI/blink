package com.blink.gateway.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 消息 ACK 确认请求 DTO
 *
 * @author binblink
 * @since 2026-04-10
 */
@Getter
@Setter
public class MessageAckReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Stream 消息 ID（Redis Stream 中的 recordId）
     */
    private String streamId;

    /**
     * 业务消息 ID（StreamMessage 中的 msgId）
     */
    private String msgId;

    /**
     * 消费是否成功
     */
    private Boolean success;

    /**
     * 消费者名称
     */
    private String consumer;

    /**
     * 错误信息（失败时填写）
     */
    private String errorMsg;

    /**
     * 操作人用户ID（用于 SSE 推送通知）
     */
    private Integer operatorUser;

    /**
     * 操作人用户名
     */
    private String operatorName;
}