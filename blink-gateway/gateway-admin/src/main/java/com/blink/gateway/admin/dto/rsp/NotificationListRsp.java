package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 消息通知列表响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationListRsp implements Serializable {

    private List<NotificationItemRsp> notifications;

    private Integer unreadCount;
}