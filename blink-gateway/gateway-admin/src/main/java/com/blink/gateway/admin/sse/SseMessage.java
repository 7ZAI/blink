package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SSE 消息通用类
 * 通过 type 字段区分不同类型的消息
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class SseMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     *
     * @see SseMessageType
     */
    private String type;

    /**
     * 消息数据载荷
     */
    private T data;

    /**
     * 消息时间
     */
    private LocalDateTime timestamp;

    /**
     * 创建通知类型消息
     */
    public static SseMessage<NotificationPayload> notification(NotificationPayload payload) {
        SseMessage<NotificationPayload> msg = new SseMessage<>();
        msg.setType(SseMessageType.NOTIFICATION);
        msg.setData(payload);
        msg.setTimestamp(LocalDateTime.now());
        return msg;
    }

    /**
     * 创建实例状态推送消息
     */
    public static SseMessage<InstanceStatusPayload> instanceStatus(InstanceStatusPayload payload) {
        SseMessage<InstanceStatusPayload> msg = new SseMessage<>();
        msg.setType(SseMessageType.INSTANCE_STATUS);
        msg.setData(payload);
        msg.setTimestamp(LocalDateTime.now());
        return msg;
    }

    /**
     * 创建仪表盘数据推送消息
     */
    public static SseMessage<DashboardDataPayload> dashboardData(DashboardDataPayload payload) {
        SseMessage<DashboardDataPayload> msg = new SseMessage<>();
        msg.setType(SseMessageType.DASHBOARD_DATA);
        msg.setData(payload);
        msg.setTimestamp(LocalDateTime.now());
        return msg;
    }

    /**
     * 创建心跳消息
     */
    public static SseMessage<String> heartbeat() {
        SseMessage<String> msg = new SseMessage<>();
        msg.setType(SseMessageType.HEARTBEAT);
        msg.setData("ping");
        msg.setTimestamp(LocalDateTime.now());
        return msg;
    }
}
