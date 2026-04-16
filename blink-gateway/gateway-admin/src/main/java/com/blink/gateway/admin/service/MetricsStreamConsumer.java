package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.framework.redis.component.RedisClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.sse.InstanceStatusPayload;
import com.blink.gateway.admin.sse.NotificationPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.blink.gateway.admin.service.DashboardPushService;
import com.blink.gateway.admin.util.GatewayAdminUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_OFFLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_SHUTDOWN;
import static com.blink.gateway.admin.constants.ConfigValueConstant.METRICS_TTL_SECONDS;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CIRCUIT_BREAKER_HISTORY_KEY_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CIRCUIT_BREAKER_HISTORY_TTL_SECONDS;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CIRCUIT_BREAKER_KEY_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CIRCUIT_BREAKER_TTL_SECONDS;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_INSTANCE_LIST_KEY;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_SUMMARY;

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

    private final RedisClient redisClient;
    private final SseConnectionPool sseConnectionPool;
    private final InstanceStatusPushService instanceStatusPushService;
    private final GatewayInstanceMapper gatewayInstanceMapper;
    private final DashboardPushService dashboardPushService;
    private final TrafficIncrementService trafficIncrementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetricsStreamConsumer(RedisClient redisClient,
                                  SseConnectionPool sseConnectionPool,
                                  InstanceStatusPushService instanceStatusPushService,
                                  GatewayInstanceMapper gatewayInstanceMapper,
                                  DashboardPushService dashboardPushService,
                                  TrafficIncrementService trafficIncrementService) {
        this.redisClient = redisClient;
        this.sseConnectionPool = sseConnectionPool;
        this.instanceStatusPushService = instanceStatusPushService;
        this.gatewayInstanceMapper = gatewayInstanceMapper;
        this.dashboardPushService = dashboardPushService;
        this.trafficIncrementService = trafficIncrementService;
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

        // 校验实例是否在数据库中存在
        if (!ensureInstanceExists(message)) {
            log.warn("[MetricsStreamConsumer] 实例校验失败或被手动下线，跳过指标处理 | instanceId: {}", instanceId);
            return;
        }

        // 存储实例指标到 Redis Hash
        String metricsKey = GATEWAY_METRICS_PREFIX + instanceId;
        Map<String, Object> metricsData = convertToMetricsData(message);
        redisClient.hSet(metricsKey, metricsData);
        redisClient.expire(metricsKey, METRICS_TTL_SECONDS);

        // 处理熔断器指标
        String circuitBreakersJson = message.get("circuitBreakers");
        if (StrUtil.isNotBlank(circuitBreakersJson)) {
            storeCircuitBreakerMetrics(instanceId, circuitBreakersJson);
        }

        // 更新实例列表（使用 hPutField 设置单个字段）
        redisClient.hPutField(GATEWAY_INSTANCE_LIST_KEY, instanceId, String.valueOf(System.currentTimeMillis()));

        // 计算流量增量并存储（用于趋势图）
        calculateTrafficIncrement(message);

        // 更新汇总统计
        updateSummary();

        // 触发状态变化检测
        triggerStatusCheck();

        // 推送仪表盘数据给前端
        dashboardPushService.pushDashboardData();

        log.debug("[MetricsStreamConsumer] METRICS 消息处理完成 | instanceId: {}", instanceId);
    }

    /**
     * 计算流量增量
     */
    private void calculateTrafficIncrement(Map<String, String> message) {
        String instanceId = message.get("instanceId");

        // 解析请求数指标
        long totalRequests = parseMessageLong(message.get("totalRequests"));
        long successRequests = parseMessageLong(message.get("successRequests"));
        long failedRequests = parseMessageLong(message.get("failedRequests"));

        // 解析响应时间指标
        long avgResponseTime = parseMessageLong(message.get("avgResponseTime"));
        long p50ResponseTime = parseMessageLong(message.get("p50ResponseTime"));
        long p95ResponseTime = parseMessageLong(message.get("p95ResponseTime"));
        long p99ResponseTime = parseMessageLong(message.get("p99ResponseTime"));
        long maxResponseTime = parseMessageLong(message.get("maxResponseTime"));

        // 解析错误分类指标
        long error4xxCount = parseMessageLong(message.get("error4xxCount"));
        long error5xxCount = parseMessageLong(message.get("error5xxCount"));

        // 解析 QPS 指标
        int currentQps = parseMessageInt(message.get("currentQps"));

        // 计算并存储增量（使用扩展方法）
        trafficIncrementService.calculateAndStoreIncrementExtended(
                instanceId, totalRequests, successRequests, failedRequests,
                avgResponseTime, p50ResponseTime, p95ResponseTime, p99ResponseTime,
                maxResponseTime, error4xxCount, error5xxCount, currentQps);
    }

    /**
     * 解析消息中的 Int 值
     */
    private int parseMessageInt(String value) {
        if (StrUtil.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析消息中的 Long 值
     */
    private long parseMessageLong(String value) {
        if (StrUtil.isBlank(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 确保实例在数据库中存在
     *
     * @param message 消息内容
     * @return true-可以处理指标，false-跳过处理
     */
    private boolean ensureInstanceExists(Map<String, String> message) {
        String instanceId = message.get("instanceId");
        String serviceId = message.get("serviceId");
        String host = message.get("host");
        String portStr = message.get("port");

        // 根据 instanceId 字段查询数据库（注意：不能用 selectById，因为主键是 id 不是 instance_id）
        LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
        GatewayInstanceDO dbInstance = gatewayInstanceMapper.selectOne(queryWrapper);

        if (ObjectUtil.isNull(dbInstance)) {
            // 数据库中不存在，自动注册新实例
            Integer port = parsePort(portStr);
            if (StrUtil.isBlank(host) || port == null) {
                log.warn("[MetricsStreamConsumer] 消息缺少必要字段，无法自动注册 | instanceId: {}", instanceId);
                return false;
            }

            GatewayInstanceDO newInstance = new GatewayInstanceDO();
            newInstance.setInstanceId(instanceId);
            newInstance.setServiceId(StrUtil.isNotBlank(serviceId) ? serviceId : "unknown");
            newInstance.setHost(host);
            newInstance.setPort(port);
            newInstance.setUri("http://" + host + ":" + port);
            newInstance.setStatus(INSTANCE_STATUS_ONLINE);
            newInstance.setOnlineTime(LocalDateTime.now());
            gatewayInstanceMapper.insert(newInstance);

            log.info("[MetricsStreamConsumer] 自动注册新实例 | instanceId: {}, host: {}, port: {}", instanceId, host, port);

            // 广播新实例上线通知
            broadcastInstanceNotification(instanceId, serviceId, "实例自动注册上线", "success");

            return true;
        }

        // 实例已存在，检查是否被手动下线
        if (INSTANCE_STATUS_SHUTDOWN.equals(dbInstance.getStatus())) {
            // 手动下线的实例不处理指标，但记录日志
            log.info("[MetricsStreamConsumer] 实例已被手动下线，忽略指标上报 | instanceId: {}", instanceId);
            return false;
        }

        // 更新数据库状态为在线（如果之前是离线）
        if (!INSTANCE_STATUS_ONLINE.equals(dbInstance.getStatus())) {
            dbInstance.setStatus(INSTANCE_STATUS_ONLINE);
            dbInstance.setOnlineTime(LocalDateTime.now());
            gatewayInstanceMapper.updateById(dbInstance);
            log.info("[MetricsStreamConsumer] 实例状态更新为在线 | instanceId: {}", instanceId);
        }

        return true;
    }

    /**
     * 解析端口号
     */
    private Integer parsePort(String portStr) {
        if (StrUtil.isBlank(portStr)) {
            return null;
        }
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 处理 REGISTER 类型消息
     */
    private void handleRegister(Map<String, String> message) {
        String instanceId = message.get("instanceId");
        String serviceId = message.get("serviceId");

        // 校验并确保实例在数据库中存在
        if (!ensureInstanceExists(message)) {
            log.warn("[MetricsStreamConsumer] 实例注册校验失败 | instanceId: {}", instanceId);
            return;
        }

        // 注册实例到列表
        redisClient.hPutField(GATEWAY_INSTANCE_LIST_KEY, instanceId, String.valueOf(System.currentTimeMillis()));

        // 存储初始指标
        String metricsKey = GATEWAY_METRICS_PREFIX + instanceId;
        Map<String, Object> metricsData = convertToMetricsData(message);
        redisClient.hSet(metricsKey, metricsData);
        redisClient.expire(metricsKey, METRICS_TTL_SECONDS);

        // 广播新实例上线通知
        broadcastInstanceNotification(instanceId, serviceId, "实例上线", "success");

        // 触发状态变化检测
        triggerStatusCheck();

        // 推送仪表盘数据给前端
        dashboardPushService.pushDashboardData();

        log.info("[MetricsStreamConsumer] 实例注册 | instanceId: {}, serviceId: {}", instanceId, serviceId);
    }

    /**
     * 处理 UNREGISTER 类型消息
     */
    private void handleUnregister(Map<String, String> message) {
        String instanceId = message.get("instanceId");
        String serviceId = message.get("serviceId");

        // 从实例列表移除
        redisClient.hDeleteFields(GATEWAY_INSTANCE_LIST_KEY, instanceId);

        // 删除实例指标缓存
        redisClient.delete(GATEWAY_METRICS_PREFIX + instanceId);

        // 根据 instanceId 字段查询并更新数据库状态为离线
        LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
        GatewayInstanceDO dbInstance = gatewayInstanceMapper.selectOne(queryWrapper);

        if (ObjectUtil.isNotNull(dbInstance) && !INSTANCE_STATUS_SHUTDOWN.equals(dbInstance.getStatus())) {
            dbInstance.setStatus(INSTANCE_STATUS_OFFLINE);
            dbInstance.setOfflineTime(LocalDateTime.now());
            gatewayInstanceMapper.updateById(dbInstance);
            log.info("[MetricsStreamConsumer] 更新实例状态为离线 | instanceId: {}", instanceId);
        }

        // 广播实例下线通知
        broadcastInstanceNotification(instanceId, serviceId, "实例下线", "warning");

        // 触发状态变化检测
        triggerStatusCheck();

        // 推送仪表盘数据给前端
        dashboardPushService.pushDashboardData();

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

        // HTTP 响应时间分布
        copyIfPresent(data, message, "p50ResponseTime", Long.class);
        copyIfPresent(data, message, "p95ResponseTime", Long.class);
        copyIfPresent(data, message, "p99ResponseTime", Long.class);
        copyIfPresent(data, message, "maxResponseTime", Long.class);

        // HTTP 错误分类
        copyIfPresent(data, message, "error4xxCount", Long.class);
        copyIfPresent(data, message, "error5xxCount", Long.class);
        copyIfPresent(data, message, "errorRate", Double.class);

        // 实时 QPS
        copyIfPresent(data, message, "currentQps", Integer.class);

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
        Map<String, Object> instanceList = redisClient.hGetStringMap(GATEWAY_INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return;
        }

        int totalCount = instanceList.size();
        int onlineCount = 0;
        int healthyCount = 0;
        double totalCpu = 0;
        long totalRequests = 0;
        long totalSuccessRequests = 0;
        long totalFailedRequests = 0;
        long totalResponseTime = 0;
        int responseTimeCount = 0;

        for (String instanceId : instanceList.keySet()) {
            Map<String, Object> metrics = redisClient.hGetStringMap(GATEWAY_METRICS_PREFIX + instanceId);

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
                totalCpu += GatewayAdminUtil.toDoubleValue(cpuUsage);
            }

            // 统计请求数
            Object reqTotal = metrics.get("totalRequests");
            if (reqTotal != null) {
                totalRequests += GatewayAdminUtil.toLongValue(reqTotal);
            }

            Object reqSuccess = metrics.get("successRequests");
            if (reqSuccess != null) {
                totalSuccessRequests += GatewayAdminUtil.toLongValue(reqSuccess);
            }

            Object reqFailed = metrics.get("failedRequests");
            if (reqFailed != null) {
                totalFailedRequests += GatewayAdminUtil.toLongValue(reqFailed);
            }

            Object avgRespTime = metrics.get("avgResponseTime");
            if (avgRespTime != null && GatewayAdminUtil.toLongValue(avgRespTime) > 0) {
                totalResponseTime += GatewayAdminUtil.toLongValue(avgRespTime);
                responseTimeCount++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", totalCount);
        summary.put("online", onlineCount);
        summary.put("healthy", healthyCount);
        summary.put("avgCpuUsage", onlineCount > 0 ?
                BigDecimal.valueOf(totalCpu / onlineCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
        // 请求数统计
        summary.put("totalRequests", totalRequests);
        summary.put("totalSuccessRequests", totalSuccessRequests);
        summary.put("totalFailedRequests", totalFailedRequests);
        summary.put("avgResponseTime", responseTimeCount > 0 ?
                BigDecimal.valueOf(totalResponseTime / responseTimeCount).setScale(0, RoundingMode.HALF_UP).longValue() : 0L);
        summary.put("timestamp", System.currentTimeMillis());

        redisClient.hSet(GATEWAY_METRICS_SUMMARY, summary);
        redisClient.expire(GATEWAY_METRICS_SUMMARY, METRICS_TTL_SECONDS);
    }

    /**
     * 触发状态变化检测
     */
    private void triggerStatusCheck() {
        Map<String, Object> instanceList = redisClient.hGetStringMap(GATEWAY_INSTANCE_LIST_KEY);

        if (CollUtil.isEmpty(instanceList)) {
            return;
        }

        List<InstanceStatusPayload.InstanceSummary> summaries = new ArrayList<>();

        for (String instanceId : instanceList.keySet()) {
            Map<String, Object> metrics = redisClient.hGetStringMap(GATEWAY_METRICS_PREFIX + instanceId);

            InstanceStatusPayload.InstanceSummary summary = new InstanceStatusPayload.InstanceSummary();
            summary.setInstanceId(instanceId);

            if (CollUtil.isNotEmpty(metrics)) {
                Object status = metrics.get("status");
                if (status != null) {
                    summary.setStatus(GatewayAdminUtil.toIntValue(status));
                }

                summary.setHealthStatus((String) metrics.get("healthStatus"));

                Object cpuUsage = metrics.get("cpuUsage");
                if (cpuUsage != null) {
                    summary.setCpuUsage(GatewayAdminUtil.toDoubleValue(cpuUsage));
                }

                Object heapUsagePercent = metrics.get("heapUsagePercent");
                if (heapUsagePercent != null) {
                    summary.setHeapUsagePercent(GatewayAdminUtil.toDoubleValue(heapUsagePercent));
                }

                Object timestamp = metrics.get("timestamp");
                if (timestamp != null) {
                    summary.setTimestamp(GatewayAdminUtil.toLongValue(timestamp));
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

    // ==================== 熔断器指标处理 ====================

    /**
     * 存储熔断器指标到 Redis
     *
     * @param instanceId           实例ID
     * @param circuitBreakersJson  熔断器指标JSON数组
     */
    private void storeCircuitBreakerMetrics(String instanceId, String circuitBreakersJson) {
        try {
            // 解析 JSON 数组
            List<Map<String, Object>> metrics = objectMapper.readValue(
                    circuitBreakersJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            if (CollUtil.isEmpty(metrics)) {
                return;
            }

            // 存储到 Redis Hash: blink:gateway:circuitbreaker:{instanceId}
            String cbKey = CIRCUIT_BREAKER_KEY_PREFIX + instanceId;
            Map<String, Object> cbData = new HashMap<>();
            cbData.put("timestamp", System.currentTimeMillis());

            for (Map<String, Object> metric : metrics) {
                String cbName = (String) metric.get("name");
                if (StrUtil.isNotBlank(cbName)) {
                    // 检测状态变化
                    checkStateTransition(instanceId, cbName, metric);
                    // 存储单个熔断器指标（JSON 字符串）
                    cbData.put(cbName, objectMapper.writeValueAsString(metric));
                }
            }

            redisClient.hSet(cbKey, cbData);
            redisClient.expire(cbKey, CIRCUIT_BREAKER_TTL_SECONDS);

            log.debug("[MetricsStreamConsumer] 存储熔断器指标 | instanceId: {}, count: {}",
                    instanceId, metrics.size());

        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 解析熔断器指标失败 | error: {}", e.getMessage());
        }
    }

    /**
     * 检测状态转换
     *
     * @param instanceId 实例ID
     * @param cbName     熔断器名称
     * @param newMetric  新指标数据
     */
    private void checkStateTransition(String instanceId, String cbName, Map<String, Object> newMetric) {
        try {
            String cbKey = CIRCUIT_BREAKER_KEY_PREFIX + instanceId;
            String oldMetricJson = (String) redisClient.hGetField(cbKey, cbName);

            if (StrUtil.isBlank(oldMetricJson)) {
                return;
            }

            Map<String, Object> oldMetric = objectMapper.readValue(oldMetricJson,
                    new TypeReference<Map<String, Object>>() {});

            String oldState = (String) oldMetric.get("state");
            String newState = (String) newMetric.get("state");

            if (!StrUtil.equals(oldState, newState)) {
                // 记录状态转换历史
                recordStateTransition(instanceId, cbName, oldState, newState, newMetric);

                // 触发告警
                triggerCircuitBreakerAlert(instanceId, cbName, oldState, newState, newMetric);
            }
        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 检测状态转换失败 | error: {}", e.getMessage());
        }
    }

    /**
     * 记录状态转换历史
     *
     * @param instanceId 实例ID
     * @param cbName     熔断器名称
     * @param fromState  原状态
     * @param toState    新状态
     * @param metric     指标数据
     */
    private void recordStateTransition(String instanceId, String cbName,
                                        String fromState, String toState,
                                        Map<String, Object> metric) {
        try {
            String historyKey = CIRCUIT_BREAKER_HISTORY_KEY_PREFIX + instanceId + ":" + cbName;

            Map<String, Object> transition = new HashMap<>();
            transition.put("from", fromState);
            transition.put("to", toState);
            transition.put("time", System.currentTimeMillis());
            transition.put("reason", buildTransitionReason(fromState, toState));
            transition.put("failureRate", metric.get("failureRate"));
            transition.put("numberOfCalls", metric.get("numberOfCalls"));

            String json = objectMapper.writeValueAsString(transition);
            redisClient.lPush(historyKey, json);
            redisClient.expire(historyKey, CIRCUIT_BREAKER_HISTORY_TTL_SECONDS);

            log.info("[MetricsStreamConsumer] 状态转换记录 | instance: {}, cb: {}, {} -> {}",
                    instanceId, cbName, fromState, toState);

        } catch (Exception e) {
            log.error("[MetricsStreamConsumer] 记录状态转换历史失败", e);
        }
    }

    /**
     * 构建状态转换原因
     *
     * @param fromState 原状态
     * @param toState   新状态
     * @return 转换原因
     */
    private String buildTransitionReason(String fromState, String toState) {
        if ("CLOSED".equals(fromState) && "OPEN".equals(toState)) {
            return "failureRate_exceeded";
        } else if ("OPEN".equals(fromState) && "HALF_OPEN".equals(toState)) {
            return "waitDurationElapsed";
        } else if ("HALF_OPEN".equals(fromState) && "OPEN".equals(toState)) {
            return "probe_failed";
        } else if ("HALF_OPEN".equals(fromState) && "CLOSED".equals(toState)) {
            return "probe_succeeded";
        }
        return fromState.toLowerCase() + "_to_" + toState.toLowerCase();
    }

    /**
     * 触发熔断器告警
     *
     * @param instanceId 实例ID
     * @param cbName     熔断器名称
     * @param fromState  原状态
     * @param toState    新状态
     * @param metric     指标数据
     */
    private void triggerCircuitBreakerAlert(String instanceId, String cbName,
                                            String fromState, String toState,
                                            Map<String, Object> metric) {
        String severity = determineAlertSeverity(fromState, toState);

        if (severity == null) {
            return;
        }

        NotificationPayload payload = new NotificationPayload();
        payload.setTitle("熔断器状态变化");
        payload.setContent(String.format("实例 %s 的 %s 从 %s 变为 %s",
                instanceId, cbName, fromState, toState));
        payload.setSeverity(severity);
        payload.setCreatedTime(LocalDateTime.now());
        payload.setTargetType("all");

        SseMessage<NotificationPayload> message = SseMessage.notification(payload);
        sseConnectionPool.broadcast(message);

        log.info("[MetricsStreamConsumer] 触发熔断器告警 | instance: {}, cb: {}, severity: {}",
                instanceId, cbName, severity);
    }

    /**
     * 确定告警级别
     *
     * @param fromState 原状态
     * @param toState   新状态
     * @return 告警级别，null表示无需告警
     */
    private String determineAlertSeverity(String fromState, String toState) {
        if ("CLOSED".equals(fromState) && "OPEN".equals(toState)) {
            return "warning";
        } else if ("HALF_OPEN".equals(fromState) && "OPEN".equals(toState)) {
            return "error";
        }
        return null;
    }
}
