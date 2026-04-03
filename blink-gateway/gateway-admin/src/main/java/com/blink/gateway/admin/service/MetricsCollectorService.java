package com.blink.gateway.admin.service;

/**
 * 网关指标采集服务接口
 * 定时采集 gateway-reactive 实例的 actuator 监控指标
 *
 * @author binblink
 */
public interface MetricsCollectorService {

    /**
     * 定时采集所有网关实例的指标
     * 存储到 Redis（实时数据）和 MySQL（历史数据）
     */
    void collectMetrics();

    /**
     * 清理过期的历史数据
     */
    void cleanHistoryMetrics();
}