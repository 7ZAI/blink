package com.blink.gateway.admin.service;

import com.blink.gateway.admin.sse.InstanceStatusPayload;
import com.blink.gateway.admin.sse.SseConnectionPool;
import com.blink.gateway.admin.sse.SseMessage;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.framework.redis.component.RedisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MetricsStreamConsumer 单元测试
 *
 * 测试 Redis Stream 消费者的核心功能：
 * 1. 消费 METRICS 类型消息并存储到 Redis Hash
 * 2. 消费 REGISTER 类型消息并注册实例
 * 3. 消费 UNREGISTER 类型消息并注销实例
 * 4. 触发状态变化检测与 SSE 推送
 *
 * @author binblink
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricsStreamConsumerTest {

    private static final String INSTANCE_ID = "gateway-reactive@192.168.1.100:8080";
    private static final String METRICS_KEY = "blink:gateway:metrics:" + INSTANCE_ID;
    private static final String INSTANCE_LIST_KEY = "blink:gateway:instance:list";

    @Mock
    private RedisClient redisClient;

    @Mock
    private SseConnectionPool sseConnectionPool;

    @Mock
    private InstanceStatusPushService instanceStatusPushService;

    @Mock
    private GatewayInstanceMapper gatewayInstanceMapper;

    @Mock
    private DashboardPushService dashboardPushService;

    @Mock
    private TrafficIncrementService trafficIncrementService;

    private MetricsStreamConsumer metricsStreamConsumer;

    @BeforeEach
    void setUp() {
        metricsStreamConsumer = new MetricsStreamConsumer(
                redisClient,
                sseConnectionPool,
                instanceStatusPushService,
                gatewayInstanceMapper,
                dashboardPushService,
                trafficIncrementService
        );

        // 默认 mock：hGetStringMap 返回空 Map，避免 updateSummary 和 triggerStatusCheck 的 NPE
        when(redisClient.hGetStringMap(anyString())).thenReturn(Collections.emptyMap());
        // 默认 mock：数据库中不存在实例
        when(gatewayInstanceMapper.selectById(anyString())).thenReturn(null);
    }

    @Nested
    @DisplayName("METRICS 消息处理测试")
    class MetricsMessageTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("应该正确处理 METRICS 消息并存储指标")
        void shouldProcessMetricsMessageAndStoreMetrics() {
            // Given: METRICS 类型消息，数据库中已存在实例
            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");

            GatewayInstanceDO existingInstance = createOnlineInstance();
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(existingInstance);

            // When: 处理消息
            metricsStreamConsumer.processMessage(message);

            // Then: 验证指标存储到 Redis Hash
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(redisClient).hSet(eq(METRICS_KEY), captor.capture());
            verify(redisClient).expire(eq(METRICS_KEY), anyLong());

            Map<String, Object> storedData = captor.getValue();
            assertEquals(INSTANCE_ID, storedData.get("instanceId"));
            assertEquals("gateway-reactive", storedData.get("serviceId"));
        }

        @Test
        @DisplayName("数据库不存在实例时应自动注册")
        void shouldAutoRegisterWhenInstanceNotInDatabase() {
            // Given: 数据库中不存在实例
            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(null);

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证自动插入数据库
            verify(gatewayInstanceMapper).insert(any(GatewayInstanceDO.class));
        }

        @Test
        @DisplayName("手动下线的实例应忽略指标上报")
        void shouldIgnoreMetricsForShutdownInstance() {
            // Given: 实例被手动下线
            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");

            GatewayInstanceDO shutdownInstance = createShutdownInstance();
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(shutdownInstance);

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 不应存储指标
            verify(redisClient, never()).hSet(eq(METRICS_KEY), any(Map.class));
        }

        @Test
        @DisplayName("应该更新实例列表")
        void shouldUpdateInstanceList() {
            // Given
            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证实例列表更新（使用 hPutField）
            verify(redisClient).hPutField(
                    eq(INSTANCE_LIST_KEY),
                    eq(INSTANCE_ID),
                    anyString()
            );
        }

        @Test
        @DisplayName("应该触发状态变化检测")
        void shouldTriggerStatusChangeDetection() {
            // Given: Mock 实例列表有数据
            Map<String, Object> instanceList = new HashMap<>();
            instanceList.put(INSTANCE_ID, String.valueOf(System.currentTimeMillis()));
            when(redisClient.hGetStringMap(INSTANCE_LIST_KEY)).thenReturn(instanceList);
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证状态变化检测被调用
            verify(instanceStatusPushService).checkAndPush(any());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("应该更新汇总统计")
        void shouldUpdateSummaryStatistics() {
            // Given: Mock 实例列表和指标数据
            Map<String, Object> instanceList = new HashMap<>();
            instanceList.put(INSTANCE_ID, String.valueOf(System.currentTimeMillis()));
            when(redisClient.hGetStringMap(INSTANCE_LIST_KEY)).thenReturn(instanceList);

            Map<String, Object> metricsData = new HashMap<>();
            metricsData.put("healthStatus", "UP");
            metricsData.put("cpuUsage", 15.5);
            when(redisClient.hGetStringMap(METRICS_KEY)).thenReturn(metricsData);
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证汇总统计更新
            verify(redisClient).hSet(eq("blink:gateway:metrics:summary"), any(Map.class));
        }
    }

    @Nested
    @DisplayName("REGISTER 消息处理测试")
    class RegisterMessageTests {

        @Test
        @DisplayName("应该正确处理 REGISTER 消息")
        void shouldProcessRegisterMessage() {
            // Given: REGISTER 类型消息，数据库不存在实例
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("serviceId", "gateway-reactive");
            message.put("host", "192.168.1.100");
            message.put("port", "8080");
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));
            message.put("type", "REGISTER");
            message.put("healthStatus", "UP");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(null);

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证实例注册（使用 hPutField）
            verify(redisClient).hPutField(
                    eq(INSTANCE_LIST_KEY),
                    eq(INSTANCE_ID),
                    anyString()
            );
        }

        @Test
        @DisplayName("REGISTER 消息应广播新实例上线通知")
        void registerMessageShouldBroadcastNotification() {
            // Given: 数据库中已存在实例，避免自动注册时广播
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("serviceId", "gateway-reactive");
            message.put("host", "192.168.1.100");
            message.put("port", "8080");
            message.put("type", "REGISTER");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证广播上线通知
            ArgumentCaptor<SseMessage<?>> captor = ArgumentCaptor.forClass(SseMessage.class);
            verify(sseConnectionPool).broadcast(captor.capture());

            SseMessage<?> sseMessage = captor.getValue();
            assertEquals("notification", sseMessage.getType());
        }
    }

    @Nested
    @DisplayName("UNREGISTER 消息处理测试")
    class UnregisterMessageTests {

        @Test
        @DisplayName("应该正确处理 UNREGISTER 消息")
        void shouldProcessUnregisterMessage() {
            // Given: UNREGISTER 类型消息
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("serviceId", "gateway-reactive");
            message.put("type", "UNREGISTER");
            message.put("healthStatus", "DOWN");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证实例从列表移除（使用 hDeleteFields）
            verify(redisClient).hDeleteFields(
                    eq(INSTANCE_LIST_KEY),
                    eq(INSTANCE_ID)
            );
        }

        @Test
        @DisplayName("UNREGISTER 消息应广播实例下线通知")
        void unregisterMessageShouldBroadcastNotification() {
            // Given
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("serviceId", "gateway-reactive");
            message.put("type", "UNREGISTER");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证广播下线通知
            ArgumentCaptor<SseMessage<?>> captor = ArgumentCaptor.forClass(SseMessage.class);
            verify(sseConnectionPool).broadcast(captor.capture());

            SseMessage<?> sseMessage = captor.getValue();
            assertEquals("notification", sseMessage.getType());
        }

        @Test
        @DisplayName("UNREGISTER 消息应删除实例指标缓存")
        void unregisterMessageShouldDeleteMetricsCache() {
            // Given
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("type", "UNREGISTER");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证删除指标缓存
            verify(redisClient).delete(METRICS_KEY);
        }

        @Test
        @DisplayName("UNREGISTER 消息应更新数据库状态为离线")
        void unregisterMessageShouldUpdateDatabaseStatus() {
            // Given
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("type", "UNREGISTER");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 验证数据库状态更新
            ArgumentCaptor<GatewayInstanceDO> captor = ArgumentCaptor.forClass(GatewayInstanceDO.class);
            verify(gatewayInstanceMapper).updateById(captor.capture());
            assertEquals((byte) 1, captor.getValue().getStatus()); // INSTANCE_STATUS_OFFLINE = 1
        }

        @Test
        @DisplayName("手动下线的实例 UNREGISTER 不应更新状态")
        void shouldNotUpdateStatusForShutdownInstance() {
            // Given: 实例已被手动下线
            Map<String, String> message = new HashMap<>();
            message.put("instanceId", INSTANCE_ID);
            message.put("type", "UNREGISTER");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createShutdownInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 不应更新数据库状态
            verify(gatewayInstanceMapper, never()).updateById(any(GatewayInstanceDO.class));
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("消息缺少 instanceId 时应跳过处理")
        void shouldSkipWhenInstanceIdMissing() {
            // Given: 缺少 instanceId 的消息
            Map<String, String> message = new HashMap<>();
            message.put("type", "METRICS");

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 不应进行任何 Redis 操作
            verify(redisClient, never()).hSet(anyString(), any(Map.class));
            verify(redisClient, never()).hPutField(anyString(), anyString(), any());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("消息缺少 type 时应默认为 METRICS")
        void shouldDefaultToMetricsWhenTypeMissing() {
            // Given: 缺少 type 的消息
            Map<String, String> message = createMetricsMessage();
            message.remove("type");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            // When
            metricsStreamConsumer.processMessage(message);

            // Then: 应按 METRICS 类型处理
            verify(redisClient).hSet(eq(METRICS_KEY), any(Map.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Redis 操作失败时不应抛出异常")
        void shouldNotThrowWhenRedisOperationFails() {
            // Given
            Map<String, String> message = createMetricsMessage();
            message.put("type", "METRICS");
            when(gatewayInstanceMapper.selectById(INSTANCE_ID)).thenReturn(createOnlineInstance());

            doThrow(new RuntimeException("Redis connection failed"))
                    .when(redisClient).hSet(anyString(), any(Map.class));

            // When & Then: 不应抛出异常
            assertDoesNotThrow(() -> metricsStreamConsumer.processMessage(message));
        }
    }

    /**
     * 创建测试用的 METRICS 消息
     */
    private Map<String, String> createMetricsMessage() {
        Map<String, String> message = new HashMap<>();
        message.put("instanceId", INSTANCE_ID);
        message.put("serviceId", "gateway-reactive");
        message.put("host", "192.168.1.100");
        message.put("port", "8080");
        message.put("timestamp", String.valueOf(System.currentTimeMillis()));
        message.put("heapUsed", "134217728");
        message.put("heapMax", "536870912");
        message.put("heapUsagePercent", "25.0");
        message.put("cpuUsage", "15.5");
        message.put("healthStatus", "UP");
        return message;
    }

    /**
     * 创建在线实例
     */
    private GatewayInstanceDO createOnlineInstance() {
        GatewayInstanceDO instance = new GatewayInstanceDO();
        instance.setInstanceId(INSTANCE_ID);
        instance.setServiceId("gateway-reactive");
        instance.setHost("192.168.1.100");
        instance.setPort(8080);
        instance.setStatus((byte) 0); // INSTANCE_STATUS_ONLINE
        return instance;
    }

    /**
     * 创建手动下线的实例
     */
    private GatewayInstanceDO createShutdownInstance() {
        GatewayInstanceDO instance = createOnlineInstance();
        instance.setStatus((byte) 2); // INSTANCE_STATUS_SHUTDOWN
        return instance;
    }
}
