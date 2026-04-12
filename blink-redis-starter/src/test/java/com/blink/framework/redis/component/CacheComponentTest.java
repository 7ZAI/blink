package com.blink.framework.redis.component;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheComponent 多级缓存单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheComponentTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private Cache<String, Object> localCache;

    private CacheComponent cacheComponentEnabled;
    private CacheComponent cacheComponentDisabled;

    @BeforeEach
    void setUp() throws Exception {
        cacheComponentEnabled = new CacheComponent(true);
        cacheComponentDisabled = new CacheComponent(false);

        // 使用反射设置私有字段 redisClient
        Field redisClientField = CacheComponent.class.getDeclaredField("redisClient");
        redisClientField.setAccessible(true);
        redisClientField.set(cacheComponentEnabled, redisClient);
        redisClientField.set(cacheComponentDisabled, redisClient);
    }

    // ==================== getFromAllCache 测试 ====================

    @Nested
    @DisplayName("getFromAllCache 测试")
    class GetFromAllCacheTests {

        @Test
        @DisplayName("08-01: 本地缓存命中")
        void testGetFromAllCache_LocalCacheHit() {
            // Given
            String key = "test-key";
            Object expectedValue = "cached-value";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(expectedValue);

                // When
                Object result = cacheComponentEnabled.getFromAllCache(key);

                // Then
                assertEquals(expectedValue, result);
                verify(localCache).getIfPresent(key);
                verify(redisClient, never()).get(anyString());
            }
        }

        @Test
        @DisplayName("08-02: Redis缓存命中")
        void testGetFromAllCache_RedisCacheHit() {
            // Given
            String key = "test-key";
            Object expectedValue = "redis-value";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(null);
                when(redisClient.get(key)).thenReturn(expectedValue);

                // When
                Object result = cacheComponentEnabled.getFromAllCache(key);

                // Then
                assertEquals(expectedValue, result);
                verify(redisClient).get(key);
                // 验证回填本地缓存
                verify(localCache).put(key, expectedValue);
            }
        }

        @Test
        @DisplayName("08-03: 两级缓存都未命中")
        void testGetFromAllCache_BothMiss() {
            // Given
            String key = "test-key";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(null);
                when(redisClient.get(key)).thenReturn(null);

                // When
                Object result = cacheComponentEnabled.getFromAllCache(key);

                // Then
                assertNull(result);
                verify(redisClient).get(key);
            }
        }

        @Test
        @DisplayName("08-04: 本地缓存禁用")
        void testGetFromAllCache_LocalCacheDisabled() {
            // Given
            String key = "test-key";
            Object expectedValue = "redis-value";
            when(redisClient.get(key)).thenReturn(expectedValue);

            // When
            Object result = cacheComponentDisabled.getFromAllCache(key);

            // Then
            assertEquals(expectedValue, result);
            verify(redisClient).get(key);
        }
    }

    // ==================== getFromCacheOrDB 测试 ====================

    @Nested
    @DisplayName("getFromCacheOrDB 测试")
    class GetFromCacheOrDBTests {

        @Test
        @DisplayName("08-05: 缓存命中")
        void testGetFromCacheOrDB_CacheHit() {
            // Given
            String key = "test-key";
            Object expectedValue = "cached-value";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(expectedValue);

                // When
                Object result = cacheComponentEnabled.getFromCacheOrDB(key, () -> "db-value");

                // Then
                assertEquals(expectedValue, result);
                // DB supplier 不应该被调用
                verify(redisClient, never()).set(anyString(), any());
            }
        }

        @Test
        @DisplayName("08-06: 缓存未命中，DB查询成功")
        void testGetFromCacheOrDB_CacheMiss_DBSuccess() {
            // Given
            String key = "test-key";
            Object dbValue = "db-value";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(null);
                when(redisClient.get(key)).thenReturn(null);

                // When
                Object result = cacheComponentEnabled.getFromCacheOrDB(key, () -> dbValue);

                // Then
                assertEquals(dbValue, result);
                verify(redisClient).delete(key);
                verify(redisClient).set(key, dbValue);
                verify(localCache).put(key, dbValue);
            }
        }

        @Test
        @DisplayName("08-07: 缓存未命中，DB返回null")
        void testGetFromCacheOrDB_CacheMiss_DBReturnsNull() {
            // Given
            String key = "test-key";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenReturn(null);
                when(redisClient.get(key)).thenReturn(null);

                // When
                Object result = cacheComponentEnabled.getFromCacheOrDB(key, () -> null);

                // Then
                assertNull(result);
                // DB返回null时不写入缓存
                verify(redisClient, never()).set(anyString(), any());
            }
        }

        @Test
        @DisplayName("08-08: 缓存操作异常")
        void testGetFromCacheOrDB_Exception() {
            // Given
            String key = "test-key";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(localCache.getIfPresent(key)).thenThrow(new RuntimeException("Cache error"));

                // When & Then
                assertThrows(BlinkException.class, () ->
                        cacheComponentEnabled.getFromCacheOrDB(key, () -> "db-value"));
            }
        }
    }

    // ==================== resetCache 测试 ====================

    @Nested
    @DisplayName("resetCache 测试")
    class ResetCacheTests {

        @Test
        @DisplayName("08-09: 重置缓存-本地缓存启用")
        void testResetCache_LocalCacheEnabled() {
            // Given
            String key = "test-key";
            Object value = "new-value";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);

                // When
                cacheComponentEnabled.resetCache(key, value);

                // Then
                verify(localCache).put(key, value);
                verify(redisClient).delete(key);
                verify(redisClient).set(key, value);
            }
        }

        @Test
        @DisplayName("08-10: 重置缓存-本地缓存禁用")
        void testResetCache_LocalCacheDisabled() {
            // Given
            String key = "test-key";
            Object value = "new-value";

            // When
            cacheComponentDisabled.resetCache(key, value);

            // Then
            verify(redisClient).delete(key);
            verify(redisClient).set(key, value);
        }
    }

    // ==================== loadCacheFromDB 测试 ====================

    @Nested
    @DisplayName("loadCacheFromDB 测试")
    class LoadCacheFromDBTests {

        @Test
        @DisplayName("08-11: 从DB批量加载缓存")
        void testLoadCacheFromDB() {
            // Given
            String keyPrefix = "test-prefix";
            Map<String, Object> cacheMap = new HashMap<>();
            cacheMap.put("test-prefix-key1", "value1");
            cacheMap.put("test-prefix-key2", "value2");

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(redisClient.deleteByPrefixScan(keyPrefix)).thenReturn(2L);

                // When
                cacheComponentEnabled.loadCacheFromDB(keyPrefix, () -> cacheMap);

                // Then
                verify(redisClient).deleteByPrefixScan(keyPrefix);
                verify(redisClient).batchSet(cacheMap);
                verify(localCache).putAll(cacheMap);
            }
        }

        @Test
        @DisplayName("08-12: 从DB加载空Map")
        void testLoadCacheFromDB_EmptyMap() {
            // Given
            String keyPrefix = "test-prefix";
            Map<String, Object> emptyMap = new HashMap<>();

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);
                when(redisClient.deleteByPrefixScan(keyPrefix)).thenReturn(0L);

                // When
                cacheComponentEnabled.loadCacheFromDB(keyPrefix, () -> emptyMap);

                // Then
                verify(redisClient).deleteByPrefixScan(keyPrefix);
                verify(redisClient).batchSet(emptyMap);
                verify(localCache).putAll(emptyMap);
            }
        }
    }

    // ==================== clearLocalCache 测试 ====================

    @Nested
    @DisplayName("clearLocalCache 测试")
    class ClearLocalCacheTests {

        @Test
        @DisplayName("08-13: 清除单个本地缓存key")
        void testClearLocalCache_SingleKey() {
            // Given
            String key = "test-key";

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);

                // When
                cacheComponentEnabled.clearLocalCache(key);

                // Then
                verify(localCache).invalidate(key);
            }
        }

        @Test
        @DisplayName("08-14: 清除多个本地缓存key")
        void testClearLocalCache_MultipleKeys() {
            // Given
            List<String> keys = Arrays.asList("key1", "key2", "key3");

            try (MockedStatic<ApplicationContextUtil> mockedStatic = mockStatic(ApplicationContextUtil.class)) {
                mockedStatic.when(() -> ApplicationContextUtil.getBean(Cache.class)).thenReturn(localCache);

                // When
                cacheComponentEnabled.clearLocalCache(keys);

                // Then
                verify(localCache).invalidate("key1");
                verify(localCache).invalidate("key2");
                verify(localCache).invalidate("key3");
            }
        }

        @Test
        @DisplayName("08-15: 清除空key列表")
        void testClearLocalCache_EmptyList() {
            // Given
            List<String> keys = Collections.emptyList();

            // When
            cacheComponentEnabled.clearLocalCache(keys);

            // Then - 无操作
            verify(localCache, never()).invalidate(anyString());
        }

        @Test
        @DisplayName("08-16: 本地缓存禁用时清除")
        void testClearLocalCache_LocalCacheDisabled() {
            // Given
            String key = "test-key";

            // When
            cacheComponentDisabled.clearLocalCache(key);

            // Then - 无操作
            verify(localCache, never()).invalidate(anyString());
        }
    }
}
