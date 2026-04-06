package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis消息传输对象
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class NotificationMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long notificationId;

    private String title;

    private String content;

    private String type;

    private String severity;

    private String targetType;

    private Integer targetUserId;

    private String sourceRef;

    private LocalDateTime createdTime;
}