package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知项响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationItemRsp implements Serializable {

    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String sourceRef;

    private LocalDateTime createdTime;

    private Boolean read = false;
}