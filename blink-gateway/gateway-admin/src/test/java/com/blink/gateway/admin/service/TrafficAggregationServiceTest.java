package com.blink.gateway.admin.service;

import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.entity.GatewayTrafficHistoryDO;
import com.blink.gateway.admin.mapper.GatewayTrafficHistoryMapper;
import com.blink.gateway.admin.service.impl.TrafficAggregationServiceImpl;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TrafficAggregationService 单元测试
 *
 * 测试流量聚合服务的核心功能：
 * 1. 分钟级聚合逻辑
 * 2. 小时级聚合逻辑
 * 3. 数据清理逻辑
 *
 * @author binblink
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrafficAggregationServiceTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private GatewayTrafficHistoryMapper trafficHistoryMapper;

    private TrafficAggregationServiceImpl aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new TrafficAggregationServiceImpl();
        // 通过反射注入依赖
        try {
            java.lang.reflect.Field redisField = TrafficAggregationServiceImpl.class.getDeclaredField("redisClient");
            redisField.setAccessible(true);
            redisField.set(aggregationService, redisClient);

            java.lang.reflect.Field mapperField = TrafficAggregationServiceImpl.class.getDeclaredField("trafficHistoryMapper");
            mapperField.setAccessible(true);
            mapperField.set(aggregationService, trafficHistoryMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("分钟级聚合测试")
    class MinuteAggregationTests {

        @Test
        @DisplayName("应该正确聚合秒级数据")
        void shouldAggregateMinuteData() {
            // Given: Redis 中有秒级数据
            Set<Object> dataPoints = new HashSet<>();
            long minuteStart = System.currentTimeMillis() - 60000;
            // 数据格式：increment:success:failed:timestamp
            dataPoints.add("10:8:2:" + minuteStart);
            dataPoints.add("20:18:2:" + (minuteStart + 10000));
            dataPoints.add("30:28:2:" + (minuteStart + 20000));

            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(dataPoints);
            when(trafficHistoryMapper.insert(any(GatewayTrafficHistoryDO.class))).thenReturn(1);

            // When: 执行分钟聚合
            aggregationService.aggregateToMinute();

            // Then: 验证聚合数据存储到 MySQL
            ArgumentCaptor<GatewayTrafficHistoryDO> captor = ArgumentCaptor.forClass(GatewayTrafficHistoryDO.class);
            verify(trafficHistoryMapper).insert(captor.capture());

            GatewayTrafficHistoryDO history = captor.getValue();
            assertEquals("MINUTE", history.getGranularity());
            assertEquals(60L, history.getRequestCount()); // 10 + 20 + 30
            assertEquals(54L, history.getSuccessCount()); // 8 + 18 + 28
            assertEquals(6L, history.getFailedCount());   // 2 + 2 + 2
            assertEquals(30, history.getPeakQps());       // 最大增量 30

            // 验证清理已聚合的 Redis 数据
            verify(redisClient).zRemoveRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), eq(0.0), anyDouble());
        }

        @Test
        @DisplayName("无秒级数据时跳过聚合")
        void shouldSkipWhenNoData() {
            // Given: Redis 无数据
            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(Collections.emptySet());

            // When: 执行分钟聚合
            aggregationService.aggregateToMinute();

            // Then: 不应插入数据库
            verify(trafficHistoryMapper, never()).insert(any(GatewayTrafficHistoryDO.class));
        }

        @Test
        @DisplayName("应该解析异常数据点时跳过")
        void shouldSkipInvalidDataPoint() {
            // Given: 包含异常格式的数据点
            Set<Object> dataPoints = new HashSet<>();
            dataPoints.add("invalid-format");
            dataPoints.add("10:8:2:" + System.currentTimeMillis()); // 正常格式

            when(redisClient.zRangeByScore(eq(RedisKeyConstant.TRAFFIC_REALTIME_KEY), anyDouble(), anyDouble()))
                    .thenReturn(dataPoints);
            when(trafficHistoryMapper.insert(any(GatewayTrafficHistoryDO.class))).thenReturn(1);

            // When: 执行分钟聚合
            aggregationService.aggregateToMinute();

            // Then: 应跳过异常数据，只聚合正常的
            verify(trafficHistoryMapper).insert(any(GatewayTrafficHistoryDO.class));
        }
    }

    @Nested
    @DisplayName("小时级聚合测试")
    class HourAggregationTests {

        @Test
        @DisplayName("应该正确聚合分钟级数据到小时级")
        void shouldAggregateHourData() {
            // Given: 数据库中有分钟级数据
            List<GatewayTrafficHistoryDO> minuteData = List.of(
                    createMinuteHistory(100L, 95L, 5L, 10),
                    createMinuteHistory(200L, 190L, 10L, 20),
                    createMinuteHistory(150L, 140L, 10L, 15)
            );

            when(trafficHistoryMapper.selectByTimeRangeAndGranularity(any(), any(), eq("MINUTE")))
                    .thenReturn(minuteData);
            when(trafficHistoryMapper.insert(any(GatewayTrafficHistoryDO.class))).thenReturn(1);

            // When: 执行小时聚合
            aggregationService.aggregateToHour();

            // Then: 验证聚合数据
            ArgumentCaptor<GatewayTrafficHistoryDO> captor = ArgumentCaptor.forClass(GatewayTrafficHistoryDO.class);
            verify(trafficHistoryMapper).insert(captor.capture());

            GatewayTrafficHistoryDO history = captor.getValue();
            assertEquals("HOUR", history.getGranularity());
            assertEquals(450L, history.getRequestCount()); // 100 + 200 + 150
            assertEquals(425L, history.getSuccessCount()); // 95 + 190 + 140
            assertEquals(25L, history.getFailedCount());   // 5 + 10 + 10
            assertEquals(20, history.getPeakQps());        // 最大峰值 20
        }

        @Test
        @DisplayName("无分钟级数据时跳过小时聚合")
        void shouldSkipHourWhenNoMinuteData() {
            // Given: 无分钟级数据
            when(trafficHistoryMapper.selectByTimeRangeAndGranularity(any(), any(), eq("MINUTE")))
                    .thenReturn(Collections.emptyList());

            // When: 执行小时聚合
            aggregationService.aggregateToHour();

            // Then: 不应插入小时级数据
            verify(trafficHistoryMapper, never()).insert(any(GatewayTrafficHistoryDO.class));
        }
    }

    @Nested
    @DisplayName("清理历史数据测试")
    class CleanHistoryTests {

        @Test
        @DisplayName("应该清理分钟级和小时级过期数据")
        void shouldCleanExpiredData() {
            // Given
            when(trafficHistoryMapper.deleteBeforeTime(any(LocalDateTime.class), eq("MINUTE"))).thenReturn(100);
            when(trafficHistoryMapper.deleteBeforeTime(any(LocalDateTime.class), eq("HOUR"))).thenReturn(50);

            // When: 执行清理
            aggregationService.cleanExpiredHistory();

            // Then: 验证清理两种粒度的数据
            verify(trafficHistoryMapper).deleteBeforeTime(any(LocalDateTime.class), eq("MINUTE"));
            verify(trafficHistoryMapper).deleteBeforeTime(any(LocalDateTime.class), eq("HOUR"));
        }

        @Test
        @DisplayName("清理失败时不应抛出异常")
        void shouldNotThrowWhenCleanFails() {
            // Given: 数据库操作失败
            when(trafficHistoryMapper.deleteBeforeTime(any(LocalDateTime.class), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then: 不应抛出异常
            assertDoesNotThrow(() -> aggregationService.cleanExpiredHistory());
        }
    }

    /**
     * 创建分钟级历史数据测试对象
     */
    private GatewayTrafficHistoryDO createMinuteHistory(long requestCount, long successCount,
                                                         long failedCount, int peakQps) {
        GatewayTrafficHistoryDO history = new GatewayTrafficHistoryDO();
        history.setTimeBucket(LocalDateTime.now());
        history.setGranularity("MINUTE");
        history.setRequestCount(requestCount);
        history.setSuccessCount(successCount);
        history.setFailedCount(failedCount);
        history.setPeakQps(peakQps);
        return history;
    }
}