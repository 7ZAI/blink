package com.blink.gateway.admin.service;

import com.blink.gateway.admin.sse.DashboardDataPayload;

/**
 * 仪表盘数据推送服务
 * 负责构建仪表盘数据并通过 SSE 广播给所有客户端
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface DashboardPushService {

    /**
     * 构建仪表盘数据并广播推送
     * 在 MetricsStreamConsumer 处理完指标后调用
     */
    void pushDashboardData();

    /**
     * 发送完整仪表盘数据给指定用户
     * 用于用户首次进入仪表盘页面时获取初始数据
     *
     * @param userId 用户ID
     */
    void sendFullDashboardToUser(Integer userId);
}