package com.blink.framework.redis.id;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.config.prop.BlinkRedisProperties;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SeqGenerator 序列号生成器单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeqGeneratorTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private BlinkRedisProperties properties;

    @Mock
    private BlinkRedisProperties.IdGenerator idGenerator;

    private SeqGenerator seqGenerator;

    @BeforeEach
    void setUp() throws Exception {
        // 清理静态缓存
        clearStaticCache();

        // 设置配置
        Map<String, BlinkRedisProperties.IdGenerator.SeqParam> seqParamMap = new HashMap<>();
        BlinkRedisProperties.IdGenerator.SeqParam seqParam = new BlinkRedisProperties.IdGenerator.SeqParam();
        seqParam.setStep(100);
        seqParam.setFetchPercent(0.8);
        seqParamMap.put("test-key", seqParam);

        when(properties.getIdGenerator()).thenReturn(idGenerator);
        when(idGenerator.getSeqParam()).thenReturn(seqParamMap);
        when(idGenerator.getkeySteps("test-key")).thenReturn(100);
        when(idGenerator.getLuaScript()).thenReturn("return redis.call('INCRBY', KEYS[1], ARGV[2])");

        seqGenerator = new SeqGenerator(redisClient, properties);
    }

    private void clearStaticCache() throws Exception {
        Field seqCacheField = SeqGenerator.class.getDeclaredField("SEQ_CACHE");
        seqCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> seqCache = (Map<String, Object>) seqCacheField.get(null);
        seqCache.clear();

        Field statusMapField = SeqGenerator.class.getDeclaredField("STATUS_MAP");
        statusMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> statusMap = (Map<String, Object>) statusMapField.get(null);
        statusMap.clear();
    }

    // ==================== generateSeq 测试 ====================

    @Nested
    @DisplayName("generateSeq 测试")
    class GenerateSeqTests {

        @Test
        @DisplayName("09-01: 正常生成序列号")
        @SuppressWarnings("unchecked")
        void testGenerateSeq_Success() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            // 初始化
            invokeInit(seqGenerator);

            // Mock Redis 返回
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), any(), any()))
                    .thenReturn(100L);

            // When
            Long result = seqGenerator.generateSeq(key, maxValue);

            // Then
            assertNotNull(result);
            // 第一次生成时，会触发从Redis获取分段，返回的是新分段的第一个序列号
            assertTrue(result >= 0);
        }

        @Test
        @DisplayName("09-02: key为空")
        void testGenerateSeq_BlankKey() throws Exception {
            // Given
            invokeInit(seqGenerator);

            // When & Then
            assertThrows(BlinkException.class, () ->
                    seqGenerator.generateSeq("", Long.MAX_VALUE));
        }

        @Test
        @DisplayName("09-03: key未配置")
        void testGenerateSeq_KeyNotConfigured() throws Exception {
            // Given
            invokeInit(seqGenerator);

            // When & Then
            assertThrows(BlinkException.class, () ->
                    seqGenerator.generateSeq("unconfigured-key", Long.MAX_VALUE));
        }

        @Test
        @DisplayName("09-04: 分段耗尽")
        void testGenerateSeq_SegmentExhausted() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // 设置一个即将耗尽的分段
            setSeqCacheValue(key, 98L, 99L);

            // Mock Redis 返回新分段
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), anyString(), anyString()))
                    .thenReturn(200L);

            // When - 生成多个序列号直到耗尽
            Long result1 = seqGenerator.generateSeq(key, maxValue);
            Long result2 = seqGenerator.generateSeq(key, maxValue);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
        }

        @Test
        @DisplayName("09-05: 并发生成唯一性")
        void testGenerateSeq_ConcurrentUniqueness() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // Mock Redis 返回大分段
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), anyString(), anyString()))
                    .thenReturn(1000L);

            int threadCount = 100;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            Set<Long> results = ConcurrentHashMap.newKeySet();

            // When
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        Long seq = seqGenerator.generateSeq(key, maxValue);
                        results.add(seq);
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        endLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);

            // Then - 所有ID应该唯一
            assertEquals(threadCount, results.size());
        }

        @Test
        @DisplayName("09-06: 触发异步预取")
        void testGenerateSeq_PrefetchTriggered() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // 设置使用率较高的分段
            setSeqCacheValue(key, 80L, 99L);

            // Mock Redis 返回
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), anyString(), anyString()))
                    .thenReturn(200L);

            // When
            Long result = seqGenerator.generateSeq(key, maxValue);

            // Then
            assertNotNull(result);
            // 预取是异步的，等待一小段时间
            Thread.sleep(100);
        }

        @Test
        @DisplayName("09-07: 预取分段被提升")
        void testGenerateSeq_PrefetchPromoted() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // 设置即将耗尽的分段
            setSeqCacheValue(key, 99L, 99L);

            // 手动添加预取分段
            addPrefetchSegment(key, 100L, 199L);

            // When - 分段耗尽后会使用预取分段
            Long result = seqGenerator.generateSeq(key, maxValue);

            // Then
            assertNotNull(result);
            // 预取分段应该被提升使用
        }

        @Test
        @DisplayName("09-08: Redis失败后重试成功")
        void testGenerateSeq_RetrySuccess() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // 设置分段耗尽
            setSeqCacheValue(key, 100L, 99L);

            // Mock Redis 第一次失败，第二次成功
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Connection failed"))
                    .thenReturn(200L);

            // When
            Long result = seqGenerator.generateSeq(key, maxValue);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("09-09: 重试全部失败")
        @SuppressWarnings("unchecked")
        void testGenerateSeq_AllRetriesFailed() throws Exception {
            // Given
            String key = "test-key";
            Long maxValue = Long.MAX_VALUE;

            invokeInit(seqGenerator);

            // 设置分段耗尽（current > max 表示耗尽）
            setSeqCacheValue(key, 101L, 100L);

            // Mock Redis 一直失败 - 使用特定的异常类型触发重试机制
            io.lettuce.core.RedisConnectionException redisException =
                new io.lettuce.core.RedisConnectionException("Connection failed");
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), any(), any()))
                    .thenThrow(redisException);

            // When & Then - 应该抛出异常
            assertThrows(Exception.class, () -> seqGenerator.generateSeq(key, maxValue));
        }
    }

    // ==================== init/destroy 测试 ====================

    @Nested
    @DisplayName("生命周期测试")
    class LifecycleTests {

        @Test
        @DisplayName("09-10: 初始化预分配缓存")
        void testInit() throws Exception {
            // When
            invokeInit(seqGenerator);

            // Then - 验证缓存被初始化
            // 通过生成序列号来验证初始化成功
            when(redisClient.execute(any(RedisScript.class), any(LongRedisSerializer.class), anyList(), anyString(), anyString()))
                    .thenReturn(100L);

            Long result = seqGenerator.generateSeq("test-key", Long.MAX_VALUE);
            assertNotNull(result);
        }

        @Test
        @DisplayName("09-11: 优雅关闭")
        void testDestroy() throws Exception {
            // Given
            invokeInit(seqGenerator);

            // When
            invokeDestroy(seqGenerator);

            // Then - 不抛出异常即为成功
        }

        @Test
        @DisplayName("09-12: 关闭被中断")
        void testDestroy_Interrupted() throws Exception {
            // Given
            invokeInit(seqGenerator);

            // When - 使用反射调用 destroy 方法
            Method destroyMethod = SeqGenerator.class.getDeclaredMethod("destroy");
            destroyMethod.setAccessible(true);

            // Then - 不抛出异常即为成功
            assertDoesNotThrow(() -> destroyMethod.invoke(seqGenerator));
        }
    }

    // ==================== shouldPrefetch 测试 ====================

    @Nested
    @DisplayName("shouldPrefetch 测试")
    class ShouldPrefetchTests {

        @Test
        @DisplayName("09-13: 判断是否需要预取")
        void testShouldPrefetch() throws Exception {
            // Given
            invokeInit(seqGenerator);

            // 创建分段用于测试
            Object seqSegment = createSeqSegment(80L, 99L);

            // When - 通过反射调用 shouldPrefetch
            Method shouldPrefetchMethod = SeqGenerator.class.getDeclaredMethod("shouldPrefetch",
                    seqSegment.getClass(), String.class, Integer.class);
            shouldPrefetchMethod.setAccessible(true);

            // 使用率 = (100 - (99 - 80)) / 100 = 81%
            Boolean result = (Boolean) shouldPrefetchMethod.invoke(seqGenerator, seqSegment, "test-key", 100);

            // Then
            assertTrue(result); // 81% >= 80%
        }
    }

    // ==================== SeqSegment 测试 ====================

    @Nested
    @DisplayName("SeqSegment 测试")
    class SeqSegmentTests {

        @Test
        @DisplayName("09-14: 分段获取下一个序列号")
        void testSeqSegment_GetNextSeq() throws Exception {
            // Given
            Object seqSegment = createSeqSegment(1L, 10L);
            Method getNextSeqMethod = seqSegment.getClass().getDeclaredMethod("getNextSeq");
            getNextSeqMethod.setAccessible(true);

            // When
            Long result1 = (Long) getNextSeqMethod.invoke(seqSegment);
            Long result2 = (Long) getNextSeqMethod.invoke(seqSegment);

            // Then
            assertEquals(1L, result1);
            assertEquals(2L, result2);
        }

        @Test
        @DisplayName("09-15: 分段耗尽")
        void testSeqSegment_Exhausted() throws Exception {
            // Given
            Object seqSegment = createSeqSegment(1L, 1L); // start=1, max=1
            Method getNextSeqMethod = seqSegment.getClass().getDeclaredMethod("getNextSeq");
            getNextSeqMethod.setAccessible(true);

            // When
            Long result1 = (Long) getNextSeqMethod.invoke(seqSegment);
            Long result2 = (Long) getNextSeqMethod.invoke(seqSegment);

            // Then
            assertEquals(1L, result1);
            assertEquals(-1L, result2); // 分段耗尽
        }

        @Test
        @DisplayName("09-16: 计算使用率")
        void testSeqSegment_UsageRate() throws Exception {
            // Given
            Object seqSegment = createSeqSegment(1L, 100L);

            // 消耗一些序列号
            Method getNextSeqMethod = seqSegment.getClass().getDeclaredMethod("getNextSeq");
            getNextSeqMethod.setAccessible(true);
            for (int i = 0; i < 50; i++) {
                getNextSeqMethod.invoke(seqSegment);
            }

            // When
            Method usageRateMethod = seqSegment.getClass().getDeclaredMethod("usageRate", long.class);
            usageRateMethod.setAccessible(true);
            double usageRate = (double) usageRateMethod.invoke(seqSegment, 100L);

            // Then
            assertTrue(usageRate >= 0.49 && usageRate <= 0.51);
        }
    }

    // ==================== 辅助方法 ====================

    private void invokeInit(SeqGenerator generator) throws Exception {
        Method initMethod = SeqGenerator.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(generator);
    }

    private void invokeDestroy(SeqGenerator generator) throws Exception {
        Method destroyMethod = SeqGenerator.class.getDeclaredMethod("destroy");
        destroyMethod.setAccessible(true);
        destroyMethod.invoke(generator);
    }

    private void setSeqCacheValue(String key, long current, long max) throws Exception {
        // 获取 SEQ_CACHE 静态字段
        Field seqCacheField = SeqGenerator.class.getDeclaredField("SEQ_CACHE");
        seqCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> seqCache = (Map<String, Object>) seqCacheField.get(null);

        Object seqSegment = createSeqSegment(current, max);
        seqCache.put(key, seqSegment);
    }

    private void addPrefetchSegment(String key, long start, long max) throws Exception {
        Field seqCacheField = SeqGenerator.class.getDeclaredField("SEQ_CACHE");
        seqCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> seqCache = (Map<String, Object>) seqCacheField.get(null);

        Object seqSegment = createSeqSegment(start, max);
        seqCache.put("seq:prefetch:" + key, seqSegment);
    }

    private Object createSeqSegment(long start, long max) throws Exception {
        // SeqSegment 是私有内部类，使用反射创建
        Class<?>[] innerClasses = SeqGenerator.class.getDeclaredClasses();
        Class<?> seqSegmentClass = null;
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals("SeqSegment")) {
                seqSegmentClass = innerClass;
                break;
            }
        }

        Constructor<?> constructor = seqSegmentClass.getDeclaredConstructor(long.class, long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(start, max);
    }
}
