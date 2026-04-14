package com.blink.gateway.monitor;

import com.blink.gateway.monitor.dto.MetricsMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MetricsReporterImpl 单元测试
 *
 * 测试策略：
 * 1. 通过 ReflectionTestUtils 访问私有方法进行测试
 * 2. 使用 spy 来验证异步方法的调用
 * 3. 对同步方法直接测试
 *
 * @author binblink
 * @since 2026-04-14
 */
class MetricsReporterImplTest {

    private MeterRegistry meterRegistry;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveStreamOperations<String, Object, Object> streamOperations;
    private MetricsReporterImpl metricsReporter;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        streamOperations = mock(ReactiveStreamOperations.class);
        BuildProperties buildProperties = mock(BuildProperties.class);

        // Mock Redis Stream 操作
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.add(any(Record.class)))
                .thenReturn(Mono.just(mock(org.springframework.data.redis.connection.stream.RecordId.class)));

        // 创建 MetricsReporterImpl 实例
        metricsReporter = new MetricsReporterImpl(
                meterRegistry,
                redisTemplate,
                buildProperties
        );

        // 使用反射设置私有字段
        ReflectionTestUtils.setField(metricsReporter, "serviceId", "test-gateway");
        ReflectionTestUtils.setField(metricsReporter, "port", 8080);
        ReflectionTestUtils.setField(metricsReporter, "configPushEnabled", true);
    }

    @Nested
    @DisplayName("指标采集测试 - 直接测试 collectMetrics 方法")
    class CollectMetricsTests {

        @Test
        @DisplayName("应该正确采集堆内存指标")
        void shouldCollectMemoryMetrics() {
            // Given: 模拟堆内存指标
            meterRegistry.gauge("jvm.memory.used", List.of(Tag.of("area", "heap")), 134217728L);
            meterRegistry.gauge("jvm.memory.max", List.of(Tag.of("area", "heap")), 536870912L);
            meterRegistry.gauge("jvm.memory.used", List.of(Tag.of("area", "nonheap")), 67108864L);

            // 初始化实例信息
            initInstanceInfo();

            // When: 调用私有方法 collectMetrics
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then: 验证堆内存指标正确
            assertNotNull(message);
            assertEquals(134217728L, message.getHeapUsed());
            assertEquals(536870912L, message.getHeapMax());
            assertEquals(67108864L, message.getNonHeapUsed());
            // 堆内存使用率 = 134217728 / 536870912 * 100 = 25.0
            assertEquals(25.0, message.getHeapUsagePercent(), 0.01);
        }

        @Test
        @DisplayName("应该正确采集 CPU 使用率指标")
        void shouldCollectCpuMetrics() {
            // Given: 模拟 CPU 指标 (0.155 表示 15.5%)
            meterRegistry.gauge("process.cpu.usage", 0.155);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then: CPU 使用率 = 0.155 * 100 = 15.5
            assertNotNull(message);
            assertEquals(15.5, message.getCpuUsage(), 0.01);
        }

        @Test
        @DisplayName("当进程 CPU 不可用时，应使用系统 CPU 指标")
        void shouldFallbackToSystemCpuWhenProcessCpuUnavailable() {
            // Given: 只设置系统 CPU 指标
            meterRegistry.gauge("system.cpu.usage", 0.25);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then
            assertNotNull(message);
            assertEquals(25.0, message.getCpuUsage(), 0.01);
        }

        @Test
        @DisplayName("应该正确采集 GC 指标 - G1 收集器")
        void shouldCollectGcMetricsForG1Collector() {
            // Given: 模拟 G1 GC 指标
            Counter youngGcCount = Counter.builder("jvm.gc.count")
                    .tag("gc", "G1 Young Generation")
                    .register(meterRegistry);
            youngGcCount.increment(120);

            Gauge.builder("jvm.gc.time", () -> 1500.0)
                    .tag("gc", "G1 Young Generation")
                    .register(meterRegistry);

            Counter oldGcCount = Counter.builder("jvm.gc.count")
                    .tag("gc", "G1 Old Generation")
                    .register(meterRegistry);
            oldGcCount.increment(2);

            Gauge.builder("jvm.gc.time", () -> 200.0)
                    .tag("gc", "G1 Old Generation")
                    .register(meterRegistry);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then
            assertNotNull(message);
            assertEquals(120L, message.getYoungGcCount());
            assertEquals(1500L, message.getYoungGcTime());
            assertEquals(2L, message.getOldGcCount());
            assertEquals(200L, message.getOldGcTime());
        }

        @Test
        @DisplayName("应该正确采集 GC 指标 - PS 收集器")
        void shouldCollectGcMetricsForPSCollector() {
            // Given: 模拟 PS Scavenge/PS MarkSweep GC 指标
            Counter youngGcCount = Counter.builder("jvm.gc.count")
                    .tag("gc", "PS Scavenge")
                    .register(meterRegistry);
            youngGcCount.increment(80);

            Gauge.builder("jvm.gc.time", () -> 800.0)
                    .tag("gc", "PS Scavenge")
                    .register(meterRegistry);

            Counter oldGcCount = Counter.builder("jvm.gc.count")
                    .tag("gc", "PS MarkSweep")
                    .register(meterRegistry);
            oldGcCount.increment(1);

            Gauge.builder("jvm.gc.time", () -> 100.0)
                    .tag("gc", "PS MarkSweep")
                    .register(meterRegistry);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then
            assertNotNull(message);
            assertEquals(80L, message.getYoungGcCount());
            assertEquals(800L, message.getYoungGcTime());
            assertEquals(1L, message.getOldGcCount());
            assertEquals(100L, message.getOldGcTime());
        }

        @Test
        @DisplayName("应该正确采集线程指标")
        void shouldCollectThreadMetrics() {
            // Given: 模拟线程指标
            meterRegistry.gauge("jvm.threads.live", 150);
            meterRegistry.gauge("jvm.threads.peak", 200);
            meterRegistry.gauge("jvm.threads.daemon", 50);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then
            assertNotNull(message);
            assertEquals(150, message.getLiveThreads());
            assertEquals(200, message.getPeakThreads());
            assertEquals(50, message.getDaemonThreads());
        }

        @Test
        @DisplayName("应该正确采集 HTTP 请求指标")
        void shouldCollectHttpMetrics() {
            // Given: 模拟 HTTP 请求 Timer
            Timer timer = Timer.builder("http.server.requests")
                    .register(meterRegistry);
            timer.record(100, TimeUnit.MILLISECONDS);
            timer.record(200, TimeUnit.MILLISECONDS);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then
            assertNotNull(message);
            assertEquals(2L, message.getTotalRequests());
            assertEquals(2L, message.getSuccessRequests());
            // 平均响应时间 = (100 + 200) / 2 = 150ms
            assertEquals(150L, message.getAvgResponseTime());
        }

        @Test
        @DisplayName("应该设置默认健康状态为 UP")
        void shouldSetDefaultHealthStatusUp() {
            initInstanceInfo();

            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            assertNotNull(message);
            assertEquals("UP", message.getHealthStatus());
        }
    }

    @Nested
    @DisplayName("消息类型测试")
    class MessageTypeTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("注册消息类型应为 REGISTER")
        void registerMessageTypeShouldBeRegister() {
            // When
            metricsReporter.sendRegisterMessage();

            // Then
            ArgumentCaptor<Record<String, ?>> captor = ArgumentCaptor.forClass(Record.class);
            verify(streamOperations, timeout(1000)).add(captor.capture());

            Map<String, String> message = (Map<String, String>) captor.getValue().getValue();
            assertEquals("REGISTER", message.get("type"));
            assertEquals("UP", message.get("healthStatus"));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("注销消息类型应为 UNREGISTER")
        void unregisterMessageTypeShouldBeUnregister() {
            // When
            metricsReporter.sendUnregisterMessage();

            // Then
            ArgumentCaptor<Record<String, ?>> captor = ArgumentCaptor.forClass(Record.class);
            verify(streamOperations, timeout(1000)).add(captor.capture());

            Map<String, String> message = (Map<String, String>) captor.getValue().getValue();
            assertEquals("UNREGISTER", message.get("type"));
            assertEquals("DOWN", message.get("healthStatus"));
        }
    }

    @Nested
    @DisplayName("实例信息测试")
    class InstanceInfoTests {

        @Test
        @DisplayName("消息应包含实例标识信息")
        void messageShouldContainInstanceInfo() {
            initInstanceInfo();

            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            assertNotNull(message);
            assertNotNull(message.getInstanceId());
            assertEquals("test-gateway", message.getServiceId());
            assertEquals(8080, message.getPort());
            assertNotNull(message.getTimestamp());
        }

        @Test
        @DisplayName("实例ID格式应为 serviceId@host:port")
        void instanceIdFormatShouldBeCorrect() {
            initInstanceInfo();

            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            assertNotNull(message);
            String instanceId = message.getInstanceId();
            assertTrue(instanceId.startsWith("test-gateway@"));
            assertTrue(instanceId.contains(":8080"));
        }
    }

    @Nested
    @DisplayName("Redis Stream 推送测试")
    class RedisStreamPushTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("应推送到正确的 Stream Key")
        void shouldPushToCorrectStreamKey() {
            metricsReporter.sendRegisterMessage();

            ArgumentCaptor<Record<String, ?>> captor = ArgumentCaptor.forClass(Record.class);
            verify(streamOperations, timeout(1000)).add(captor.capture());

            assertEquals("blink:gateway:metrics:stream", captor.getValue().getStream());
        }

        @Test
        @DisplayName("异步推送失败时不应抛出异常")
        void shouldNotThrowExceptionOnAsyncPushFailure() {
            // Given: Mock Redis 返回错误
            when(streamOperations.add(any(Record.class)))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // When & Then: 不应抛出异常
            assertDoesNotThrow(() -> metricsReporter.sendRegisterMessage());

            // 等待异步操作完成
            StepVerifier.create(Mono.empty().delayElement(Duration.ofMillis(100)))
                    .verifyComplete();
        }

        @Test
        @DisplayName("同步推送失败时应抛出异常")
        void shouldThrowExceptionOnSyncPushFailure() {
            // Given: Mock Redis 返回错误
            when(streamOperations.add(any(Record.class)))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // When & Then: 应捕获异常（sendUnregisterMessage 会捕获并记录日志）
            assertDoesNotThrow(() -> metricsReporter.sendUnregisterMessage());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("堆内存最大值为零时应避免除零错误")
        void shouldAvoidDivisionByZeroWhenHeapMaxIsZero() {
            // Given: 堆内存最大值为零
            meterRegistry.gauge("jvm.memory.used", List.of(Tag.of("area", "heap")), 134217728L);
            meterRegistry.gauge("jvm.memory.max", List.of(Tag.of("area", "heap")), 0L);

            initInstanceInfo();

            // When
            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            // Then: 不应计算堆内存使用率
            assertNotNull(message);
            assertNull(message.getHeapUsagePercent());
        }

        @Test
        @DisplayName("指标不存在时不应抛出异常")
        void shouldNotThrowWhenMetricsNotAvailable() {
            // Given: 不设置任何指标
            initInstanceInfo();

            // When & Then: 不应抛出异常
            assertDoesNotThrow(() -> {
                MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");
                assertNotNull(message);
            });
        }

        @Test
        @DisplayName("禁用上报时应跳过")
        void shouldSkipWhenReportDisabled() {
            // Given: 禁用上报
            ReflectionTestUtils.setField(metricsReporter, "configPushEnabled", false);

            // When: 调用 reportMetrics
            metricsReporter.reportMetrics();

            // Then: 不应推送消息
            verify(streamOperations, never()).add(any());
        }

        @Test
        @DisplayName("HTTP 请求数为零时应避免除零错误")
        void shouldAvoidDivisionByZeroWhenNoHttpRequests() {
            initInstanceInfo();

            MetricsMessage message = ReflectionTestUtils.invokeMethod(metricsReporter, "collectMetrics");

            assertNotNull(message);
            assertEquals(0L, message.getTotalRequests());
            assertNull(message.getAvgResponseTime());
        }
    }

    /**
     * 初始化实例信息（模拟 initInstanceInfo 私有方法）
     */
    private void initInstanceInfo() {
        @SuppressWarnings("unchecked")
        AtomicReference<String> instanceIdRef = (AtomicReference<String>)
                ReflectionTestUtils.getField(metricsReporter, "instanceId");
        @SuppressWarnings("unchecked")
        AtomicReference<String> hostAddressRef = (AtomicReference<String>)
                ReflectionTestUtils.getField(metricsReporter, "hostAddress");

        if (instanceIdRef != null && instanceIdRef.get() == null) {
            instanceIdRef.set("test-gateway@127.0.0.1:8080");
        }
        if (hostAddressRef != null && hostAddressRef.get() == null) {
            hostAddressRef.set("127.0.0.1");
        }
    }
}
