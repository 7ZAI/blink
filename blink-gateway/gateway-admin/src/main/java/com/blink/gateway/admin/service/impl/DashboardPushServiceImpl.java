package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.sse.DashboardDataPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.blink.gateway.admin.service.DashboardPushService;
import com.blink.gateway.admin.service.TrafficIncrementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据推送服务实现
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class DashboardPushServiceImpl implements DashboardPushService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Resource
    private RedisClient redisClient;

    @Resource
    private SseConnectionPool sseConnectionPool;

    @Resource
    private TrafficIncrementService trafficIncrementService;

    @Override
    public void pushDashboardData() {
        try {
            DashboardDataPayload payload = buildDashboardPayload();

            if (payload == null) {
                log.debug("[DashboardPush] 无数据，跳过推送");
                return;
            }

            // 广播给所有连接的客户端
            SseMessage<DashboardDataPayload> msg = SseMessage.dashboardData(payload);
            sseConnectionPool.broadcast(msg);

            log.debug("[DashboardPush] 仪表盘数据已推送 | 实例数: {}, 总请求: {}",
                    payload.getInstances().size(),
                    payload.getStatistics().getTotalRequests());
        } catch (Exception e) {
            log.error("[DashboardPush] 推送仪表盘数据失败 | error: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendFullDashboardToUser(Integer userId) {
        try {
            DashboardDataPayload payload = buildDashboardPayload();

            if (payload == null) {
                log.debug("[DashboardPush] 无数据，跳过发送");
                return;
            }

            // 发送给指定用户
            SseMessage<DashboardDataPayload> msg = SseMessage.dashboardData(payload);
            sseConnectionPool.sendToUser(userId, msg);

            log.info("[DashboardPush] 发送仪表盘数据给用户 | userId: {}, 实例数: {}", userId, payload.getInstances().size());
        } catch (Exception e) {
            log.error("[DashboardPush] 发送仪表盘数据失败 | userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 构建仪表盘数据载荷
     */
    private DashboardDataPayload buildDashboardPayload() {
        // 从 Redis 读取汇总统计
        Map<String, Object> summary = redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_METRICS_SUMMARY);

        // 从 Redis 读取实例列表
        Map<String, Object> instanceList = redisClient.hGetStringMap(RedisKeyConstant.GATEWAY_INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList) && CollUtil.isEmpty(summary)) {
            return null;
        }

        DashboardDataPayload payload = new DashboardDataPayload();
        payload.setTimestamp(System.currentTimeMillis());

        // 构建统计信息
        DashboardDataPayload.StatisticsSummary statistics = buildStatistics(summary);
        payload.setStatistics(statistics);

        // 构建实例列表
        List<DashboardDataPayload.InstanceInfo> instances = buildInstanceList(instanceList);
        payload.setInstances(instances);

        // 构建最新流量数据点
        DashboardDataPayload.TrafficPoint latestTraffic = buildTrafficPoint(statistics);
        payload.setLatestTraffic(latestTraffic);

        return payload;
    }

    /**
     * 构建统计信息
     */
    private DashboardDataPayload.StatisticsSummary buildStatistics(Map<String, Object> summary) {
        DashboardDataPayload.StatisticsSummary statistics = new DashboardDataPayload.StatisticsSummary();

        if (CollUtil.isNotEmpty(summary)) {
            statistics.setTotalInstances(getIntValue(summary, "total"));
            statistics.setHealthyInstances(getIntValue(summary, "healthy"));
            statistics.setTotalRequests(getLongValue(summary, "totalRequests"));
            statistics.setSuccessRequests(getLongValue(summary, "totalSuccessRequests"));
            statistics.setFailedRequests(getLongValue(summary, "totalFailedRequests"));
            statistics.setAvgResponseTime(getLongValue(summary, "avgResponseTime"));
        } else {
            // 默认值
            statistics.setTotalInstances(0);
            statistics.setHealthyInstances(0);
            statistics.setTotalRequests(0L);
            statistics.setSuccessRequests(0L);
            statistics.setFailedRequests(0L);
            statistics.setAvgResponseTime(0L);
        }

        // 计算成功率
        if (statistics.getTotalRequests() != null && statistics.getTotalRequests() > 0) {
            double rate = (statistics.getSuccessRequests() * 100.0) / statistics.getTotalRequests();
            statistics.setSuccessRate(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP) + "%");
        } else {
            statistics.setSuccessRate("0%");
        }

        return statistics;
    }

    /**
     * 构建实例列表
     */
    private List<DashboardDataPayload.InstanceInfo> buildInstanceList(Map<String, Object> instanceList) {
        List<DashboardDataPayload.InstanceInfo> instances = new ArrayList<>();

        if (CollUtil.isEmpty(instanceList)) {
            return instances;
        }

        for (String instanceId : instanceList.keySet()) {
            String metricsKey = RedisKeyConstant.GATEWAY_METRICS_PREFIX + instanceId;
            Map<String, Object> metrics = redisClient.hGetStringMap(metricsKey);

            DashboardDataPayload.InstanceInfo instance = new DashboardDataPayload.InstanceInfo();
            instance.setInstanceId(instanceId);

            if (CollUtil.isNotEmpty(metrics)) {
                instance.setServiceId((String) metrics.get("serviceId"));
                instance.setHost((String) metrics.get("host"));
                instance.setPort(getIntValue(metrics, "port"));

                // 健康状态
                String healthStatus = (String) metrics.get("healthStatus");
                instance.setHealthStatus(healthStatus);
                instance.setHealthy("UP".equals(healthStatus));

                // 指标数据
                instance.setCpuUsage(getDoubleValue(metrics, "cpuUsage"));
                instance.setHeapUsagePercent(getDoubleValue(metrics, "heapUsagePercent"));
                instance.setTotalRequests(getLongValue(metrics, "totalRequests"));
                instance.setAvgResponseTime(getLongValue(metrics, "avgResponseTime"));
            } else {
                instance.setHealthy(false);
                instance.setHealthStatus("UNKNOWN");
            }

            instances.add(instance);
        }

        return instances;
    }

    /**
     * 构建流量数据点
     * 从 TrafficIncrementService 获取最新的流量增量数据
     */
    private DashboardDataPayload.TrafficPoint buildTrafficPoint(DashboardDataPayload.StatisticsSummary statistics) {
        DashboardDataPayload.TrafficPoint point = new DashboardDataPayload.TrafficPoint();
        point.setTime(LocalDateTime.now().format(TIME_FORMATTER));
        point.setTimestamp(System.currentTimeMillis());

        // 从增量服务获取最新增量数据
        TrafficIncrementService.TrafficDataPoint latestIncrement = trafficIncrementService.getLatestIncrement();

        if (latestIncrement != null) {
            // 使用增量数据（最近一个周期的请求数）
            point.setCount(latestIncrement.increment());
            point.setTimestamp(latestIncrement.timestamp());
            log.debug("[DashboardPush] 流量增量 | count: {}, timestamp: {}",
                    latestIncrement.increment(), latestIncrement.timestamp());
        } else {
            // 无增量数据时，使用 0
            point.setCount(0L);
        }

        return point;
    }

    /**
     * 从 Map 中获取 Integer 值（支持 String 和 Number 类型）
     * 注意：Redis 配置中 Long 类型会被 ToStringSerializer 序列化为 String
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("[DashboardPush] Integer 解析失败 | key: {}, value: {}", key, value);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 从 Map 中获取 Long 值（支持 String 和 Number 类型）
     * 注意：Redis 配置中 Long 类型会被 ToStringSerializer 序列化为 String
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("[DashboardPush] Long 解析失败 | key: {}, value: {}", key, value);
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 从 Map 中获取 Double 值（支持 String 和 Number 类型）
     * 注意：部分数值可能被序列化为 String
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("[DashboardPush] Double 解析失败 | key: {}, value: {}", key, value);
                return 0.0;
            }
        }
        return 0.0;
    }
}