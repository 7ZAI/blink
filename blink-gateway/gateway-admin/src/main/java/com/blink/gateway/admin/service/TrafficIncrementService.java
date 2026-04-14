package com.blink.gateway.admin.service;

/**
 * 流量增量计算服务
 *
 * 负责计算请求增量（当前累计值 - 上次累计值）
 * 并存储到 Redis Sorted Set 中供前端展示趋势图
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface TrafficIncrementService {

    /**
     * 计算并存储流量增量
     *
     * @param instanceId 实例ID
     * @param currentTotalRequests 当前累计请求数
     * @param currentSuccessRequests 当前累计成功请求数
     * @param currentFailedRequests 当前累计失败请求数
     * @return 计算出的请求增量（最近一个周期的请求数）
     */
    long calculateAndStoreIncrement(String instanceId, long currentTotalRequests,
                                     long currentSuccessRequests, long currentFailedRequests);

    /**
     * 获取最近 N 分钟的流量增量数据
     *
     * @param minutes 时间范围（分钟）
     * @return 流量数据点列表（时间戳和增量值的映射）
     */
    java.util.Map<Long, Long> getRecentTrafficIncrement(int minutes);

    /**
     * 获取最新的流量增量（用于 SSE 推送）
     *
     * @return 最新增量数据点（时间戳和增量值），若无数据返回 null
     */
    TrafficDataPoint getLatestIncrement();

    /**
     * 清理过期数据
     *
     * @param retentionMinutes 数据保留时间（分钟）
     */
    void cleanExpiredData(int retentionMinutes);

    /**
     * 流量数据点
     */
    record TrafficDataPoint(long timestamp, long increment) {}
}