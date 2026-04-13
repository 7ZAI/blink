package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.sse.InstanceStatusPayload;
import com.blink.gateway.admin.sse.NotificationPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 指标消息消费者
 *
 * 负责消费 gateway-reactive 上报的指标消息，包括：
 * - METRICS: 定时指标上报
 * - REGISTER: 实例启动注册
 * - UNREGISTER: 实例关闭注销
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class MetricsStreamConsumer {

    private static final String METRICS_KEY_PREFIX = "blink:gateway:metrics:";
    private static final String INSTANCE_LIST_KEY = "blink:gateway:instance:list";
    private static final String SUMMARY_KEY = "blink:gateway:metrics:summary";
    private static final int METRICS_TTL_SECONDS = 90;

    private final RedisClient redisClient;
    private final SseConnectionPool sseConnectionPool;
    private final InstanceStatusPushService instanceStatusPushService;

    public MetricsStreamConsumer(RedisClient redisClient,
                                  SseConnectionPool sseConnectionPool,
                                  InstanceStatusPushService instanceStatusPushService) {
        this.redisClient = redisClient;
        this.sseConnectionPool = sseConnectionPool;
        this.instanceStatusPushService = instanceStatusPushService;
    }

    /**
     * 处理 Redis Stream 消息
     *
     * @param message 消息内容
     */
    public void processMessage(Map<String, String> message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        String instanceId = message.get("instanceId");
        if (StrUtil.isBlank(instanceId)) {
            log.warn("[MetricsStreamConsumer] 消息缺少 instanceId，跳过处理");
            return;
        }

        String type = message.getOrDefault("type", "METRICS");

        try {
            switch (type) {
                case "REGISTER" -> handleRegister(message);
                case "UNREGISTER" -> handleUnregister(message);
                default -> handleMetrics(message);
            }
        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 处理消息失败 | instanceId: {}, type: {}, error: {}",
                    instanceId, type, e.getMessage(), e);
        }
    }

    /**
     * 处理 METRICS 类型消息
     */
    private void handleMetrics(Map<String, String> message) {
        String instanceId = message.get("instanceId");

        // 存储实例指标到 Redis Hash
        String metricsKey = METRICS_KEY_PREFIX + instanceId;
        Map<String, Object> metricsData = convertToMetricsData(message);
        redisClient.hSet(metricsKey, metricsData);
        redisClient.expire(metricsKey, METRICS_TTL_SECONDS);

        // 更新实例列表（使用 hPutField 设置单个字段）
        redisClient.hPutField(INSTANCE_LIST_KEY, instanceId, String.valueOf(System.currentTimeMillis()));

        // 更新汇总统计
        updateSummary();

        // 触发状态变化检测
        triggerStatusCheck();

        log.debug("[MetricsStreamConsumer] METRICS 消息处理完成 | instanceId: {}", instanceId);
    }

    /**
     * 处理 REGISTER 类型消息
     */
    private void handleRegister(Map<String, String> message) {
        String instanceId = message.get("instanceId");
        String serviceId = message.get("serviceId");

        // 注册实例到列表
        redisClient.hPutField(INSTANCE_LIST_KEY, instanceId, String.valueOf(System.currentTimeMillis()));

        // 存储初始指标
        String metricsKey = METRICS_KEY_PREFIX + instanceId;
        Map<String, Object> metricsData = convertToMetricsData(message);
        redisClient.hSet(metricsKey, metricsData);
        redisClient.expire(metricsKey, METRICS_TTL_SECONDS);

        // 广播新实例上线通知
        broadcastInstanceNotification(instanceId, serviceId, "实例上线", "success");

        // 触发状态变化检测
        triggerStatusCheck();

        log.info("[MetricsStreamConsumer] 实例注册 | instanceId: {}, serviceId: {}", instanceId, serviceId);
    }

    /**
     * 处理 UNREGISTER 类型消息
     */
    private void handleUnregister(Map<String, String> message) {
        String instanceId = message.get("instanceId");
        String serviceId = message.get("serviceId");

        // 从实例列表移除
        redisClient.hDeleteFields(INSTANCE_LIST_KEY, instanceId);

        // 删除实例指标缓存
        redisClient.delete(METRICS_KEY_PREFIX + instanceId);

        // 广播实例下线通知
        broadcastInstanceNotification(instanceId, serviceId, "实例下线", "warning");

        // 触发状态变化检测
        triggerStatusCheck();

        log.info("[MetricsStreamConsumer] 实例注销 | instanceId: {}, serviceId: {}", instanceId, serviceId);
    }

    /**
     * 转换消息为指标数据
     */
    private Map<String, Object> convertToMetricsData(Map<String, String> message) {
        Map<String, Object> data = new HashMap<>();

        // 复制所有字段，保持原始类型
        copyIfPresent(data, message, "instanceId", String.class);
        copyIfPresent(data, message, "serviceId", String.class);
        copyIfPresent(data, message, "host", String.class);
        copyIfPresent(data, message, "port", Integer.class);
        copyIfPresent(data, message, "timestamp", Long.class);
        copyIfPresent(data, message, "type", String.class);
        copyIfPresent(data, message, "healthStatus", String.class);

        // JVM 指标
        copyIfPresent(data, message, "heapUsed", Long.class);
        copyIfPresent(data, message, "heapMax", Long.class);
        copyIfPresent(data, message, "heapUsagePercent", Double.class);
        copyIfPresent(data, message, "nonHeapUsed", Long.class);
        copyIfPresent(data, message, "cpuUsage", Double.class);

        // GC 指标
        copyIfPresent(data, message, "youngGcCount", Long.class);
        copyIfPresent(data, message, "youngGcTime", Long.class);
        copyIfPresent(data, message, "oldGcCount", Long.class);
        copyIfPresent(data, message, "oldGcTime", Long.class);

        // 线程指标
        copyIfPresent(data, message, "liveThreads", Integer.class);
        copyIfPresent(data, message, "peakThreads", Integer.class);
        copyIfPresent(data, message, "daemonThreads", Integer.class);

        // HTTP 指标
        copyIfPresent(data, message, "totalRequests", Long.class);
        copyIfPresent(data, message, "successRequests", Long.class);
        copyIfPresent(data, message, "failedRequests", Long.class);
        copyIfPresent(data, message, "avgResponseTime", Long.class);

        // 实例状态
        data.put("status", 0); // 默认在线

        return data;
    }

    /**
     * 复制字段（带类型转换）
     */
    private void copyIfPresent(Map<String, Object> target, Map<String, String> source,
                               String key, Class<?> type) {
        String value = source.get(key);
        if (StrUtil.isNotBlank(value)) {
            try {
                if (type == String.class) {
                    target.put(key, value);
                } else if (type == Integer.class) {
                    target.put(key, Integer.parseInt(value));
                } else if (type == Long.class) {
                    target.put(key, Long.parseLong(value));
                } else if (type == Double.class) {
                    target.put(key, Double.parseDouble(value));
                }
            } catch (NumberFormatException e) {
                log.warn("[MetricsStreamConsumer] 字段类型转换失败 | key: {}, value: {}", key, value);
            }
        }
    }

    /**
     * 更新汇总统计
     */
    private void updateSummary() {
        Map<String, Object> instanceList = redisClient.hGetStringMap(INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return;
        }

        int totalCount = instanceList.size();
        int onlineCount = 0;
        int healthyCount = 0;
        double totalCpu = 0;

        for (String instanceId : instanceList.keySet()) {
            Map<String, Object> metrics = redisClient.hGetStringMap(METRICS_KEY_PREFIX + instanceId);

            if (CollUtil.isEmpty(metrics)) {
                continue;
            }

            onlineCount++;

            Object healthStatus = metrics.get("healthStatus");
            if ("UP".equals(healthStatus)) {
                healthyCount++;
            }

            Object cpuUsage = metrics.get("cpuUsage");
            if (cpuUsage != null) {
                totalCpu += ((Number) cpuUsage).doubleValue();
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", totalCount);
        summary.put("online", onlineCount);
        summary.put("healthy", healthyCount);
        summary.put("avgCpuUsage", onlineCount > 0 ?
                BigDecimal.valueOf(totalCpu / onlineCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
        summary.put("timestamp", System.currentTimeMillis());

        redisClient.hSet(SUMMARY_KEY, summary);
        redisClient.expire(SUMMARY_KEY, METRICS_TTL_SECONDS);
    }

    /**
     * 触发状态变化检测
     */
    private void triggerStatusCheck() {
        Map<String, Object> instanceList = redisClient.hGetStringMap(INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return;
        }

        List<InstanceStatusPayload.InstanceSummary> summaries = new ArrayList<>();

        for (String instanceId : instanceList.keySet()) {
            Map<String, Object> metrics = redisClient.hGetStringMap(METRICS_KEY_PREFIX + instanceId);

            InstanceStatusPayload.InstanceSummary summary = new InstanceStatusPayload.InstanceSummary();
            summary.setInstanceId(instanceId);

            if (CollUtil.isNotEmpty(metrics)) {
                Object status = metrics.get("status");
                if (status != null) {
                    summary.setStatus(((Number) status).intValue());
                }

                summary.setHealthStatus((String) metrics.get("healthStatus"));

                Object cpuUsage = metrics.get("cpuUsage");
                if (cpuUsage != null) {
                    summary.setCpuUsage(((Number) cpuUsage).doubleValue());
                }

                Object heapUsagePercent = metrics.get("heapUsagePercent");
                if (heapUsagePercent != null) {
                    summary.setHeapUsagePercent(((Number) heapUsagePercent).doubleValue());
                }

                Object timestamp = metrics.get("timestamp");
                if (timestamp != null) {
                    summary.setTimestamp(((Number) timestamp).longValue());
                }
            }

            summaries.add(summary);
        }

        instanceStatusPushService.checkAndPush(summaries);
    }

    /**
     * 广播实例通知
     */
    private void broadcastInstanceNotification(String instanceId, String serviceId,
                                                String title, String severity) {
        NotificationPayload payload = new NotificationPayload();
        payload.setTitle(title);
        payload.setContent(String.format("实例 %s (%s)", instanceId, serviceId));
        payload.setSeverity(severity);
        payload.setCreatedTime(LocalDateTime.now());
        payload.setTargetType("all");

        SseMessage<NotificationPayload> message = SseMessage.notification(payload);
        sseConnectionPool.broadcast(message);
    }
}
