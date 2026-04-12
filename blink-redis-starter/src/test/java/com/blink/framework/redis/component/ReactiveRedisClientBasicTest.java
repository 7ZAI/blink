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
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReactiveRedisClient 基础操作单元测试
 * 包含通用操作、String操作、Hash操作
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveRedisClientBasicTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Mock
    private ReactiveRedisTemplate<String, Object> streamTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    @Mock
    private ReactiveHashOperations<String, Object, Object> hashOperations;

    private ReactiveRedisClient reactiveRedisClient;

    @BeforeEach
    void setUp() {
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(reactiveRedisTemplate.opsForHash()).thenReturn(hashOperations);

        reactiveRedisClient = new ReactiveRedisClient(reactiveRedisTemplate, streamTemplate);
    }

    // ==================== 通用操作测试 ====================

    @Nested
    @DisplayName("通用操作测试")
    class CommonOperationTests {

        @Test
        @DisplayName("06-01: key存在判断-存在")
        void testExists_True() {
            // Given
            String key = "test-key";
            when(reactiveRedisTemplate.hasKey(key)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.exists(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(reactiveRedisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("06-02: key存在判断-不存在")
        void testExists_False() {
            // Given
            String key = "non-existent-key";
            when(reactiveRedisTemplate.hasKey(key)).thenReturn(Mono.just(false));

            // When
            Mono<Boolean> result = reactiveRedisClient.exists(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-03: 删除多个key")
        void testDelete_Multiple() {
            // Given
            String[] keys = {"key1", "key2", "key3"};
            when(reactiveRedisTemplate.delete(keys)).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.delete(keys);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(reactiveRedisTemplate).delete(keys);
        }

        @Test
        @DisplayName("06-04: 删除单个key")
        void testDelete_Single() {
            // Given
            String key = "test-key";
            when(reactiveRedisTemplate.delete(key)).thenReturn(Mono.just(1L));

            // When
            Mono<Boolean> result = reactiveRedisClient.delete(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(reactiveRedisTemplate).delete(key);
        }

        @Test
        @DisplayName("06-05: 设置过期时间")
        void testExpire() {
            // Given
            String key = "test-key";
            Duration timeout = Duration.ofSeconds(60);
            when(reactiveRedisTemplate.expire(key, timeout)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.expire(key, timeout);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(reactiveRedisTemplate).expire(key, timeout);
        }

        @Test
        @DisplayName("06-06: 获取剩余过期时间")
        void testTtl() {
            // Given
            String key = "test-key";
            Duration expectedTtl = Duration.ofSeconds(30);
            when(reactiveRedisTemplate.getExpire(key)).thenReturn(Mono.just(expectedTtl));

            // When
            Mono<Duration> result = reactiveRedisClient.ttl(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedTtl)
                    .verifyComplete();
            verify(reactiveRedisTemplate).getExpire(key);
        }

        @Test
        @DisplayName("06-07: 移除过期时间")
        void testPersist() {
            // Given
            String key = "test-key";
            when(reactiveRedisTemplate.persist(key)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.persist(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(reactiveRedisTemplate).persist(key);
        }

        @Test
        @DisplayName("06-08: 获取匹配模式的所有key")
        void testKeys() {
            // Given
            String pattern = "test-*";
            Flux<String> expectedKeys = Flux.just("test-key1", "test-key2");
            when(reactiveRedisTemplate.keys(pattern)).thenReturn(expectedKeys);

            // When
            Flux<String> result = reactiveRedisClient.keys(pattern);

            // Then
            StepVerifier.create(result)
                    .expectNext("test-key1", "test-key2")
                    .verifyComplete();
            verify(reactiveRedisTemplate).keys(pattern);
        }

        @Test
        @DisplayName("06-09: 扫描匹配key")
        void testScan() {
            // Given
            String pattern = "test-*";
            Flux<String> expectedKeys = Flux.just("test-key1", "test-key2");
            when(reactiveRedisTemplate.scan(any(ScanOptions.class))).thenReturn(expectedKeys);

            // When
            Flux<String> result = reactiveRedisClient.scan(pattern);

            // Then
            StepVerifier.create(result)
                    .expectNext("test-key1", "test-key2")
                    .verifyComplete();
            verify(reactiveRedisTemplate).scan(any(ScanOptions.class));
        }

        @Test
        @DisplayName("06-10: 获取key类型")
        void testType() {
            // Given
            String key = "test-key";
            when(reactiveRedisTemplate.type(key)).thenReturn(Mono.just(DataType.STRING));

            // When
            Mono<String> result = reactiveRedisClient.type(key);

            // Then
            StepVerifier.create(result)
                    .expectNext("STRING")
                    .verifyComplete();
            verify(reactiveRedisTemplate).type(key);
        }
    }

    // ==================== String 操作测试 ====================

    @Nested
    @DisplayName("String 操作测试")
    class StringOperationTests {

        @Test
        @DisplayName("06-11: 设置值")
        void testSet() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.set(key, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.set(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(valueOperations).set(key, value);
        }

        @Test
        @DisplayName("06-12: 设置带过期时间的值")
        void testSetEx() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            Duration timeout = Duration.ofSeconds(60);
            when(valueOperations.set(key, value, timeout)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.setEx(key, value, timeout);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(valueOperations).set(key, value, timeout);
        }

        @Test
        @DisplayName("06-13: key不存在时设置成功")
        void testSetIfAbsent_Success() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            when(valueOperations.setIfAbsent(key, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.setIfAbsent(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(valueOperations).setIfAbsent(key, value);
        }

        @Test
        @DisplayName("06-14: key已存在时设置失败")
        void testSetIfAbsent_Fail() {
            // Given
            String key = "existing-key";
            Object value = "test-value";
            when(valueOperations.setIfAbsent(key, value)).thenReturn(Mono.just(false));

            // When
            Mono<Boolean> result = reactiveRedisClient.setIfAbsent(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-15: key不存在时设置带过期时间")
        void testSetIfAbsentWithExpire() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            Duration timeout = Duration.ofSeconds(60);
            when(valueOperations.setIfAbsent(key, value, timeout)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.setIfAbsentWithExpire(key, value, timeout);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(valueOperations).setIfAbsent(key, value, timeout);
        }

        @Test
        @DisplayName("06-16: key存在时更新成功")
        void testSetIfPresent_Success() {
            // Given
            String key = "test-key";
            Object value = "new-value";
            when(valueOperations.setIfPresent(key, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.setIfPresent(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(valueOperations).setIfPresent(key, value);
        }

        @Test
        @DisplayName("06-17: key不存在时设置失败")
        void testSetIfPresent_Fail() {
            // Given
            String key = "non-existent-key";
            Object value = "test-value";
            when(valueOperations.setIfPresent(key, value)).thenReturn(Mono.just(false));

            // When
            Mono<Boolean> result = reactiveRedisClient.setIfPresent(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-18: 获取值")
        void testGet() {
            // Given
            String key = "test-key";
            Object expectedValue = "test-value";
            when(valueOperations.get(key)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.get(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("06-19: 获取并设置新值")
        void testGetAndSet() {
            // Given
            String key = "test-key";
            Object newValue = "new-value";
            Object oldValue = "old-value";
            when(valueOperations.getAndSet(key, newValue)).thenReturn(Mono.just(oldValue));

            // When
            Mono<Object> result = reactiveRedisClient.getAndSet(key, newValue);

            // Then
            StepVerifier.create(result)
                    .expectNext(oldValue)
                    .verifyComplete();
            verify(valueOperations).getAndSet(key, newValue);
        }

        @Test
        @DisplayName("06-20: 递增操作")
        void testIncrement() {
            // Given
            String key = "counter";
            when(valueOperations.increment(key)).thenReturn(Mono.just(1L));

            // When
            Mono<Long> result = reactiveRedisClient.increment(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
            verify(valueOperations).increment(key);
        }

        @Test
        @DisplayName("06-21: 递增指定步长")
        void testIncrementBy() {
            // Given
            String key = "counter";
            long delta = 5L;
            when(valueOperations.increment(key, delta)).thenReturn(Mono.just(6L));

            // When
            Mono<Long> result = reactiveRedisClient.incrementBy(key, delta);

            // Then
            StepVerifier.create(result)
                    .expectNext(6L)
                    .verifyComplete();
            verify(valueOperations).increment(key, delta);
        }

        @Test
        @DisplayName("06-22: 递减操作")
        void testDecrement() {
            // Given
            String key = "counter";
            when(valueOperations.decrement(key)).thenReturn(Mono.just(-1L));

            // When
            Mono<Long> result = reactiveRedisClient.decrement(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(-1L)
                    .verifyComplete();
            verify(valueOperations).decrement(key);
        }

        @Test
        @DisplayName("06-23: 递减指定步长")
        void testDecrementBy() {
            // Given
            String key = "counter";
            long delta = 3L;
            when(valueOperations.decrement(key, delta)).thenReturn(Mono.just(-3L));

            // When
            Mono<Long> result = reactiveRedisClient.decrementBy(key, delta);

            // Then
            StepVerifier.create(result)
                    .expectNext(-3L)
                    .verifyComplete();
            verify(valueOperations).decrement(key, delta);
        }

        @Test
        @DisplayName("06-24: 获取字符串长度")
        void testStrLen() {
            // Given
            String key = "test-key";
            when(valueOperations.size(key)).thenReturn(Mono.just(10L));

            // When
            Mono<Long> result = reactiveRedisClient.strLen(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(10L)
                    .verifyComplete();
            verify(valueOperations).size(key);
        }

        @Test
        @DisplayName("06-25: key存在时获取默认值")
        void testGetOrDefault_Exists() {
            // Given
            String key = "test-key";
            Object expectedValue = "actual-value";
            Object defaultValue = "default-value";
            when(valueOperations.get(key)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.getOrDefault(key, defaultValue);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-26: key不存在时获取默认值")
        void testGetOrDefault_NotExists() {
            // Given
            String key = "non-existent-key";
            Object defaultValue = "default-value";
            when(valueOperations.get(key)).thenReturn(Mono.empty());

            // When
            Mono<Object> result = reactiveRedisClient.getOrDefault(key, defaultValue);

            // Then
            StepVerifier.create(result)
                    .expectNext(defaultValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-27: 重试机制-成功")
        void testSetWithRetry_Success() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            int maxRetries = 3;
            when(valueOperations.set(key, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.setWithRetry(key, value, maxRetries);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-28: 重试机制-全部失败")
        void testSetWithRetry_AllFail() {
            // Given
            String key = "test-key";
            Object value = "test-value";
            int maxRetries = 2;
            when(valueOperations.set(key, value))
                    .thenReturn(Mono.error(new RuntimeException("Connection error")));

            // When
            Mono<Boolean> result = reactiveRedisClient.setWithRetry(key, value, maxRetries);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    // ==================== Hash 操作测试 ====================

    @Nested
    @DisplayName("Hash 操作测试")
    class HashOperationTests {

        @Test
        @DisplayName("06-29: 设置hash字段")
        void testHPut() {
            // Given
            String key = "hash-key";
            String field = "field1";
            Object value = "value1";
            when(hashOperations.put(key, field, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.hPut(key, field, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(hashOperations).put(key, field, value);
        }

        @Test
        @DisplayName("06-30: 批量设置hash字段")
        void testHPutAll() {
            // Given
            String key = "hash-key";
            Map<String, Object> fieldValues = new HashMap<>();
            fieldValues.put("field1", "value1");
            fieldValues.put("field2", "value2");
            when(hashOperations.putAll(key, fieldValues)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.hPutAll(key, fieldValues);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(hashOperations).putAll(key, fieldValues);
        }

        @Test
        @DisplayName("06-31: 获取hash字段值")
        void testHGet() {
            // Given
            String key = "hash-key";
            String field = "field1";
            Object expectedValue = "value1";
            when(hashOperations.get(key, field)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.hGet(key, field);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(hashOperations).get(key, field);
        }

        @Test
        @DisplayName("06-32: 批量获取hash字段")
        void testHMultiGet() {
            // Given
            String key = "hash-key";
            List<String> fields = Arrays.asList("field1", "field2");
            List<Object> expectedValues = Arrays.asList("value1", "value2");
            when(hashOperations.multiGet(eq(key), anyList())).thenReturn(Mono.just(expectedValues));

            // When
            Mono<List<Object>> result = reactiveRedisClient.hMultiGet(key, fields);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValues)
                    .verifyComplete();
            verify(hashOperations).multiGet(eq(key), anyList());
        }

        @Test
        @DisplayName("06-33: 获取所有字段和值")
        void testHEntries() {
            // Given
            String key = "hash-key";
            Map<Object, Object> entries = new HashMap<>();
            entries.put("field1", "value1");
            entries.put("field2", "value2");
            when(hashOperations.entries(key)).thenReturn(Flux.fromIterable(entries.entrySet()));

            // When
            Flux<Map.Entry<Object, Object>> result = reactiveRedisClient.hEntries(key);

            // Then
            StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
            verify(hashOperations).entries(key);
        }

        @Test
        @DisplayName("06-34: 获取所有字段名")
        void testHKeys() {
            // Given
            String key = "hash-key";
            when(hashOperations.keys(key)).thenReturn(Flux.just("field1", "field2"));

            // When
            Flux<Object> result = reactiveRedisClient.hKeys(key);

            // Then
            StepVerifier.create(result)
                    .expectNext("field1", "field2")
                    .verifyComplete();
            verify(hashOperations).keys(key);
        }

        @Test
        @DisplayName("06-35: 获取所有字段值")
        void testHValues() {
            // Given
            String key = "hash-key";
            when(hashOperations.values(key)).thenReturn(Flux.just("value1", "value2"));

            // When
            Flux<Object> result = reactiveRedisClient.hValues(key);

            // Then
            StepVerifier.create(result)
                    .expectNext("value1", "value2")
                    .verifyComplete();
            verify(hashOperations).values(key);
        }

        @Test
        @DisplayName("06-36: 删除hash字段")
        void testHDelete() {
            // Given
            String key = "hash-key";
            String[] fields = {"field1", "field2"};
            when(hashOperations.remove(eq(key), any(Object[].class))).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.hDelete(key, fields);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(hashOperations).remove(eq(key), any(Object[].class));
        }

        @Test
        @DisplayName("06-37: hash字段存在判断-存在")
        void testHExists_True() {
            // Given
            String key = "hash-key";
            String field = "field1";
            when(hashOperations.hasKey(key, field)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.hExists(key, field);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(hashOperations).hasKey(key, field);
        }

        @Test
        @DisplayName("06-38: hash字段存在判断-不存在")
        void testHExists_False() {
            // Given
            String key = "hash-key";
            String field = "non-existent-field";
            when(hashOperations.hasKey(key, field)).thenReturn(Mono.just(false));

            // When
            Mono<Boolean> result = reactiveRedisClient.hExists(key, field);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("06-39: 获取hash字段数量")
        void testHSize() {
            // Given
            String key = "hash-key";
            when(hashOperations.size(key)).thenReturn(Mono.just(3L));

            // When
            Mono<Long> result = reactiveRedisClient.hSize(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(3L)
                    .verifyComplete();
            verify(hashOperations).size(key);
        }

        @Test
        @DisplayName("06-40: hash字段递增")
        void testHIncrement() {
            // Given
            String key = "hash-key";
            String field = "counter";
            long delta = 5L;
            when(hashOperations.increment(key, field, delta)).thenReturn(Mono.just(10L));

            // When
            Mono<Long> result = reactiveRedisClient.hIncrement(key, field, delta);

            // Then
            StepVerifier.create(result)
                    .expectNext(10L)
                    .verifyComplete();
            verify(hashOperations).increment(key, field, delta);
        }
    }
}
