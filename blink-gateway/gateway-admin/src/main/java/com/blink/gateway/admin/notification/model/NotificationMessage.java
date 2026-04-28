package com.blink.gateway.admin.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知消息模型
 *
 * @author binblink
 * @since 2026-04-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型（ALERT/OPERATION/SYSTEM）
     */
    private String notificationType;

    /**
     * 严重级别（INFO/WARNING/ERROR/SUCCESS）
     */
    private String severity;

    /**
     * 接收人列表
     */
    private List<String> recipients;

    /**
     * 扩展参数（JSON格式，用于Webhook等）
     */
    private Map<String, Object> extra;

    /**
     * 关联的业务ID（如告警ID）
     */
    private String businessId;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}
