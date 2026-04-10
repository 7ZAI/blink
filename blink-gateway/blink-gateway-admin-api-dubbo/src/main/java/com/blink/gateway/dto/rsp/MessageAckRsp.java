package com.blink.gateway.dto.rsp;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 消息 ACK 确认响应 DTO
 *
 * @author binblink
 * @since 2026-04-10
 */
@Getter
@Setter
public class MessageAckRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否确认成功
     */
    private Boolean acked;

    /**
     * 结果消息
     */
    private String message;
}