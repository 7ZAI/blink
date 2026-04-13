package com.blink.gateway.admin.service;

import com.blink.gateway.admin.sse.InstanceStatusPayload;

import java.util.List;

/**
 * 实例状态推送服务
 * 负责检测状态变化并推送 SSE 消息
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface InstanceStatusPushService {

    /**
     * 采集完成后的状态检测与推送
     * 由 MetricsCollectorService 在采集完成后调用
     *
     * @param payloads 所有实例的状态载荷
     */
    void checkAndPush(List<InstanceStatusPayload.InstanceSummary> payloads);

    /**
     * 实例状态变化时推送（用于手动上下线）
     *
     * @param instanceId 实例ID
     * @param status     新状态
     */
    void pushStatusChange(String instanceId, Integer status);

    /**
     * 发送完整状态给指定用户（用户首次进入页面时）
     *
     * @param userId 用户ID
     */
    void sendFullStatusToUser(Integer userId);
}
