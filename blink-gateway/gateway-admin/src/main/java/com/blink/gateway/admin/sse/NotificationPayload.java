package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息载荷
 * 用于 SSE 推送通知类消息
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class NotificationPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    private Long notificationId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 通知类型（alert/announcement/system）
     */
    private String type;

    /**
     * 严重程度（info/warning/error）
     */
    private String severity;

    /**
     * 目标类型（user/all）
     */
    private String targetType;

    /**
     * 目标用户ID
     */
    private Integer targetUserId;

    /**
     * 来源引用
     */
    private String sourceRef;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 从旧版 NotificationMsg 转换
     */
    public static NotificationPayload from(NotificationMsg msg) {
        if (msg == null) {
            return null;
        }
        NotificationPayload payload = new NotificationPayload();
        payload.setNotificationId(msg.getNotificationId());
        payload.setTitle(msg.getTitle());
        payload.setContent(msg.getContent());
        payload.setType(msg.getType());
        payload.setSeverity(msg.getSeverity());
        payload.setTargetType(msg.getTargetType());
        payload.setTargetUserId(msg.getTargetUserId());
        payload.setSourceRef(msg.getSourceRef());
        payload.setCreatedTime(msg.getCreatedTime());
        return payload;
    }
}
