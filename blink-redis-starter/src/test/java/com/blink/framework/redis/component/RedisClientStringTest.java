package com.blink.framework.redis.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisClient String 操作单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisClientStringTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisSerializer<?> keySerializer;

    @Mock
    private RedisSerializer<?> valueSerializer;

    private RedisClient redisClient;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // 设置 mock 返回值
        when(redisTemplate.getKeySerializer()).thenReturn((RedisSerializer) keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn((RedisSerializer) valueSerializer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 创建被测试对象
        redisClient = new RedisClient(redisTemplate, streamRedisTemplate);
    }

    @Nested
    @DisplayName("Set/Get 操作测试")
    class SetGetTests {

        @Test
        @DisplayName("01-01: 设置并获取字符串值")
        void testSetAndGet() {
            // Given
            String key = "test-key";
            Object value = "test-value";

            // When - 设置值
            redisClient.set(key, value);

            // Then
            verify(valueOperations).set(key, value);

            // When - 获取值
            when(valueOperations.get(key)).thenReturn(value);
            Object result = redisClient.get(key);

            // Then
            assertEquals(value, result);
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("01-02: 获取不存在的key返回null")
        void testSetAndGetNull() {
            // Given
            String key = "non-existent-key";
            when(valueOperations.get(key)).thenReturn(null);

            // When
            Object result = redisClient.get(key);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("01-03: 设置带过期时间的值（Duration）")
        void testSetExWithDuration() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            Duration timeout = Duration.ofSeconds(60);

            // When
            redisClient.setEx(key, value, timeout);

            // Then
            verify(valueOperations).set(key, value, timeout);
        }

        @Test
        @DisplayName("01-04: 设置带过期时间的值（秒）")
        void testSetExWithSeconds() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            long seconds = 60L;

            // When
            redisClient.setEx(key, value, seconds);

            // Then
            verify(valueOperations).set(key, value, seconds, TimeUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("SetIfAbsent/Present 操作测试")
    class SetIfTests {

        @Test
        @DisplayName("01-05: key不存在时设置成功")
        void testSetIfAbsent_Success() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.setIfAbsent(key, value)).thenReturn(true);

            // When
            Boolean result = redisClient.setIfAbsent(key, value);

            // Then
            assertTrue(result);
            verify(valueOperations).setIfAbsent(key, value);
        }

        @Test
        @DisplayName("01-06: key已存在时设置失败")
        void testSetIfAbsent_Fail() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.setIfAbsent(key, value)).thenReturn(false);

            // When
            Boolean result = redisClient.setIfAbsent(key, value);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("01-07: key不存在时设置带过期时间")
        void testSetIfAbsentWithExpire() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            Duration timeout = Duration.ofSeconds(60);
            when(valueOperations.setIfAbsent(key, value, timeout)).thenReturn(true);

            // When
            Boolean result = redisClient.setIfAbsentWithExpire(key, value, timeout);

            // Then
            assertTrue(result);
            verify(valueOperations).setIfAbsent(key, value, timeout);
        }

        @Test
        @DisplayName("01-08: key存在时更新成功")
        void testSetIfPresent_Success() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.setIfPresent(key, value)).thenReturn(true);

            // When
            Boolean result = redisClient.setIfPresent(key, value);

            // Then
            assertTrue(result);
            verify(valueOperations).setIfPresent(key, value);
        }

        @Test
        @DisplayName("01-09: key不存在时设置失败")
        void testSetIfPresent_Fail() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.setIfPresent(key, value)).thenReturn(false);

            // When
            Boolean result = redisClient.setIfPresent(key, value);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("GetAndSet 操作测试")
    class GetAndSetTests {

        @Test
        @DisplayName("01-10: 获取旧值并设置新值")
        void testGetAndSet() {
            // Given
            String key = "test-key";
            Object oldValue = "old-value";
            Object newValue = "new-value";
            when(valueOperations.getAndSet(key, newValue)).thenReturn(oldValue);

            // When
            Object result = redisClient.getAndSet(key, newValue);

            // Then
            assertEquals(oldValue, result);
            verify(valueOperations).getAndSet(key, newValue);
        }
    }

    @Nested
    @DisplayName("Increment/Decrement 操作测试")
    class IncrementTests {

        @Test
        @DisplayName("01-11: 整数递增操作")
        void testIncrement() {
            // Given
            String key = "counter-key";
            when(valueOperations.increment(key)).thenReturn(1L);

            // When
            Long result = redisClient.increment(key);

            // Then
            assertEquals(1L, result);
            verify(valueOperations).increment(key);
        }

        @Test
        @DisplayName("01-12: 整数递增指定步长")
        void testIncrementBy() {
            // Given
            String key = "counter-key";
            long delta = 5L;
            when(valueOperations.increment(key, delta)).thenReturn(5L);

            // When
            Long result = redisClient.incrementBy(key, delta);

            // Then
            assertEquals(5L, result);
            verify(valueOperations).increment(key, delta);
        }

        @Test
        @DisplayName("01-13: 浮点数递增")
        void testIncrementByDouble() {
            // Given
            String key = "counter-key";
            double delta = 2.5;
            when(valueOperations.increment(key, delta)).thenReturn(2.5);

            // When
            Double result = redisClient.incrementBy(key, delta);

            // Then
            assertEquals(2.5, result);
            verify(valueOperations).increment(key, delta);
        }

        @Test
        @DisplayName("01-14: 整数递减操作")
        void testDecrement() {
            // Given
            String key = "counter-key";
            when(valueOperations.decrement(key)).thenReturn(-1L);

            // When
            Long result = redisClient.decrement(key);

            // Then
            assertEquals(-1L, result);
            verify(valueOperations).decrement(key);
        }

        @Test
        @DisplayName("01-15: 整数递减指定步长")
        void testDecrementBy() {
            // Given
            String key = "counter-key";
            long delta = 3L;
            when(valueOperations.decrement(key, delta)).thenReturn(-3L);

            // When
            Long result = redisClient.decrementBy(key, delta);

            // Then
            assertEquals(-3L, result);
            verify(valueOperations).decrement(key, delta);
        }
    }

    @Nested
    @DisplayName("String 辅助操作测试")
    class StringHelperTests {

        @Test
        @DisplayName("01-16: 获取字符串长度")
        void testStrLen() {
            // Given
            String key = "test-key";
            when(valueOperations.size(key)).thenReturn(10L);

            // When
            Long result = redisClient.strLen(key);

            // Then
            assertEquals(10L, result);
            verify(valueOperations).size(key);
        }

        @Test
        @DisplayName("01-17: 追加字符串")
        void testAppend() {
            // Given
            String key = "test-key";
            String value = "-appended";
            when(valueOperations.append(key, value)).thenReturn(20);

            // When
            Integer result = redisClient.append(key, value);

            // Then
            assertEquals(20, result);
            verify(valueOperations).append(key, value);
        }
    }

    @Nested
    @DisplayName("GetOrDefault 操作测试")
    class GetOrDefaultTests {

        @Test
        @DisplayName("01-18: key存在时返回实际值")
        void testGetOrDefault_Exists() {
            // Given
            String key = "test-key";
            Object actualValue = "actual-value";
            Object defaultValue = "default-value";
            when(valueOperations.get(key)).thenReturn(actualValue);

            // When
            Object result = redisClient.getOrDefault(key, defaultValue);

            // Then
            assertEquals(actualValue, result);
        }

        @Test
        @DisplayName("01-19: key不存在时返回默认值")
        void testGetOrDefault_NotExists() {
            // Given
            String key = "test-key";
            Object defaultValue = "default-value";
            when(valueOperations.get(key)).thenReturn(null);

            // When
            Object result = redisClient.getOrDefault(key, defaultValue);

            // Then
            assertEquals(defaultValue, result);
        }
    }

    @Nested
    @DisplayName("SetWithRetry 操作测试")
    class SetWithRetryTests {

        @Test
        @DisplayName("01-20: 重试机制-首次成功")
        void testSetWithRetry_Success() {
            // Given
            String key = "test-key";
            Object value = "test-value";

            // When
            Boolean result = redisClient.setWithRetry(key, value, 3);

            // Then
            assertTrue(result);
            verify(valueOperations, times(1)).set(key, value);
        }

        @Test
        @DisplayName("01-21: 重试机制-重试后成功")
        void testSetWithRetry_RetrySuccess() {
            // Given
            String key = "test-key";
            Object value = "test-value";

            // 第一次失败，第二次成功
            doThrow(new RuntimeException("Connection failed"))
                    .doNothing()
                    .when(valueOperations).set(key, value);

            // When
            Boolean result = redisClient.setWithRetry(key, value, 3);

            // Then
            assertTrue(result);
            verify(valueOperations, times(2)).set(key, value);
        }

        @Test
        @DisplayName("01-22: 重试机制-全部失败")
        void testSetWithRetry_AllFail() {
            // Given
            String key = "test-key";
            Object value = "test-value";

            // 所有调用都失败
            doThrow(new RuntimeException("Connection failed"))
                    .when(valueOperations).set(key, value);

            // When
            Boolean result = redisClient.setWithRetry(key, value, 3);

            // Then
            assertFalse(result);
            verify(valueOperations, times(3)).set(key, value);
        }
    }

    @Nested
    @DisplayName("Exists 操作测试")
    class ExistsTests {

        @Test
        @DisplayName("01-23: key存在判断-存在")
        void testExists_True() {
            // Given
            String key = "test-key";
            when(redisTemplate.hasKey(key)).thenReturn(true);

            // When
            Boolean result = redisClient.exists(key);

            // Then
            assertTrue(result);
            verify(redisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("01-24: key存在判断-不存在")
        void testExists_False() {
            // Given
            String key = "test-key";
            when(redisTemplate.hasKey(key)).thenReturn(false);

            // When
            Boolean result = redisClient.exists(key);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Delete 操作测试")
    class DeleteTests {

        @Test
        @DisplayName("01-25: 删除存在的key")
        void testDelete_Success() {
            // Given
            String key = "test-key";
            when(redisTemplate.delete(key)).thenReturn(true);

            // When
            Boolean result = redisClient.delete(key);

            // Then
            assertTrue(result);
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("01-26: 删除不存在的key")
        void testDelete_NotExists() {
            // Given
            String key = "non-existent-key";
            when(redisTemplate.delete(key)).thenReturn(false);

            // When
            Boolean result = redisClient.delete(key);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("01-27: 批量删除多个key")
        void testDeleteMultiple() {
            // Given
            String key1 = "test-key-1";
            String key2 = "test-key-2";
            when(redisTemplate.delete(anyList())).thenReturn(2L);

            // When
            Long result = redisClient.delete(key1, key2);

            // Then
            assertEquals(2L, result);
            verify(redisTemplate).delete(anyList());
        }
    }

    @Nested
    @DisplayName("Expire/TTL/Persist 操作测试")
    class ExpireTests {

        @Test
        @DisplayName("01-28: 设置过期时间")
        void testExpire() {
            // Given
            String key = "test-key";
            long seconds = 60L;
            when(redisTemplate.expire(key, seconds, TimeUnit.SECONDS)).thenReturn(true);

            // When
            Boolean result = redisClient.expire(key, seconds);

            // Then
            assertTrue(result);
            verify(redisTemplate).expire(key, seconds, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("01-29: 获取剩余过期时间")
        void testTtl() {
            // Given
            String key = "test-key";
            when(redisTemplate.getExpire(key)).thenReturn(30L);

            // When
            Long result = redisClient.ttl(key);

            // Then
            assertEquals(30L, result);
            verify(redisTemplate).getExpire(key);
        }

        @Test
        @DisplayName("01-30: 移除过期时间")
        void testPersist() {
            // Given
            String key = "test-key";
            when(redisTemplate.persist(key)).thenReturn(true);

            // When
            Boolean result = redisClient.persist(key);

            // Then
            assertTrue(result);
            verify(redisTemplate).persist(key);
        }
    }

    @Nested
    @DisplayName("Type 操作测试")
    class TypeTests {

        @Test
        @DisplayName("01-31: 获取key类型")
        void testType() {
            // Given
            String key = "test-key";
            // DataType.STRING.code() 返回 "string" (小写)
            when(redisTemplate.type(key)).thenReturn(org.springframework.data.redis.connection.DataType.STRING);

            // When
            String result = redisClient.type(key);

            // Then
            assertEquals("string", result);
            verify(redisTemplate).type(key);
        }
    }
}
