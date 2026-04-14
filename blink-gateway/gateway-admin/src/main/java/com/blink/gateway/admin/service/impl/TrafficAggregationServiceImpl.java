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

            // 新增指标的聚合变量
            long totalAvgResponseTime = 0;
            long totalP50ResponseTime = 0;
            long totalP95ResponseTime = 0;
            long totalP99ResponseTime = 0;
            long maxResponseTime = 0;
            long totalError4xx = 0;
            long totalError5xx = 0;
            int maxCurrentQps = 0;

            for (Object pointObj : dataPoints) {
                if (pointObj == null) {
                    continue;
                }

                String point = pointObj.toString();
                try {
                    // 扩展数据格式：increment:success:failed:avgTime:p50:p95:p99:maxTime:4xx:5xx:qps:timestamp
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

                        // 解析新增指标（兼容旧数据格式）
                        if (parts.length >= 12) {
                            long avgTime = Long.parseLong(parts[3]);
                            long p50 = Long.parseLong(parts[4]);
                            long p95 = Long.parseLong(parts[5]);
                            long p99 = Long.parseLong(parts[6]);
                            long maxTime = Long.parseLong(parts[7]);
                            long error4xx = Long.parseLong(parts[8]);
                            long error5xx = Long.parseLong(parts[9]);
                            int qps = Integer.parseInt(parts[10]);

                            totalAvgResponseTime += avgTime;
                            totalP50ResponseTime += p50;
                            totalP95ResponseTime += p95;
                            totalP99ResponseTime += p99;
                            if (maxTime > maxResponseTime) {
                                maxResponseTime = maxTime;
                            }
                            totalError4xx += error4xx;
                            totalError5xx += error5xx;
                            if (qps > maxCurrentQps) {
                                maxCurrentQps = qps;
                            }
                        }
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

            // 计算平均响应时间
            if (dataPointCount > 0 && totalAvgResponseTime > 0) {
                history.setAvgResponseTime(totalAvgResponseTime / dataPointCount);
            } else {
                history.setAvgResponseTime(0L);
            }

            // 响应时间分布
            if (dataPointCount > 0) {
                history.setP50ResponseTime(totalP50ResponseTime / dataPointCount);
                history.setP95ResponseTime(totalP95ResponseTime / dataPointCount);
                history.setP99ResponseTime(totalP99ResponseTime / dataPointCount);
            }
            history.setMaxResponseTime(maxResponseTime);

            // 错误分类
            history.setError4xxCount(totalError4xx);
            history.setError5xxCount(totalError5xx);

            // 计算错误率
            if (totalRequestIncrement > 0) {
                double errorRateValue = (double) (totalError4xx + totalError5xx) / totalRequestIncrement * 100;
                history.setErrorRate(errorRateValue);
            }

            // QPS 指标
            history.setPeakQps(peakQps);
            history.setCurrentQps(maxCurrentQps);

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

            // 新增指标聚合变量
            long totalAvgResponseTime = 0;
            long totalP50ResponseTime = 0;
            long totalP95ResponseTime = 0;
            long totalP99ResponseTime = 0;
            long maxResponseTime = 0;
            long totalError4xx = 0;
            long totalError5xx = 0;
            int maxCurrentQps = 0;
            int minuteCount = 0;

            for (GatewayTrafficHistoryDO minute : minuteDataList) {
                totalRequestCount += minute.getRequestCount();
                totalSuccessCount += minute.getSuccessCount();
                totalFailedCount += minute.getFailedCount();

                if (minute.getPeakQps() != null && minute.getPeakQps() > peakQps) {
                    peakQps = minute.getPeakQps();
                }
                minuteCount++;

                // 聚合新增指标
                if (minute.getAvgResponseTime() != null) {
                    totalAvgResponseTime += minute.getAvgResponseTime();
                }
                if (minute.getP50ResponseTime() != null) {
                    totalP50ResponseTime += minute.getP50ResponseTime();
                }
                if (minute.getP95ResponseTime() != null) {
                    totalP95ResponseTime += minute.getP95ResponseTime();
                }
                if (minute.getP99ResponseTime() != null) {
                    totalP99ResponseTime += minute.getP99ResponseTime();
                }
                if (minute.getMaxResponseTime() != null && minute.getMaxResponseTime() > maxResponseTime) {
                    maxResponseTime = minute.getMaxResponseTime();
                }
                if (minute.getError4xxCount() != null) {
                    totalError4xx += minute.getError4xxCount();
                }
                if (minute.getError5xxCount() != null) {
                    totalError5xx += minute.getError5xxCount();
                }
                if (minute.getCurrentQps() != null && minute.getCurrentQps() > maxCurrentQps) {
                    maxCurrentQps = minute.getCurrentQps();
                }
            }

            // 存储到 MySQL
            GatewayTrafficHistoryDO history = new GatewayTrafficHistoryDO();
            history.setTimeBucket(startTime);
            history.setGranularity("HOUR");
            history.setRequestCount(totalRequestCount);
            history.setSuccessCount(totalSuccessCount);
            history.setFailedCount(totalFailedCount);

            // 计算平均响应时间
            if (minuteCount > 0 && totalAvgResponseTime > 0) {
                history.setAvgResponseTime(totalAvgResponseTime / minuteCount);
            } else {
                history.setAvgResponseTime(0L);
            }

            // 响应时间分布
            if (minuteCount > 0) {
                history.setP50ResponseTime(totalP50ResponseTime / minuteCount);
                history.setP95ResponseTime(totalP95ResponseTime / minuteCount);
                history.setP99ResponseTime(totalP99ResponseTime / minuteCount);
            }
            history.setMaxResponseTime(maxResponseTime);

            // 错误分类
            history.setError4xxCount(totalError4xx);
            history.setError5xxCount(totalError5xx);

            // 计算错误率
            if (totalRequestCount > 0) {
                double errorRateValue = (double) (totalError4xx + totalError5xx) / totalRequestCount * 100;
                history.setErrorRate(errorRateValue);
            }

            // QPS 指标
            history.setPeakQps(peakQps);
            history.setCurrentQps(maxCurrentQps);

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