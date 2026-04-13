package com.blink.gateway.admin.service;

/**
 * 网关指标历史数据管理服务接口
 * 负责历史数据的存储和清理
 *
 * 注意：实时指标采集已迁移到 Redis Stream 推模式
 * gateway-reactive 通过 MetricsReporter 主动上报指标
 * gateway-admin 通过 MetricsStreamConsumer 消费并存储到 Redis
 *
 * @author binblink
 */
public interface MetricsCollectorService {

    /**
     * 清理过期的历史数据
     */
    void cleanHistoryMetrics();
}