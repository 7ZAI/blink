package com.blink.gateway.admin.service.impl;

import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.config.MonitorProperties;
import com.blink.gateway.admin.dto.InstanceStatusSnapshot;
import com.blink.gateway.admin.sse.InstanceStatusPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.blink.gateway.admin.sse.SseMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InstanceStatusPushServiceImpl 单元测试
 *
 * 测试状态变化检测与 SSE 推送的核心功能
 *
 * @author binblink
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstanceStatusPushServiceImplTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private SseConnectionPool sseConnectionPool;

    @Mock
    private MonitorProperties monitorProperties;

    @InjectMocks
    private InstanceStatusPushServiceImpl service;

    private static final String SNAPSHOT_KEY_PREFIX = "blink:gateway:instance:snapshot:";
    private static final String METRICS_KEY_PREFIX = "blink:gateway:metrics:";
    private static final String INSTANCE_LIST_KEY = "blink:gateway:instance:list";

    @BeforeEach
    void setUp() {
        // 配置 MonitorProperties
        when(monitorProperties.getCpuChangeThreshold()).thenReturn(10);
        when(monitorProperties.getHeapChangeThreshold()).thenReturn(10);
    }

    @Nested
    @DisplayName("checkAndPush 状态变化检测测试")
    class CheckAndPushTests {

        @Test
        @DisplayName("空载荷不应触发推送")
        void shouldNotPushWhenPayloadsEmpty() {
            // When
            service.checkAndPush(null);
            service.checkAndPush(Collections.emptyList());

            // Then
            verify(sseConnectionPool, never()).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("首次上报应触发推送")
        void shouldPushWhenFirstReport() {
            // Given: 无历史快照
            when(redisClient.hGetStringMap(anyString())).thenReturn(null);

            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 0, "UP", 50.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 应广播推送
            verify(sseConnectionPool).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("状态变化应触发广播推送")
        void shouldBroadcastWhenStatusChanged() {
            // Given: 有历史快照（状态为在线）
            Map<String, Object> previousData = new HashMap<>();
            previousData.put("status", 0);
            previousData.put("healthStatus", "UP");
            previousData.put("cpuUsageInt", 50);
            previousData.put("heapUsageInt", 60);
            previousData.put("timestamp", System.currentTimeMillis() - 60000);

            when(redisClient.hGetStringMap(SNAPSHOT_KEY_PREFIX + "instance-1"))
                    .thenReturn(previousData);

            // 当前状态变为离线
            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 1, "DOWN", 50.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 应广播推送
            verify(sseConnectionPool).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("健康状态变化应触发广播推送")
        void shouldBroadcastWhenHealthStatusChanged() {
            // Given: 有历史快照（健康）
            Map<String, Object> previousData = new HashMap<>();
            previousData.put("status", 0);
            previousData.put("healthStatus", "UP");
            previousData.put("cpuUsageInt", 50);
            previousData.put("heapUsageInt", 60);

            when(redisClient.hGetStringMap(SNAPSHOT_KEY_PREFIX + "instance-1"))
                    .thenReturn(previousData);

            // 当前健康状态变为 DOWN
            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 0, "DOWN", 50.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 应广播推送
            verify(sseConnectionPool).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("CPU 变化超过阈值应触发推送")
        void shouldPushWhenCpuChangeExceedsThreshold() {
            // Given: 历史快照 CPU 50%
            Map<String, Object> previousData = new HashMap<>();
            previousData.put("status", 0);
            previousData.put("healthStatus", "UP");
            previousData.put("cpuUsageInt", 50);
            previousData.put("heapUsageInt", 60);

            when(redisClient.hGetStringMap(SNAPSHOT_KEY_PREFIX + "instance-1"))
                    .thenReturn(previousData);

            // 当前 CPU 65%（变化超过阈值 10%）
            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 0, "UP", 65.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 应推送
            verify(sseConnectionPool).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("CPU 变化未超过阈值不应触发推送")
        void shouldNotPushWhenCpuChangeBelowThreshold() {
            // Given: 历史快照 CPU 50%
            Map<String, Object> previousData = new HashMap<>();
            previousData.put("status", 0);
            previousData.put("healthStatus", "UP");
            previousData.put("cpuUsageInt", 50);
            previousData.put("heapUsageInt", 60);

            when(redisClient.hGetStringMap(SNAPSHOT_KEY_PREFIX + "instance-1"))
                    .thenReturn(previousData);

            // 当前 CPU 55%（变化未超过阈值 10%）
            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 0, "UP", 55.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 不应推送
            verify(sseConnectionPool, never()).broadcast(any(SseMessage.class));
        }

        @Test
        @DisplayName("无变化不应触发推送")
        void shouldNotPushWhenNoChange() {
            // Given: 历史快照与当前相同
            Map<String, Object> previousData = new HashMap<>();
            previousData.put("status", 0);
            previousData.put("healthStatus", "UP");
            previousData.put("cpuUsageInt", 50);
            previousData.put("heapUsageInt", 60);

            when(redisClient.hGetStringMap(SNAPSHOT_KEY_PREFIX + "instance-1"))
                    .thenReturn(previousData);

            // 当前数据与历史相同
            List<InstanceStatusPayload.InstanceSummary> payloads = createSummaries(
                    createSummary("instance-1", 0, "UP", 50.0, 60.0)
            );

            // When
            service.checkAndPush(payloads);

            // Then: 不应推送
            verify(sseConnectionPool, never()).broadcast(any(SseMessage.class));
        }
    }

    @Nested
    @DisplayName("pushStatusChange 状态推送测试")
    class PushStatusChangeTests {

        @Test
        @DisplayName("应正确推送单个实例状态变化")
        void shouldPushSingleInstanceStatusChange() {
            // Given: Redis 中有实例指标
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("healthStatus", "UP");
            metrics.put("cpuUsage", 50.0);
            metrics.put("timestamp", System.currentTimeMillis());

            when(redisClient.hGetStringMap(METRICS_KEY_PREFIX + "instance-1"))
                    .thenReturn(metrics);

            // When
            service.pushStatusChange("instance-1", 1);

            // Then: 应广播推送
            @SuppressWarnings("unchecked")
            ArgumentCaptor<SseMessage<InstanceStatusPayload>> captor = ArgumentCaptor.forClass(SseMessage.class);
            verify(sseConnectionPool).broadcast(captor.capture());

            SseMessage<InstanceStatusPayload> msg = captor.getValue();
            assertEquals(SseMessageType.INSTANCE_STATUS, msg.getType());
            assertTrue(msg.getData().getHasChange());
            assertEquals(List.of("instance-1"), msg.getData().getChangedInstanceIds());
        }

        @Test
        @DisplayName("无指标数据时仍应推送状态变化")
        @SuppressWarnings("unchecked")
        void shouldPushStatusChangeEvenWithoutMetrics() {
            // Given: Redis 中无实例指标
            when(redisClient.hGetStringMap(anyString())).thenReturn(null);

            // When
            service.pushStatusChange("instance-1", 1);

            // Then: 仍应广播推送
            verify(sseConnectionPool).broadcast(any(SseMessage.class));
        }
    }

    @Nested
    @DisplayName("sendFullStatusToUser 完整状态推送测试")
    class SendFullStatusToUserTests {

        @Test
        @DisplayName("应正确发送完整状态给用户")
        void shouldSendFullStatusToUser() {
            // Given: Redis 中有多个实例
            Map<String, Object> instanceList = new HashMap<>();
            instanceList.put("instance-1", String.valueOf(System.currentTimeMillis()));
            instanceList.put("instance-2", String.valueOf(System.currentTimeMillis()));

            when(redisClient.hGetStringMap(INSTANCE_LIST_KEY)).thenReturn(instanceList);

            // 实例 1 在线健康
            Map<String, Object> metrics1 = new HashMap<>();
            metrics1.put("status", 0);
            metrics1.put("healthStatus", "UP");
            metrics1.put("cpuUsage", 50.0);
            metrics1.put("timestamp", System.currentTimeMillis());

            // 实例 2 在线不健康
            Map<String, Object> metrics2 = new HashMap<>();
            metrics2.put("status", 0);
            metrics2.put("healthStatus", "DOWN");
            metrics2.put("cpuUsage", 30.0);
            metrics2.put("timestamp", System.currentTimeMillis());

            when(redisClient.hGetStringMap(METRICS_KEY_PREFIX + "instance-1")).thenReturn(metrics1);
            when(redisClient.hGetStringMap(METRICS_KEY_PREFIX + "instance-2")).thenReturn(metrics2);

            // When
            service.sendFullStatusToUser(1001);

            // Then: 应发送给指定用户
            @SuppressWarnings("unchecked")
            ArgumentCaptor<SseMessage<InstanceStatusPayload>> captor =
                    ArgumentCaptor.forClass(SseMessage.class);
            verify(sseConnectionPool).sendToUser(eq(1001), captor.capture());

            SseMessage<InstanceStatusPayload> msg = captor.getValue();
            assertEquals(SseMessageType.INSTANCE_STATUS, msg.getType());

            InstanceStatusPayload payload = msg.getData();
            assertEquals(2, payload.getInstances().size());
            assertEquals(2, payload.getStats().getTotal());
            assertEquals(2, payload.getStats().getOnline());
            assertEquals(1, payload.getStats().getHealthy());
            assertFalse(payload.getHasChange()); // 首次加载标记为无变化
        }

        @Test
        @DisplayName("无实例时不应发送")
        @SuppressWarnings("unchecked")
        void shouldNotSendWhenNoInstances() {
            // Given: Redis 中无实例
            when(redisClient.hGetStringMap(INSTANCE_LIST_KEY)).thenReturn(null);

            // When
            service.sendFullStatusToUser(1001);

            // Then: 不应发送
            verify(sseConnectionPool, never()).sendToUser(any(Integer.class), any(SseMessage.class));
        }
    }

    // ==================== 辅助方法 ====================

    private InstanceStatusPayload.InstanceSummary createSummary(
            String instanceId, int status, String healthStatus,
            Double cpuUsage, Double heapUsagePercent) {

        InstanceStatusPayload.InstanceSummary summary = new InstanceStatusPayload.InstanceSummary();
        summary.setInstanceId(instanceId);
        summary.setStatus(status);
        summary.setHealthStatus(healthStatus);
        summary.setCpuUsage(cpuUsage);
        summary.setHeapUsagePercent(heapUsagePercent);
        summary.setTimestamp(System.currentTimeMillis());
        return summary;
    }

    private List<InstanceStatusPayload.InstanceSummary> createSummaries(
            InstanceStatusPayload.InstanceSummary... summaries) {
        return Arrays.asList(summaries);
    }
}
