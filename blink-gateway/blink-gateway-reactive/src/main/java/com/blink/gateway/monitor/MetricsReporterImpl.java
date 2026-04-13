package com.blink.gateway.monitor;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.monitor.dto.MetricsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 指标上报服务实现
 * 从 Micrometer MeterRegistry 采集本地指标，异步推送到 Redis Stream
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class MetricsReporterImpl implements MetricsReporter {

    private static final String STREAM_KEY = "blink:gateway:metrics:stream";

    private final MeterRegistry meterRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:gateway-reactive}")
    private String serviceId;

    @Value("${server.port:8080}")
    private Integer port;

    @Value("${blink.metrics.report.enabled:true}")
    private boolean reportEnabled;

    @Value("${blink.metrics.report.interval-ms:30000}")
    private long reportIntervalMs;

    private final AtomicReference<String> instanceId = new AtomicReference<>();
    private final AtomicReference<String> hostAddress = new AtomicReference<>();

    public MetricsReporterImpl(MeterRegistry meterRegistry,
                               ReactiveStringRedisTemplate redisTemplate,
                               BuildProperties buildProperties) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 初始化实例信息
     */
    private void initInstanceInfo() {
        if (instanceId.get() == null) {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                hostAddress.set(host);
                String id = serviceId + "@" + host + ":" + port;
                instanceId.set(id);
                log.info("[MetricsReporter] 实例信息初始化完成 | instanceId: {}", id);
            } catch (Exception e) {
                log.error("[MetricsReporter] 获取主机地址失败", e);
                hostAddress.set("unknown");
                instanceId.set(serviceId + "@unknown:" + port);
            }
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${blink.metrics.report.interval-ms:30000}",
               initialDelayString = "${blink.metrics.report.initial-delay-ms:10000}")
    @Async("metricsReporterExecutor")
    public void reportMetrics() {
        if (!reportEnabled) {
            return;
        }

        initInstanceInfo();

        try {
            MetricsMessage message = collectMetrics();
            message.setType(MetricsMessage.MessageType.METRICS);
            asyncPushToStream(message).subscribe(
                    success -> log.debug("[MetricsReporter] 指标上报成功 | instanceId: {}", instanceId.get()),
                    error -> log.error("[MetricsReporter] 指标上报失败 | error: {}", error.getMessage())
            );
        } catch (Exception e) {
            log.error("[MetricsReporter] 采集指标失败", e);
        }
    }

    @Override
    public void sendRegisterMessage() {
        initInstanceInfo();

        MetricsMessage message = new MetricsMessage();
        message.setInstanceId(instanceId.get());
        message.setServiceId(serviceId);
        message.setHost(hostAddress.get());
        message.setPort(port);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(MetricsMessage.MessageType.REGISTER);
        message.setHealthStatus("UP");

        asyncPushToStream(message).subscribe(
                success -> log.info("[MetricsReporter] 实例注册消息发送成功 | instanceId: {}", instanceId.get()),
                error -> log.error("[MetricsReporter] 实例注册消息发送失败 | error: {}", error.getMessage())
        );
    }

    @Override
    @PreDestroy
    public void sendUnregisterMessage() {
        initInstanceInfo();

        MetricsMessage message = new MetricsMessage();
        message.setInstanceId(instanceId.get());
        message.setServiceId(serviceId);
        message.setHost(hostAddress.get());
        message.setPort(port);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(MetricsMessage.MessageType.UNREGISTER);
        message.setHealthStatus("DOWN");

        // 同步发送注销消息，确保应用关闭前发送成功
        try {
            pushToStreamSync(message);
            log.info("[MetricsReporter] 实例注销消息发送成功 | instanceId: {}", instanceId.get());
        } catch (Exception e) {
            log.error("[MetricsReporter] 实例注销消息发送失败", e);
        }
    }

    /**
     * 从 MeterRegistry 采集本地指标
     */
    private MetricsMessage collectMetrics() {
        MetricsMessage message = new MetricsMessage();
        message.setInstanceId(instanceId.get());
        message.setServiceId(serviceId);
        message.setHost(hostAddress.get());
        message.setPort(port);
        message.setTimestamp(System.currentTimeMillis());

        // 采集 JVM 内存指标
        collectMemoryMetrics(message);

        // 采集 CPU 指标
        collectCpuMetrics(message);

        // 采集 GC 指标
        collectGcMetrics(message);

        // 采集线程指标
        collectThreadMetrics(message);

        // 采集 HTTP 指标
        collectHttpMetrics(message);

        // 健康状态默认 UP（实际健康检查由 Actuator 提供）
        message.setHealthStatus("UP");

        return message;
    }

    /**
     * 采集 JVM 内存指标
     */
    private void collectMemoryMetrics(MetricsMessage message) {
        // 堆内存使用量
        Gauge heapUsedGauge = findGauge("jvm.memory.used", "area", "heap");
        if (heapUsedGauge != null) {
            message.setHeapUsed((long) heapUsedGauge.value());
        }

        // 堆内存最大值
        Gauge heapMaxGauge = findGauge("jvm.memory.max", "area", "heap");
        if (heapMaxGauge != null) {
            message.setHeapMax((long) heapMaxGauge.value());
        }

        // 计算堆内存使用率
        if (message.getHeapUsed() != null && message.getHeapMax() != null && message.getHeapMax() > 0) {
            double percent = (double) message.getHeapUsed() / message.getHeapMax() * 100;
            message.setHeapUsagePercent(BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        // 非堆内存使用量
        Gauge nonHeapUsedGauge = findGauge("jvm.memory.used", "area", "nonheap");
        if (nonHeapUsedGauge != null) {
            message.setNonHeapUsed((long) nonHeapUsedGauge.value());
        }
    }

    /**
     * 采集 CPU 指标
     */
    private void collectCpuMetrics(MetricsMessage message) {
        // 进程 CPU 使用率
        Gauge cpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        if (cpuGauge != null) {
            double cpuValue = cpuGauge.value() * 100;
            message.setCpuUsage(BigDecimal.valueOf(cpuValue).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        // 备用：系统 CPU 使用率
        if (message.getCpuUsage() == null) {
            Gauge systemCpuGauge = meterRegistry.find("system.cpu.usage").gauge();
            if (systemCpuGauge != null) {
                double cpuValue = systemCpuGauge.value() * 100;
                message.setCpuUsage(BigDecimal.valueOf(cpuValue).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
        }
    }

    /**
     * 采集 GC 指标
     */
    private void collectGcMetrics(MetricsMessage message) {
        // 年轻代 GC
        Counter youngGcCount = meterRegistry.find("jvm.gc.count").tag("gc", "G1 Young Generation").counter();
        if (youngGcCount == null) {
            youngGcCount = meterRegistry.find("jvm.gc.count").tag("gc", "PS Scavenge").counter();
        }
        if (youngGcCount != null) {
            message.setYoungGcCount((long) youngGcCount.count());
        }

        Gauge youngGcTime = meterRegistry.find("jvm.gc.time").tag("gc", "G1 Young Generation").gauge();
        if (youngGcTime == null) {
            youngGcTime = meterRegistry.find("jvm.gc.time").tag("gc", "PS Scavenge").gauge();
        }
        if (youngGcTime != null) {
            message.setYoungGcTime((long) youngGcTime.value());
        }

        // 老年代 GC
        Counter oldGcCount = meterRegistry.find("jvm.gc.count").tag("gc", "G1 Old Generation").counter();
        if (oldGcCount == null) {
            oldGcCount = meterRegistry.find("jvm.gc.count").tag("gc", "PS MarkSweep").counter();
        }
        if (oldGcCount != null) {
            message.setOldGcCount((long) oldGcCount.count());
        }

        Gauge oldGcTime = meterRegistry.find("jvm.gc.time").tag("gc", "G1 Old Generation").gauge();
        if (oldGcTime == null) {
            oldGcTime = meterRegistry.find("jvm.gc.time").tag("gc", "PS MarkSweep").gauge();
        }
        if (oldGcTime != null) {
            message.setOldGcTime((long) oldGcTime.value());
        }
    }

    /**
     * 采集线程指标
     */
    private void collectThreadMetrics(MetricsMessage message) {
        Gauge liveThreads = meterRegistry.find("jvm.threads.live").gauge();
        if (liveThreads != null) {
            message.setLiveThreads((int) liveThreads.value());
        }

        Gauge peakThreads = meterRegistry.find("jvm.threads.peak").gauge();
        if (peakThreads != null) {
            message.setPeakThreads((int) peakThreads.value());
        }

        Gauge daemonThreads = meterRegistry.find("jvm.threads.daemon").gauge();
        if (daemonThreads != null) {
            message.setDaemonThreads((int) daemonThreads.value());
        }
    }

    /**
     * 采集 HTTP 指标
     */
    private void collectHttpMetrics(MetricsMessage message) {
        // 从 http.server.requests 指标中提取统计信息
        long totalCount = 0;
        double totalTime = 0;

        for (Meter meter : meterRegistry.getMeters()) {
            if ("http.server.requests".equals(meter.getId().getName())) {
                if (meter instanceof Timer timer) {
                    totalCount += timer.count();
                    totalTime += timer.totalTime(TimeUnit.MILLISECONDS);
                }
            }
        }

        message.setTotalRequests(totalCount);
        // 简化处理：成功请求 = 总请求 - 失败请求（实际应该根据状态码统计）
        message.setSuccessRequests(totalCount);
        message.setFailedRequests(0L);

        if (totalCount > 0 && totalTime > 0) {
            message.setAvgResponseTime((long) (totalTime / totalCount));
        }
    }

    /**
     * 查找指定标签的 Gauge
     */
    private Gauge findGauge(String name, String tagKey, String tagValue) {
        return meterRegistry.find(name).tag(tagKey, tagValue).gauge();
    }

    /**
     * 异步推送到 Redis Stream
     */
    private Mono<Boolean> asyncPushToStream(MetricsMessage message) {
        try {
            Map<String, String> data = messageToMap(message);
            ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
                    .ofObject(data)
                    .withStreamKey(STREAM_KEY);

            return redisTemplate.opsForStream()
                    .add(record)
                    .map(result -> true)
                    .onErrorResume(e -> {
                        log.error("[MetricsReporter] Redis Stream 写入失败 | error: {}", e.getMessage());
                        return Mono.just(false);
                    });
        } catch (Exception e) {
            log.error("[MetricsReporter] 序列化消息失败", e);
            return Mono.just(false);
        }
    }

    /**
     * 同步推送到 Redis Stream（用于关闭时）
     */
    private void pushToStreamSync(MetricsMessage message) throws JsonProcessingException {
        Map<String, String> data = messageToMap(message);
        ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
                .ofObject(data)
                .withStreamKey(STREAM_KEY);

        redisTemplate.opsForStream()
                .add(record)
                .block();
    }

    /**
     * 消息转 Map
     */
    private Map<String, String> messageToMap(MetricsMessage message) {
        Map<String, String> data = new HashMap<>();
        data.put("instanceId", message.getInstanceId());
        data.put("serviceId", message.getServiceId());
        data.put("host", message.getHost());
        data.put("port", String.valueOf(message.getPort()));
        data.put("timestamp", String.valueOf(message.getTimestamp()));
        data.put("type", message.getType());
        data.put("healthStatus", message.getHealthStatus());

        if (message.getHeapUsed() != null) {
            data.put("heapUsed", String.valueOf(message.getHeapUsed()));
        }
        if (message.getHeapMax() != null) {
            data.put("heapMax", String.valueOf(message.getHeapMax()));
        }
        if (message.getHeapUsagePercent() != null) {
            data.put("heapUsagePercent", String.valueOf(message.getHeapUsagePercent()));
        }
        if (message.getNonHeapUsed() != null) {
            data.put("nonHeapUsed", String.valueOf(message.getNonHeapUsed()));
        }
        if (message.getCpuUsage() != null) {
            data.put("cpuUsage", String.valueOf(message.getCpuUsage()));
        }
        if (message.getYoungGcCount() != null) {
            data.put("youngGcCount", String.valueOf(message.getYoungGcCount()));
        }
        if (message.getYoungGcTime() != null) {
            data.put("youngGcTime", String.valueOf(message.getYoungGcTime()));
        }
        if (message.getOldGcCount() != null) {
            data.put("oldGcCount", String.valueOf(message.getOldGcCount()));
        }
        if (message.getOldGcTime() != null) {
            data.put("oldGcTime", String.valueOf(message.getOldGcTime()));
        }
        if (message.getLiveThreads() != null) {
            data.put("liveThreads", String.valueOf(message.getLiveThreads()));
        }
        if (message.getPeakThreads() != null) {
            data.put("peakThreads", String.valueOf(message.getPeakThreads()));
        }
        if (message.getDaemonThreads() != null) {
            data.put("daemonThreads", String.valueOf(message.getDaemonThreads()));
        }
        if (message.getTotalRequests() != null) {
            data.put("totalRequests", String.valueOf(message.getTotalRequests()));
        }
        if (message.getSuccessRequests() != null) {
            data.put("successRequests", String.valueOf(message.getSuccessRequests()));
        }
        if (message.getFailedRequests() != null) {
            data.put("failedRequests", String.valueOf(message.getFailedRequests()));
        }
        if (message.getAvgResponseTime() != null) {
            data.put("avgResponseTime", String.valueOf(message.getAvgResponseTime()));
        }

        return data;
    }
}
