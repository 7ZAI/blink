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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisClient Hash 操作单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisClientHashTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

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
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // 创建被测试对象
        redisClient = new RedisClient(redisTemplate, streamRedisTemplate);
    }

    @Nested
    @DisplayName("HPut/HSet 操作测试")
    class HPutTests {

        @Test
        @DisplayName("02-01: 设置单个hash字段")
        void testHPutField() {
            // Given
            String key = "hash-key";
            String field = "field1";
            Object value = "value1";

            // When
            redisClient.hPutField(key, field, value);

            // Then
            verify(hashOperations).put(key, field, value);
        }

        @Test
        @DisplayName("02-02: 批量设置hash字段")
        void testHSet() {
            // Given
            String key = "hash-key";
            Map<String, Object> fieldValues = new HashMap<>();
            fieldValues.put("field1", "value1");
            fieldValues.put("field2", "value2");

            // When
            redisClient.hSet(key, fieldValues);

            // Then
            verify(hashOperations).putAll(key, fieldValues);
        }
    }

    @Nested
    @DisplayName("HGet 操作测试")
    class HGetTests {

        @Test
        @DisplayName("02-03: 获取存在的hash字段")
        void testHGetField_Exists() {
            // Given
            String key = "hash-key";
            String field = "field1";
            Object expectedValue = "value1";
            when(hashOperations.get(key, field)).thenReturn(expectedValue);

            // When
            Object result = redisClient.hGetField(key, field);

            // Then
            assertEquals(expectedValue, result);
            verify(hashOperations).get(key, field);
        }

        @Test
        @DisplayName("02-04: 获取不存在的hash字段")
        void testHGetField_NotExists() {
            // Given
            String key = "hash-key";
            String field = "non-existent-field";
            when(hashOperations.get(key, field)).thenReturn(null);

            // When
            Object result = redisClient.hGetField(key, field);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("02-05: 批量获取多个hash字段")
        void testHMultiGetFields() {
            // Given
            String key = "hash-key";
            List<String> fields = Arrays.asList("field1", "field2");
            List<Object> expectedValues = Arrays.asList("value1", "value2");
            when(hashOperations.multiGet(eq(key), any(Collection.class))).thenReturn(expectedValues);

            // When
            List<Object> result = redisClient.hMultiGetFields(key, fields);

            // Then
            assertEquals(expectedValues, result);
            verify(hashOperations).multiGet(eq(key), any(Collection.class));
        }

        @Test
        @DisplayName("02-06: 获取整个hash")
        void testHGet() {
            // Given
            String key = "hash-key";
            Map<Object, Object> expectedMap = new HashMap<>();
            expectedMap.put("field1", "value1");
            expectedMap.put("field2", "value2");
            when(hashOperations.entries(key)).thenReturn(expectedMap);

            // When
            Map<?, Object> result = redisClient.hGet(key);

            // Then
            assertEquals(expectedMap, result);
            verify(hashOperations).entries(key);
        }
    }

    @Nested
    @DisplayName("HGetStringMap 操作测试")
    class HGetStringMapTests {

        @Test
        @DisplayName("02-07: 获取String类型key的hash")
        void testHGetStringMap() {
            // Given
            String key = "hash-key";
            Map<Object, Object> rawMap = new HashMap<>();
            rawMap.put("field1", "value1");
            rawMap.put("field2", "value2");
            when(hashOperations.entries(key)).thenReturn(rawMap);

            // When
            Map<String, Object> result = redisClient.hGetStringMap(key);

            // Then
            assertEquals(2, result.size());
            assertEquals("value1", result.get("field1"));
            assertEquals("value2", result.get("field2"));
        }

        @Test
        @DisplayName("02-08: 空hash返回空Map")
        void testHGetStringMap_Empty() {
            // Given
            String key = "empty-hash-key";
            when(hashOperations.entries(key)).thenReturn(null);

            // When
            Map<String, Object> result = redisClient.hGetStringMap(key);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("02-08b: null值字段被正确处理")
        void testHGetStringMap_NullKey() {
            // Given
            String key = "hash-key";
            Map<Object, Object> rawMap = new HashMap<>();
            rawMap.put("field1", "value1");
            rawMap.put(null, "null-value"); // null key
            rawMap.put("field2", "value2");
            when(hashOperations.entries(key)).thenReturn(rawMap);

            // When
            Map<String, Object> result = redisClient.hGetStringMap(key);

            // Then
            assertEquals(2, result.size()); // null key should be skipped
            assertEquals("value1", result.get("field1"));
            assertEquals("value2", result.get("field2"));
        }
    }

    @Nested
    @DisplayName("HDelete 操作测试")
    class HDeleteTests {

        @Test
        @DisplayName("02-09: 删除hash字段")
        void testHDeleteFields() {
            // Given
            String key = "hash-key";
            String field1 = "field1";
            String field2 = "field2";
            when(hashOperations.delete(eq(key), eq(field1), eq(field2))).thenReturn(2L);

            // When
            Long result = redisClient.hDeleteFields(key, field1, field2);

            // Then
            assertEquals(2L, result);
            verify(hashOperations).delete(key, field1, field2);
        }
    }

    @Nested
    @DisplayName("HExists 操作测试")
    class HExistsTests {

        @Test
        @DisplayName("02-10: hash字段存在判断-存在")
        void testHExists_True() {
            // Given
            String key = "hash-key";
            String field = "field1";
            when(hashOperations.hasKey(key, field)).thenReturn(true);

            // When
            Boolean result = redisClient.hExists(key, field);

            // Then
            assertTrue(result);
            verify(hashOperations).hasKey(key, field);
        }

        @Test
        @DisplayName("02-11: hash字段存在判断-不存在")
        void testHExists_False() {
            // Given
            String key = "hash-key";
            String field = "non-existent-field";
            when(hashOperations.hasKey(key, field)).thenReturn(false);

            // When
            Boolean result = redisClient.hExists(key, field);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("HSize 操作测试")
    class HSizeTests {

        @Test
        @DisplayName("02-12: 获取hash字段数量")
        void testHSize() {
            // Given
            String key = "hash-key";
            when(hashOperations.size(key)).thenReturn(3L);

            // When
            Long result = redisClient.hSize(key);

            // Then
            assertEquals(3L, result);
            verify(hashOperations).size(key);
        }
    }

    @Nested
    @DisplayName("HIncrement 操作测试")
    class HIncrementTests {

        @Test
        @DisplayName("02-13: hash字段递增")
        void testHIncrement() {
            // Given
            String key = "hash-key";
            String field = "counter";
            long delta = 5L;
            when(hashOperations.increment(key, field, delta)).thenReturn(10L);

            // When
            Long result = redisClient.hIncrement(key, field, delta);

            // Then
            assertEquals(10L, result);
            verify(hashOperations).increment(key, field, delta);
        }
    }
}
