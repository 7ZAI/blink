package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.lock.DistributedLockClient;
import com.blink.gateway.admin.entity.GatewayMetricsHistoryDO;
import com.blink.gateway.admin.mapper.GatewayMetricsHistoryMapper;
import com.blink.gateway.admin.service.MetricsCollectorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_COLLECT_LOCK;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_SUMMARY;

/**
 * 网关指标采集服务实现
 * 定时采集 gateway-reactive 实例的 actuator 监控指标
 *
 * @author binblink
 */
@Service
@Slf4j
public class MetricsCollectorServiceImpl implements MetricsCollectorService {

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GatewayMetricsHistoryMapper metricsHistoryMapper;

    @Resource
    private DistributedLockClient distributedLockClient;

    private static final String DEFAULT_GATEWAY_SERVICE_NAME = "gateway-app";

    @Value("${blink.gateway.monitor.gateway-service-name:" + DEFAULT_GATEWAY_SERVICE_NAME + "}")
    private String gatewayServiceName;

    /**
     * 采集任务分布式锁 Key
     */
    private static final String COLLECT_LOCK_KEY = GATEWAY_METRICS_COLLECT_LOCK;

    @Value("${blink.gateway.monitor.actuator-username:admin}")
    private String actuatorUsername;

    /**
     * Actuator 认证密码，生产环境应通过环境变量配置
     */
    @Value("${blink.gateway.monitor.actuator-password:}")
    private String actuatorPassword;

    @Value("${blink.gateway.monitor.metrics-expire-seconds:60}")
    private int metricsExpireSeconds;

    @Value("${blink.gateway.monitor.history-retention-days:7}")
    private int historyRetentionDays;

    /**
     * WebClient 实例，直接初始化避免线程安全问题
     */
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Scheduled(fixedDelayString = "${blink.gateway.monitor.collect-interval-ms:30000}")
    public void collectMetrics() {
        // 使用 DistributedLockClient 获取分布式锁（非阻塞）
        boolean locked = distributedLockClient.tryLock(COLLECT_LOCK_KEY);
        if (!locked) {
            log.debug("[MetricsCollector] 其他实例正在采集，跳过本次");
            return;
        }

        try {
            doCollectMetrics();
        } finally {
            distributedLockClient.unlock(COLLECT_LOCK_KEY);
        }
    }

