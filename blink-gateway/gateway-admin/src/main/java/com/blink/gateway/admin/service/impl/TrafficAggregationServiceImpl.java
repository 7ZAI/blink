package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.entity.GatewayTrafficHistoryDO;
import com.blink.gateway.admin.mapper.GatewayTrafficHistoryMapper;
import com.blink.gateway.admin.service.TrafficAggregationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 流量聚合服务实现
 *
 * 定时任务：
 * - 每分钟执行：秒级数据 → 分钟级聚合
 * - 每小时执行：分钟级数据 → 小时级聚合
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class TrafficAggregationServiceImpl implements TrafficAggregationService {

    /**
     * 分钟级数据保留天数
     */
    private static final int MINUTE_DATA_RETENTION_DAYS = 1;

    /**
     * 小时级数据保留天数
     */
    private static final int HOUR_DATA_RETENTION_DAYS = 30;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private RedisClient redisClient;

    @Resource
    private GatewayTrafficHistoryMapper trafficHistoryMapper;

    /**
     * 每分钟执行：聚合秒级数据到分钟级
     * Cron: 每分钟的第 0 秒执行
     */
    @Override
    @Scheduled(cron = "0 * * * * ?")
    public void aggregateToMinute() {
        try {
            long minuteStart = truncateToMinute(System.currentTimeMillis() - 60000);
            long minuteEnd = minuteStart + 60000;

            // 从 Redis Sorted Set 获取该分钟内的所有秒级数据
            Set<Object> dataPoints = redisClient.zRangeByScore(
                    RedisKeyConstant.TRAFFIC_REALTIME_KEY,
                    minuteStart,
                    minuteEnd);

            if (CollUtil.isEmpty(dataPoints)) {
                log.debug("[TrafficAggregation] 无秒级数据，跳过分钟聚合 | minute: {}",
                        formatTime(minuteStart));
                return;
            }

            // 计算聚合值
            long totalRequestIncrement = 0;
            long totalSuccessIncrement = 0;
            long totalFailedIncrement = 0;
            int peakQps = 0;
            int dataPointCount = 0;

            for (Object pointObj : dataPoints) {
                if (pointObj == null) {
                    continue;
                }

                String point = pointObj.toString();
                try {
                    // 数据格式：increment:success:failed:timestamp
                    String[] parts = point.split(":");
                    if (parts.length >= 4) {
                        long increment = Long.parseLong(parts[0]);
                        long success = Long.parseLong(parts[1]);
                        long failed = Long.parseLong(parts[2]);

                        totalRequestIncrement += increment;
                        totalSuccessIncrement += success;
                        totalFailedIncrement += failed;

                        // 计算峰值 QPS（最大增量）
                        if (increment > peakQps) {
                            peakQps = (int) increment;
                        }
                        dataPointCount++;
                    }
                } catch (NumberFormatException e) {
                    log.warn("[TrafficAggregation] 数据点解析失败 | point: {}", point);
                }
            }

            // 存储到 MySQL
            GatewayTrafficHistoryDO history = new GatewayTrafficHistoryDO();
            history.setTimeBucket(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(minuteStart),
                    java.time.ZoneId.systemDefault()));
            history.setGranularity("MINUTE");
            history.setRequestCount(totalRequestIncrement);
            history.setSuccessCount(totalSuccessIncrement);
            history.setFailedCount(totalFailedIncrement);
            history.setAvgResponseTime(0L); // 暂不计算
            history.setPeakQps(peakQps);

            trafficHistoryMapper.insert(history);

            log.info("[TrafficAggregation] 分钟聚合完成 | time: {}, count: {}, peakQps: {}, dataPoints: {}",
                    formatTime(minuteStart), totalRequestIncrement, peakQps, dataPointCount);

            // 清理已聚合的秒级数据
            redisClient.zRemoveRangeByScore(
                    RedisKeyConstant.TRAFFIC_REALTIME_KEY,
                    0,
                    minuteEnd);

        } catch (Exception e) {
            log.error("[TrafficAggregation] 分钟聚合失败 | error: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时执行：聚合分钟级数据到小时级
     * Cron: 每小时的第 0 分第 0 秒执行
     */
    @Override
    @Scheduled(cron = "0 0 * * * ?")
    public void aggregateToHour() {
        try {
            long hourStart = truncateToHour(System.currentTimeMillis() - 3600000);
            long hourEnd = hourStart + 3600000;

            // 从数据库查询该小时内的分钟级数据
            LocalDateTime startTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(hourStart),
                    java.time.ZoneId.systemDefault());
            LocalDateTime endTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(hourEnd),
                    java.time.ZoneId.systemDefault());

            // 查询分钟级数据
            var minuteDataList = trafficHistoryMapper.selectByTimeRangeAndGranularity(
                    startTime, endTime, "MINUTE");

            if (CollUtil.isEmpty(minuteDataList)) {
                log.debug("[TrafficAggregation] 无分钟级数据，跳过小时聚合 | hour: {}",
                        formatTime(hourStart));
                return;
            }

            // 计算聚合值
            long totalRequestCount = 0;
            long totalSuccessCount = 0;
            long totalFailedCount = 0;
            int peakQps = 0;

            for (GatewayTrafficHistoryDO minute : minuteDataList) {
                totalRequestCount += minute.getRequestCount();
                totalSuccessCount += minute.getSuccessCount();
                totalFailedCount += minute.getFailedCount();

                if (minute.getPeakQps() != null && minute.getPeakQps() > peakQps) {
                    peakQps = minute.getPeakQps();
                }
            }

            // 存储到 MySQL
            GatewayTrafficHistoryDO history = new GatewayTrafficHistoryDO();
            history.setTimeBucket(startTime);
            history.setGranularity("HOUR");
            history.setRequestCount(totalRequestCount);
            history.setSuccessCount(totalSuccessCount);
            history.setFailedCount(totalFailedCount);
            history.setAvgResponseTime(0L);
            history.setPeakQps(peakQps);

            trafficHistoryMapper.insert(history);

            log.info("[TrafficAggregation] 小时聚合完成 | time: {}, count: {}, peakQps: {}, minuteCount: {}",
                    formatTime(hourStart), totalRequestCount, peakQps, minuteDataList.size());

        } catch (Exception e) {
            log.error("[TrafficAggregation] 小时聚合失败 | error: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期历史数据
     * 每天凌晨 2 点执行
     */
    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredHistory() {
        try {
            log.info("[TrafficAggregation] 开始清理历史数据...");

            // 清理分钟级数据（保留 1 天）
            LocalDateTime minuteCutoff = LocalDateTime.now().minusDays(MINUTE_DATA_RETENTION_DAYS);
            int deletedMinute = trafficHistoryMapper.deleteBeforeTime(minuteCutoff, "MINUTE");

            // 清理小时级数据（保留 30 天）
            LocalDateTime hourCutoff = LocalDateTime.now().minusDays(HOUR_DATA_RETENTION_DAYS);
            int deletedHour = trafficHistoryMapper.deleteBeforeTime(hourCutoff, "HOUR");

            log.info("[TrafficAggregation] 清理历史数据完成 | 删除分钟级: {}, 删除小时级: {}",
                    deletedMinute, deletedHour);

        } catch (Exception e) {
            log.error("[TrafficAggregation] 清理历史数据失败", e);
        }
    }

    /**
     * 将时间戳截断到分钟
     */
    private long truncateToMinute(long timestamp) {
        return timestamp / 60000 * 60000;
    }

    /**
     * 将时间戳截断到小时
     */
    private long truncateToHour(long timestamp) {
        return timestamp / 3600000 * 3600000;
    }

    /**
     * 格式化时间戳为可读格式
     */
    private String formatTime(long timestamp) {
        LocalDateTime time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}