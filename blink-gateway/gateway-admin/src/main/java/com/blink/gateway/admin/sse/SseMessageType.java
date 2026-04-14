package com.blink.gateway.admin.sse;

/**
 * SSE 消息类型常量
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface SseMessageType {

    /**
     * 心跳消息
     */
    String HEARTBEAT = "heartbeat";

    /**
     * 通知消息（告警、公告等）
     */
    String NOTIFICATION = "notification";

    /**
     * 实例状态推送（在线/离线/健康状态变化）
     */
    String INSTANCE_STATUS = "instance_status";

    /**
     * 仪表盘数据推送（统计信息、实例列表、流量数据）
     */
    String DASHBOARD_DATA = "dashboard_data";
}