    /**
     * 执行指标采集逻辑
     */
    private void doCollectMetrics() {
        log.debug("[MetricsCollector] 开始采集网关指标...");
        long startTime = System.currentTimeMillis();

        List<ServiceInstance> instances = discoveryClient.getInstances(gatewayServiceName);
        if (CollUtil.isEmpty(instances)) {
            log.warn("[MetricsCollector] 未发现网关实例");
            return;
        }

        int successCount = 0;
        int failCount = 0;
        long totalCpu = 0;
        long totalMemory = 0;
        long totalRequests = 0;
        long totalSuccessRequests = 0;
        long totalFailedRequests = 0;
        int healthyInstances = 0;

        for (ServiceInstance instance : instances) {
            try {
                InstanceMetrics metrics = collectInstanceMetrics(instance);
                if (ObjectUtil.isNotNull(metrics)) {
                    // 存储到 Redis
                    saveToRedis(instance.getInstanceId(), metrics);

                    // 存储到 MySQL
                    saveToMySQL(instance, metrics);

                    // 汇总统计
                    if ("UP".equals(metrics.healthStatus)) {
                        healthyInstances++;
                    }
                    if (ObjectUtil.isNotNull(metrics.cpuUsage)) {
                        totalCpu += metrics.cpuUsage.multiply(BigDecimal.valueOf(100)).longValue();
                    }
                    if (ObjectUtil.isNotNull(metrics.memoryUsed)) {
                        totalMemory += metrics.memoryUsed;
                    }
                    totalRequests += ObjectUtil.isNotNull(metrics.totalRequests) ? metrics.totalRequests : 0;
                    totalSuccessRequests += ObjectUtil.isNotNull(metrics.successRequests) ? metrics.successRequests : 0;
                    totalFailedRequests += ObjectUtil.isNotNull(metrics.failedRequests) ? metrics.failedRequests : 0;

                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("[MetricsCollector] 采集实例指标失败 | instanceId: {}, error: {}",
                        instance.getInstanceId(), e.getMessage());
                failCount++;
            }
        }

        // 更新汇总统计
        saveSummaryToRedis(instances.size(), healthyInstances, totalCpu, totalMemory,
                totalRequests, totalSuccessRequests, totalFailedRequests);

        long costTime = System.currentTimeMillis() - startTime;
        log.info("[MetricsCollector] 采集完成 | 成功: {}, 失败: {}, 耗时: {}ms",
                successCount, failCount, costTime);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanHistoryMetrics() {
        try {
            log.info("[MetricsCollector] 开始清理历史数据...");
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(historyRetentionDays);
            int deleted = metricsHistoryMapper.deleteBeforeTime(beforeTime);
            log.info("[MetricsCollector] 清理历史数据完成 | 删除记录数: {}", deleted);
        } catch (Exception e) {
            log.error("[MetricsCollector] 清理历史数据失败", e);
        }
    }

    /**
     * 采集单个实例的指标
     */
    private InstanceMetrics collectInstanceMetrics(ServiceInstance instance) {
        String baseUrl = instance.getUri().toString();
        String instanceId = instance.getInstanceId();

        try {
            InstanceMetrics metrics = new InstanceMetrics();

            // 1. 获取健康状态
            Map<String, Object> health = fetchActuatorEndpoint(baseUrl + "/actuator/health");
            if (MapUtil.isNotEmpty(health) && health.containsKey("status")) {
                metrics.healthStatus = (String) health.get("status");
            }

            // 2. 获取 JVM 内存指标
            Map<String, Object> memoryMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.used");
            if (MapUtil.isNotEmpty(memoryMetrics)) {
                metrics.memoryUsed = extractMeasureValue(memoryMetrics);
            }

            Map<String, Object> memoryMaxMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.max");
            if (MapUtil.isNotEmpty(memoryMaxMetrics)) {
                metrics.memoryMax = extractMeasureValue(memoryMaxMetrics);
            }

            // 3. 获取 CPU 使用率
            Map<String, Object> cpuMetrics = fetchMetrics(baseUrl + "/actuator/metrics/process.cpu.usage");
            if (MapUtil.isNotEmpty(cpuMetrics)) {
                Double cpuValue = extractMeasureValueAsDouble(cpuMetrics);
                if (ObjectUtil.isNotNull(cpuValue)) {
                    metrics.cpuUsage = BigDecimal.valueOf(cpuValue * 100).setScale(2, RoundingMode.HALF_UP);
                }
            }

            // 4. 获取 HTTP 请求统计
            Map<String, Object> httpMetrics = fetchMetrics(baseUrl + "/actuator/metrics/http.server.requests");
            if (MapUtil.isNotEmpty(httpMetrics)) {
                extractHttpMetrics(httpMetrics, metrics);
            }

            // 5. 获取堆内存和非堆内存指标
            Map<String, Object> heapUsedMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:heap");
            if (MapUtil.isNotEmpty(heapUsedMetrics)) {
                metrics.heapUsed = extractMeasureValue(heapUsedMetrics);
            }

            Map<String, Object> heapMaxMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.max?tag=area:heap");
            if (MapUtil.isNotEmpty(heapMaxMetrics)) {
                metrics.heapMax = extractMeasureValue(heapMaxMetrics);
            }

            Map<String, Object> nonHeapMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.memory.used?tag=area:nonheap");
            if (MapUtil.isNotEmpty(nonHeapMetrics)) {
                metrics.nonHeapUsed = extractMeasureValue(nonHeapMetrics);
            }

            // 6. 获取 GC 指标
            try {
                Map<String, Object> youngGcMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.count?tag=gc:G1 Young Generation");
                if (MapUtil.isNotEmpty(youngGcMetrics)) {
                    metrics.youngGcCount = extractMeasureValue(youngGcMetrics);
                }

                Map<String, Object> youngGcTimeMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.time?tag=gc:G1 Young Generation");
                if (MapUtil.isNotEmpty(youngGcTimeMetrics)) {
                    metrics.youngGcTime = extractMeasureValue(youngGcTimeMetrics);
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取年轻代 GC 指标失败 | instanceId: {}", instanceId);
            }

            try {
                Map<String, Object> oldGcMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.count?tag=gc:G1 Old Generation");
                if (MapUtil.isNotEmpty(oldGcMetrics)) {
                    metrics.oldGcCount = extractMeasureValue(oldGcMetrics);
                }

                Map<String, Object> oldGcTimeMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.gc.time?tag=gc:G1 Old Generation");
                if (MapUtil.isNotEmpty(oldGcTimeMetrics)) {
                    metrics.oldGcTime = extractMeasureValue(oldGcTimeMetrics);
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取老年代 GC 指标失败 | instanceId: {}", instanceId);
            }

            // 7. 获取线程指标
            try {
                Map<String, Object> liveThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.live");
                if (MapUtil.isNotEmpty(liveThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(liveThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.liveThreads = ((Number) value).intValue();
                    }
                }

                Map<String, Object> peakThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.peak");
                if (MapUtil.isNotEmpty(peakThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(peakThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.peakThreads = ((Number) value).intValue();
                    }
                }

                Map<String, Object> daemonThreadsMetrics = fetchMetrics(baseUrl + "/actuator/metrics/jvm.threads.daemon");
                if (MapUtil.isNotEmpty(daemonThreadsMetrics)) {
                    Object value = extractMeasureValueAsObject(daemonThreadsMetrics);
                    if (ObjectUtil.isNotNull(value)) {
                        metrics.daemonThreads = ((Number) value).intValue();
                    }
                }
            } catch (Exception e) {
                log.debug("[MetricsCollector] 获取线程指标失败 | instanceId: {}", instanceId);
            }

            // 8. 熔断器状态（暂不支持实时采集，默认 CLOSED）
            // TODO: 后续可通过 /actuator/circuitbreakers 端点获取实际状态
            metrics.circuitBreakerState = "CLOSED";

            metrics.collectTime = System.currentTimeMillis();

            log.debug("[MetricsCollector] 采集实例指标成功 | instanceId: {}, cpu: {}%, memory: {}MB",
                    instanceId, metrics.cpuUsage,
                    ObjectUtil.isNotNull(metrics.memoryUsed) ? metrics.memoryUsed / 1024 / 1024 : "N/A");

            return metrics;

        } catch (Exception e) {
            log.warn("[MetricsCollector] 采集实例指标失败 | instanceId: {}, error: {}",
                    instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 actuator 端点
     */
    private Map<String, Object> fetchActuatorEndpoint(String url) {
        try {
            String response = webClient.get()
                    .uri(url)
                    .headers(headers -> {
                        if (StrUtil.isNotBlank(actuatorPassword)) {
                            headers.setBasicAuth(actuatorUsername, actuatorPassword);
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (StrUtil.isNotBlank(response)) {
                return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("[MetricsCollector] 调用端点失败 | url: {}, error: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * 获取 metrics 端点数据
     */
    private Map<String, Object> fetchMetrics(String url) {
        return fetchActuatorEndpoint(url);
    }

    /**
     * 从 metrics 响应中提取测量值
     */
    @SuppressWarnings("unchecked")
    private Long extractMeasureValue(Map<String, Object> metrics) {
        try {
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) metrics.get("measurements");
            if (CollUtil.isNotEmpty(measurements)) {
                Object value = measurements.get(0).get("value");
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
        } catch (Exception e) {
            log.debug("[MetricsCollector] 提取测量值失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 metrics 响应中提取测量值（Double 类型）
     */
    @SuppressWarnings("unchecked")
    private Double extractMeasureValueAsDouble(Map<String, Object> metrics) {
        try {
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) metrics.get("measurements");
            if (CollUtil.isNotEmpty(measurements)) {
                Object value = measurements.get(0).get("value");
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }
        } catch (Exception e) {
            log.debug("[MetricsCollector] 提取测量值失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 metrics 响应中提取测量值（Object 类型）
     */
    @SuppressWarnings("unchecked")
    private Object extractMeasureValueAsObject(Map<String, Object> metrics) {
        try {
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) metrics.get("measurements");
            if (CollUtil.isNotEmpty(measurements)) {
                return measurements.get(0).get("value");
            }
        } catch (Exception e) {
            log.debug("[MetricsCollector] 提取测量值失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 HTTP 请求指标中提取统计数据
     */
    @SuppressWarnings("unchecked")
    private void extractHttpMetrics(Map<String, Object> httpMetrics, InstanceMetrics metrics) {
        try {
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) httpMetrics.get("measurements");
            if (CollUtil.isEmpty(measurements)) {
                return;
            }

            long totalCount = 0;
            double totalTime = 0;

            for (Map<String, Object> measurement : measurements) {
                String statistic = (String) measurement.get("statistic");
                Object value = measurement.get("value");

                if (value instanceof Number) {
                    if ("COUNT".equals(statistic)) {
                        totalCount = ((Number) value).longValue();
                    } else if ("TOTAL_TIME".equals(statistic) || "SUM".equals(statistic)) {
                        totalTime = ((Number) value).doubleValue();
                    }
                }
            }

            metrics.totalRequests = totalCount;
            // 响应时间单位转换：秒 -> 毫秒
            if (totalCount > 0 && totalTime > 0) {
                metrics.avgResponseTime = (long) (totalTime * 1000 / totalCount);
            }

        } catch (Exception e) {
            log.debug("[MetricsCollector] 提取 HTTP 指标失败: {}", e.getMessage());
        }
    }

    /**
     * 存储到 Redis
     */
    private void saveToRedis(String instanceId, InstanceMetrics metrics) {
        String key = GATEWAY_METRICS_PREFIX + instanceId;

        Map<String, Object> data = new HashMap<>();
        if (ObjectUtil.isNotNull(metrics.cpuUsage)) {
            data.put("cpuUsage", metrics.cpuUsage.doubleValue());
        }
        if (ObjectUtil.isNotNull(metrics.memoryUsed)) {
            data.put("memoryUsed", metrics.memoryUsed);
        }
        if (ObjectUtil.isNotNull(metrics.memoryMax)) {
            data.put("memoryMax", metrics.memoryMax);
        }

        // 存储堆内存和非堆内存指标
        if (ObjectUtil.isNotNull(metrics.heapUsed)) {
            data.put("heapUsed", metrics.heapUsed);
        }
        if (ObjectUtil.isNotNull(metrics.heapMax)) {
            data.put("heapMax", metrics.heapMax);
        }
        if (ObjectUtil.isNotNull(metrics.nonHeapUsed)) {
            data.put("nonHeapUsed", metrics.nonHeapUsed);
        }

        if (ObjectUtil.isNotNull(metrics.totalRequests)) {
            data.put("totalRequests", metrics.totalRequests);
        }
        if (ObjectUtil.isNotNull(metrics.successRequests)) {
            data.put("successRequests", metrics.successRequests);
        }
        if (ObjectUtil.isNotNull(metrics.failedRequests)) {
            data.put("failedRequests", metrics.failedRequests);
        }
        if (ObjectUtil.isNotNull(metrics.avgResponseTime)) {
            data.put("avgResponseTime", metrics.avgResponseTime);
        }

        // 存储 GC 指标
        if (ObjectUtil.isNotNull(metrics.youngGcCount)) {
            data.put("youngGcCount", metrics.youngGcCount);
        }
        if (ObjectUtil.isNotNull(metrics.youngGcTime)) {
            data.put("youngGcTime", metrics.youngGcTime);
        }
        if (ObjectUtil.isNotNull(metrics.oldGcCount)) {
            data.put("oldGcCount", metrics.oldGcCount);
        }
        if (ObjectUtil.isNotNull(metrics.oldGcTime)) {
            data.put("oldGcTime", metrics.oldGcTime);
        }

        // 存储线程指标
        if (ObjectUtil.isNotNull(metrics.liveThreads)) {
            data.put("liveThreads", metrics.liveThreads);
        }
        if (ObjectUtil.isNotNull(metrics.peakThreads)) {
            data.put("peakThreads", metrics.peakThreads);
        }
        if (ObjectUtil.isNotNull(metrics.daemonThreads)) {
            data.put("daemonThreads", metrics.daemonThreads);
        }

        if (StrUtil.isNotBlank(metrics.healthStatus)) {
            data.put("healthStatus", metrics.healthStatus);
        }
        if (StrUtil.isNotBlank(metrics.circuitBreakerState)) {
            data.put("circuitBreakerState", metrics.circuitBreakerState);
        }
        data.put("timestamp", metrics.collectTime);

        redisClient.hSet(key, data);
        redisClient.expire(key, metricsExpireSeconds);
    }

    /**
     * 存储汇总统计到 Redis
     */
    private void saveSummaryToRedis(int totalInstances, int healthyInstances,
                                    long totalCpu, long totalMemory,
                                    long totalRequests, long totalSuccessRequests, long totalFailedRequests) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalInstances", totalInstances);
        summary.put("healthyInstances", healthyInstances);
        summary.put("totalCpuAvg", totalInstances > 0 ? totalCpu / totalInstances / 100.0 : 0);
        summary.put("totalMemoryUsed", totalMemory);
        summary.put("totalRequests", totalRequests);
        summary.put("totalSuccessRequests", totalSuccessRequests);
        summary.put("totalFailedRequests", totalFailedRequests);
        summary.put("lastUpdateTime", System.currentTimeMillis());

        redisClient.hSet(GATEWAY_METRICS_SUMMARY, summary);
        redisClient.expire(GATEWAY_METRICS_SUMMARY, metricsExpireSeconds);
    }

    /**
     * 存储到 MySQL
     */
    private void saveToMySQL(ServiceInstance instance, InstanceMetrics metrics) {
        try {
            GatewayMetricsHistoryDO history = new GatewayMetricsHistoryDO();
            history.setInstanceId(instance.getInstanceId());
            history.setHost(instance.getHost());
            history.setPort(instance.getPort());
            history.setCpuUsage(metrics.cpuUsage);
            history.setMemoryUsed(metrics.memoryUsed);
            history.setMemoryMax(metrics.memoryMax);
            history.setTotalRequests(ObjectUtil.isNotNull(metrics.totalRequests) ? metrics.totalRequests : 0L);
            history.setSuccessRequests(ObjectUtil.isNotNull(metrics.successRequests) ? metrics.successRequests : 0L);
            history.setFailedRequests(ObjectUtil.isNotNull(metrics.failedRequests) ? metrics.failedRequests : 0L);
            history.setAvgResponseTime(ObjectUtil.isNotNull(metrics.avgResponseTime) ? metrics.avgResponseTime : 0L);
            history.setHealthStatus(metrics.healthStatus);
            history.setCircuitBreakerState(metrics.circuitBreakerState);
            history.setCollectTime(LocalDateTime.now());

            metricsHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("[MetricsCollector] 保存历史数据失败 | instanceId: {}", instance.getInstanceId(), e);
        }
    }

    /**
     * 实例指标内部类
     */
    private static class InstanceMetrics {
        /** CPU 使用率 (%) */
        BigDecimal cpuUsage;
        /** 已用内存 (bytes) */
        Long memoryUsed;
        /** 最大内存 (bytes) */
        Long memoryMax;
        /** 堆内存使用量 (bytes) */
        Long heapUsed;
        /** 堆内存最大值 (bytes) */
        Long heapMax;
        /** 非堆内存使用量 (bytes) */
        Long nonHeapUsed;
        /** 请求总数 */
        Long totalRequests;
        /** 成功请求数 */
        Long successRequests;
        /** 失败请求数 */
        Long failedRequests;
        /** 平均响应时间 (ms) */
        Long avgResponseTime;
        /** 健康状态 */
        String healthStatus;
        /** 熔断器状态 */
        String circuitBreakerState;
        /** 年轻代 GC 次数 */
        Long youngGcCount;
        /** 年轻代 GC 时间 (ms) */
        Long youngGcTime;
        /** 老年代 GC 次数 */
        Long oldGcCount;
        /** 老年代 GC 时间 (ms) */
        Long oldGcTime;
        /** 活跃线程数 */
        Integer liveThreads;
        /** 峰值线程数 */
        Integer peakThreads;
        /** 守护线程数 */
        Integer daemonThreads;
        /** 采集时间戳 */
        Long collectTime;
    }
}