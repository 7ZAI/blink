package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.service.TrafficIncrementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 流量增量计算服务实现
 *
 * 核心逻辑：
 * 1. 记录每个实例上次上报的累计值
 * 2. 每次收到新指标时，计算增量 = 当前值 - 上次值
 * 3. 将增量数据存入 Redis Sorted Set（按时间戳排序）
 * 4. 处理实例重启场景：当当前值小于上次值时，重置基准
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class TrafficIncrementServiceImpl implements TrafficIncrementService {

    /**
     * 实时流量数据保留时间（秒）- 默认 1 小时
     */
    private static final int REALTIME_DATA_TTL_SECONDS = 3600;

    /**
     * 上次累计值 Hash Key
     */
    private static final String LAST_VALUES_KEY = "blink:gateway:traffic:last:values";

    @Resource
    private RedisClient redisClient;

    @Override
    public long calculateAndStoreIncrement(String instanceId, long currentTotalRequests,
                                            long currentSuccessRequests, long currentFailedRequests) {
        // 使用扩展方法的默认值
        return calculateAndStoreIncrementExtended(instanceId, currentTotalRequests,
                currentSuccessRequests, currentFailedRequests, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public long calculateAndStoreIncrementExtended(String instanceId,
                                                     long currentTotalRequests,
                                                     long currentSuccessRequests,
                                                     long currentFailedRequests,
                                                     long avgResponseTime,
                                                     long p50ResponseTime,
                                                     long p95ResponseTime,
                                                     long p99ResponseTime,
                                                     long maxResponseTime,
                                                     long error4xxCount,
                                                     long error5xxCount,
                                                     int currentQps) {
        if (instanceId == null || instanceId.isEmpty()) {
            log.warn("[TrafficIncrement] instanceId 为空，跳过计算");
            return 0;
        }

        long timestamp = System.currentTimeMillis();

        // 从 Redis 获取上次累计值
        Object lastTotalObj = redisClient.hGetField(LAST_VALUES_KEY, instanceId + ":total");
        Object lastSuccessObj = redisClient.hGetField(LAST_VALUES_KEY, instanceId + ":success");
        Object lastFailedObj = redisClient.hGetField(LAST_VALUES_KEY, instanceId + ":failed");

        long lastTotalRequests = parseLongValue(lastTotalObj);
        long lastSuccessRequests = parseLongValue(lastSuccessObj);
        long lastFailedRequests = parseLongValue(lastFailedObj);

        // 计算增量
        long increment;
        long successIncrement;
        long failedIncrement;

        if (lastTotalRequests == 0) {
            // 首次上报或上次值为空，增量设为 0
            increment = 0;
            successIncrement = 0;
            failedIncrement = 0;
            log.debug("[TrafficIncrement] 首次上报 | instanceId: {}, currentTotal: {}", instanceId, currentTotalRequests);
        } else if (currentTotalRequests < lastTotalRequests) {
            // 当前值小于上次值（实例重启），重置基准，增量设为当前值
            increment = currentTotalRequests;
            successIncrement = currentSuccessRequests;
            failedIncrement = currentFailedRequests;
            log.info("[TrafficIncrement] 实例可能已重启 | instanceId: {}, lastTotal: {}, currentTotal: {}",
                    instanceId, lastTotalRequests, currentTotalRequests);
        } else {
            // 正常增量计算
            increment = currentTotalRequests - lastTotalRequests;
            successIncrement = currentSuccessRequests - lastSuccessRequests;
            failedIncrement = currentFailedRequests - lastFailedRequests;
            log.debug("[TrafficIncrement] 正常增量 | instanceId: {}, increment: {}", instanceId, increment);
        }

        // 更新上次累计值
        redisClient.hPutField(LAST_VALUES_KEY, instanceId + ":total", currentTotalRequests);
        redisClient.hPutField(LAST_VALUES_KEY, instanceId + ":success", currentSuccessRequests);
        redisClient.hPutField(LAST_VALUES_KEY, instanceId + ":failed", currentFailedRequests);

        // 存储增量数据到 Sorted Set
        // 扩展格式：increment:success:failed:avgTime:p50:p95:p99:maxTime:4xx:5xx:qps:timestamp
        String dataPoint = String.format("%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d",
                increment, successIncrement, failedIncrement,
                avgResponseTime, p50ResponseTime, p95ResponseTime, p99ResponseTime, maxResponseTime,
                error4xxCount, error5xxCount, currentQps, timestamp);
        redisClient.zAdd(RedisKeyConstant.TRAFFIC_REALTIME_KEY, dataPoint, timestamp);

        // 设置过期时间
        redisClient.expire(RedisKeyConstant.TRAFFIC_REALTIME_KEY, REALTIME_DATA_TTL_SECONDS);

        // 记录详细日志（调试用）
        if (increment > 0) {
            log.debug("[TrafficIncrement] 流量增量存储成功 | instanceId: {}, increment: {}, timestamp: {}",
                    instanceId, increment, timestamp);
        }

        return increment;
    }

    @Override
    public Map<Long, Long> getRecentTrafficIncrement(int minutes) {
        Map<Long, Long> result = new HashMap<>();

        long endTime = System.currentTimeMillis();
        long startTime = endTime - minutes * 60 * 1000L;

        // 从 Sorted Set 获取指定时间范围的数据
        Set<Object> dataPoints = redisClient.zRangeByScore(
                RedisKeyConstant.TRAFFIC_REALTIME_KEY,
                startTime,
                endTime);

        if (CollUtil.isEmpty(dataPoints)) {
            return result;
        }

        // 解析数据点，聚合增量值
        // 数据格式：increment:success:failed:timestamp
        for (Object pointObj : dataPoints) {
            if (pointObj == null) {
                continue;
            }

            String point = pointObj.toString();
            try {
                String[] parts = point.split(":");
                if (parts.length >= 4) {
                    long increment = Long.parseLong(parts[0]);
                    long timestamp = Long.parseLong(parts[3]);
                    result.put(timestamp, increment);
                }
            } catch (NumberFormatException e) {
                log.warn("[TrafficIncrement] 数据点解析失败 | point: {}", point);
            }
        }

        return result;
    }

    @Override
    public TrafficDataPoint getLatestIncrement() {
        // 获取最新的数据点（Sorted Set 按时间戳升序，取最后一个）
        Set<Object> latestPoints = redisClient.zRange(
                RedisKeyConstant.TRAFFIC_REALTIME_KEY,
                -1,
                -1);

        if (CollUtil.isEmpty(latestPoints)) {
            return null;
        }

        Object pointObj = latestPoints.iterator().next();
        if (pointObj == null) {
            return null;
        }

        String point = pointObj.toString();
        try {
            String[] parts = point.split(":");
            if (parts.length >= 4) {
                long increment = Long.parseLong(parts[0]);
                long timestamp = Long.parseLong(parts[3]);
                return new TrafficDataPoint(timestamp, increment);
            }
        } catch (NumberFormatException e) {
            log.warn("[TrafficIncrement] 最新数据点解析失败 | point: {}", point);
        }

        return null;
    }

    @Override
    public void cleanExpiredData(int retentionMinutes) {
        long cutoffTime = System.currentTimeMillis() - retentionMinutes * 60 * 1000L;

        Long deleted = redisClient.zRemoveRangeByScore(
                RedisKeyConstant.TRAFFIC_REALTIME_KEY,
                0,
                cutoffTime);

        if (deleted != null && deleted > 0) {
            log.info("[TrafficIncrement] 清理过期数据完成 | 删除数量: {}", deleted);
        }
    }

    /**
     * 解析 Long 值
     */
    private long parseLongValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}