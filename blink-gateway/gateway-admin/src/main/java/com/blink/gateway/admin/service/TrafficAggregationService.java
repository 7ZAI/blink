package com.blink.gateway.admin.service;

/**
 * 流量聚合服务
 *
 * 负责将秒级流量数据聚合为分钟级、小时级数据
 * 并持久化到 MySQL
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface TrafficAggregationService {

    /**
     * 执行分钟级聚合
     * 将最近一分钟内的秒级数据聚合为分钟级数据
     */
    void aggregateToMinute();

    /**
     * 执行小时级聚合
     * 将最近一小时内的分钟级数据聚合为小时级数据
     */
    void aggregateToHour();

    /**
     * 清理过期历史数据
     * 默认保留天数：分钟级 1 天，小时级 30 天
     */
    void cleanExpiredHistory();
}