package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读消息数量响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class UnreadCountRsp implements Serializable {

    private Integer unreadCount;
}