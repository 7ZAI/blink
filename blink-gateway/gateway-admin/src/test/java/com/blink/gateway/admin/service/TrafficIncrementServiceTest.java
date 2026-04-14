package com.blink.gateway.admin.service;

import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.service.impl.TrafficIncrementServiceImpl;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TrafficIncrementService 单元测试
 *
 * 测试流量增量计算的核心功能：
 * 1. 正常增量计算场景
 * 2. 首次上报场景（无上次值）
 * 3. 实例重启场景（当前值小于上次值）
 * 4. 多实例场景
 * 5. 获取最近流量数据
 *
 * @author binblink
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrafficIncrementServiceTest {

    private static final String INSTANCE_ID_1 = "gateway-1@192.168.1.100:8080";
    private static final String INSTANCE_ID_2 = "gateway-2@192.168.1.101:8080";
    private static final String LAST_VALUES_KEY = "blink:gateway:traffic:last:values";

    @Mock
    private RedisClient redisClient;

    private TrafficIncrementServiceImpl trafficIncrementService;

    @BeforeEach
    void setUp() {
        trafficIncrementService = new TrafficIncrementServiceImpl();
        // 通过反射注入 redisClient
        try {
            java.lang.reflect.Field field = TrafficIncrementServiceImpl.class.getDeclaredField("redisClient");
            field.setAccessible(true);
            field.set(trafficIncrementService, redisClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("正常增量计算测试")
    class NormalIncrementTests {

        @Test
        @DisplayName("应该正确计算增量并存储")
        void shouldCalculateIncrementAndStore() {
            // Given: 上次累计值为 100
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(100L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(95L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(5L);

            // 当前累计值为 150
            long currentTotal = 150;
            long currentSuccess = 140;
            long currentFailed = 10;

            // When: 计算增量
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, currentTotal, currentSuccess, currentFailed);

            // Then: 增量应为 50
            assertEquals(50, increment);

            // 验证更新上次值
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total", currentTotal);
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success", currentSuccess);
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed", currentFailed);

            // 验证存储到 Sorted Set
            verify(redisClient).zAdd(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyString(), anyDouble());
            verify(redisClient).expire(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyLong());
        }

        @Test
        @DisplayName("增量为 0 时仍应存储")
        void shouldStoreEvenWhenIncrementIsZero() {
            // Given: 上次值和当前值相同
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(100L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(95L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(5L);

            // When
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, 100, 95, 5);

            // Then: 增量应为 0，但仍存储
            assertEquals(0, increment);
            verify(redisClient).zAdd(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyString(), anyDouble());
        }
    }

    @Nested
    @DisplayName("首次上报测试")
    class FirstReportTests {

        @Test
        @DisplayName("首次上报时增量应为 0")
        void firstReportShouldReturnZeroIncrement() {
            // Given: 上次值不存在
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(null);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(null);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(null);

            // When: 首次上报，当前累计值为 1000
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, 1000, 950, 50);

            // Then: 增量应为 0（首次上报不计算增量）
            assertEquals(0, increment);

            // 验证更新上次值（设置基准）
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total", 1000L);
        }

        @Test
        @DisplayName("上次值为 0 时增量应为 0")
        void zeroLastValueShouldReturnZeroIncrement() {
            // Given: 上次值为 0（可能被重置）
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(0L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(0L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(0L);

            // When
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, 100, 95, 5);

            // Then: 增量应为 0（首次上报场景）
            assertEquals(0, increment);
        }
    }

    @Nested
    @DisplayName("实例重启测试")
    class InstanceRestartTests {

        @Test
        @DisplayName("当前值小于上次值时应重置基准")
        void shouldResetBaselineWhenCurrentValueSmaller() {
            // Given: 上次累计值为 10000（实例重启前）
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(10000L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(9500L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(500L);

            // 当前累计值为 50（实例重启后重新计数）
            long currentTotal = 50;
            long currentSuccess = 45;
            long currentFailed = 5;

            // When: 计算增量
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, currentTotal, currentSuccess, currentFailed);

            // Then: 增量应为当前值（重置基准）
            assertEquals(50, increment);

            // 验证更新上次值为当前值
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total", 50L);
        }

        @Test
        @DisplayName("重启后下次上报应正常计算增量")
        void shouldCalculateNormalIncrementAfterRestart() {
            // Given: 重置后的上次值为 50
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(50L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":success")).thenReturn(45L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":failed")).thenReturn(5L);

            // 当前累计值为 80
            long currentTotal = 80;
            long currentSuccess = 75;
            long currentFailed = 5;

            // When
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, currentTotal, currentSuccess, currentFailed);

            // Then: 正常计算增量
            assertEquals(30, increment);
        }
    }

    @Nested
    @DisplayName("多实例测试")
    class MultipleInstanceTests {

        @Test
        @DisplayName("应该为不同实例独立计算增量")
        void shouldCalculateIncrementForDifferentInstances() {
            // Given: 两个实例的不同上次值
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total")).thenReturn(100L);
            when(redisClient.hGetField(LAST_VALUES_KEY, INSTANCE_ID_2 + ":total")).thenReturn(200L);

            // When: 计算两个实例的增量
            long increment1 = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_1, 150, 140, 10);
            long increment2 = trafficIncrementService.calculateAndStoreIncrement(
                    INSTANCE_ID_2, 280, 270, 10);

            // Then: 各实例独立计算
            assertEquals(50, increment1);
            assertEquals(80, increment2);

            // 验证分别更新
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_1 + ":total", 150L);
            verify(redisClient).hPutField(LAST_VALUES_KEY, INSTANCE_ID_2 + ":total", 280L);
        }
    }

    @Nested
    @DisplayName("获取流量数据测试")
    class GetTrafficDataTests {

        @Test
        @DisplayName("应该获取最近 N 分钟的流量增量")
        void shouldGetRecentTrafficIncrement() {
            // Given: Redis Sorted Set 有数据
            Set<Object> mockData = new HashSet<>();
            mockData.add("10:8:2:" + (System.currentTimeMillis() - 1000));
            mockData.add("20:18:2:" + (System.currentTimeMillis() - 2000));
            mockData.add("30:28:2:" + (System.currentTimeMillis() - 3000));

            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(mockData);

            // When: 获取最近 5 分钟数据
            Map<Long, Long> result = trafficIncrementService.getRecentTrafficIncrement(5);

            // Then: 应返回解析后的数据
            assertFalse(result.isEmpty());
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("无数据时应返回空 Map")
        void shouldReturnEmptyMapWhenNoData() {
            // Given: Redis 无数据
            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(Collections.emptySet());

            // When
            Map<Long, Long> result = trafficIncrementService.getRecentTrafficIncrement(5);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("应该获取最新增量数据点")
        void shouldGetLatestIncrement() {
            // Given: Redis Sorted Set 最新数据
            Set<Object> mockData = new HashSet<>();
            long timestamp = System.currentTimeMillis();
            mockData.add("50:45:5:" + timestamp);

            when(redisClient.zRange(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), eq(-1L), eq(-1L)))
                    .thenReturn(mockData);

            // When
            TrafficIncrementService.TrafficDataPoint point = trafficIncrementService.getLatestIncrement();

            // Then
            assertNotNull(point);
            assertEquals(50, point.increment());
            assertEquals(timestamp, point.timestamp());
        }

        @Test
        @DisplayName("无最新数据时应返回 null")
        void shouldReturnNullWhenNoLatestData() {
            // Given
            when(redisClient.zRange(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), eq(-1L), eq(-1L)))
                    .thenReturn(Collections.emptySet());

            // When
            TrafficIncrementService.TrafficDataPoint point = trafficIncrementService.getLatestIncrement();

            // Then
            assertNull(point);
        }
    }

    @Nested
    @DisplayName("清理过期数据测试")
    class CleanExpiredDataTests {

        @Test
        @DisplayName("应该清理过期数据")
        void shouldCleanExpiredData() {
            // Given
            when(redisClient.zRemoveRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(10L);

            // When: 清理 30 分钟前的数据
            trafficIncrementService.cleanExpiredData(30);

            // Then: 验证调用清理方法
            verify(redisClient).zRemoveRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), eq(0.0), anyDouble());
        }

        @Test
        @DisplayName("无过期数据时清理数量应为 0")
        void shouldReturnZeroWhenNoExpiredData() {
            // Given
            when(redisClient.zRemoveRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(0L);

            // When
            trafficIncrementService.cleanExpiredData(60);

            // Then: 仍应调用清理方法
            verify(redisClient).zRemoveRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), eq(0.0), anyDouble());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("instanceId 为空时应返回 0")
        void shouldReturnZeroWhenInstanceIdEmpty() {
            // When
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    "", 100, 95, 5);

            // Then
            assertEquals(0, increment);
            verify(redisClient, never()).zAdd(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("instanceId 为 null 时应返回 0")
        void shouldReturnZeroWhenInstanceIdNull() {
            // When
            long increment = trafficIncrementService.calculateAndStoreIncrement(
                    null, 100, 95, 5);

            // Then
            assertEquals(0, increment);
        }

        @Test
        @DisplayName("数据点格式异常时应跳过解析")
        void shouldSkipInvalidDataPointFormat() {
            // Given: 格式错误的数据点
            Set<Object> mockData = new HashSet<>();
            mockData.add("invalid-format");
            mockData.add("10:8:2:" + System.currentTimeMillis()); // 正常格式

            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(mockData);

            // When
            Map<Long, Long> result = trafficIncrementService.getRecentTrafficIncrement(5);

            // Then: 应跳过格式错误的数据，只返回正常的
            assertEquals(1, result.size());
        }
    }
}