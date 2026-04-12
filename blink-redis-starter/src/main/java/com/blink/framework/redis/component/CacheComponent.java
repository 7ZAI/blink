package com.blink.framework.redis.component;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Multi-level cache component that supports both local (Caffeine) and distributed (Redis) caching.
 * <p>
 * This component is designed for synchronous web applications only.
 * For reactive applications, use {@link ReactiveRedisClient} directly.
 * </p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Two-level cache: local Caffeine cache + distributed Redis cache</li>
 *   <li>Cache-aside pattern with automatic database fallback</li>
 *   <li>Bulk cache loading from database</li>
 * </ul>
 *
 * <p>Note: All cache write operations are synchronous. If async refresh is needed,
 * the caller should handle it using @Async or custom thread pools.</p>
 *
 * @author binblink
 * @see RedisClient
 * @see ReactiveRedisClient
 */
@Slf4j
public class CacheComponent {

    @Resource
    private RedisClient redisClient;

    private final Boolean enableLocalCache;

    /**
     * Constructs a CacheComponent with the specified local cache configuration.
     *
     * @param enableLocalCache whether to enable local (Caffeine) caching
     */
    public CacheComponent(Boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }

    /**
     * Retrieves an object from all cache levels (local cache first, then Redis).
     *
     * <p>Cache lookup order:</p>
     * <ol>
     *   <li>Local Caffeine cache (if enabled)</li>
     *   <li>Distributed Redis cache</li>
     * </ol>
     *
     * @param key the cache key to look up
     * @return the cached object, or {@code null} if not found in any cache level
     */
    @SuppressWarnings("unchecked")
    public Object getFromAllCache(String key) {
        Object value = null;

        if (enableLocalCache) {
            Cache<String, Object> localCache = getLocalCache();
            value = localCache.getIfPresent(key);
        }

        if (Objects.nonNull(value)) {
            return value;
        }

        value = redisClient.get(key);
        if (enableLocalCache && Objects.nonNull(value)) {
            // Redis 命中后回填本地缓存，避免二级缓存只写不热。
            getLocalCache().put(key, value);
        }

        return value;
    }

    /**
     * Retrieves data using the cache-aside pattern.
     * <p>
     * If the data is not found in cache, it will be loaded from the database
     * via the provided supplier and then cached for future requests.
     * </p>
     *
     * @param key      the cache key
     * @param supplier the database query function to execute on cache miss
     * @return the cached or freshly loaded object
     * @throws BlinkException if cache operation fails
     */
    public Object getFromCacheOrDB(String key, Supplier<?> supplier) {
        try {
            Object value = getFromAllCache(key);

            if (Objects.nonNull(value)) {
                return value;
            }

            value = supplier.get();

            log.info("Cache miss for key: {}, loading from database", key);

            if (Objects.nonNull(value)) {
                resetCache(key, value);
            }

            return value;
        } catch (Exception e) {
            log.error("Failed to get from cache or database, key: {}", key, e);
            throw new BlinkException(e, "Cache operation failed");
        }
    }

    /**
     * Refreshes the cache with the given key-value pair.
     * <p>
     * This method updates both the local cache (if enabled) and Redis cache.
     * The old cache entry is deleted before setting the new value.
     * </p>
     *
     * @param key   the cache key to refresh
     * @param value the new value to cache
     */
    public void resetCache(String key, Object value) {
        if (enableLocalCache) {
            Cache<String, Object> localCache = getLocalCache();
            localCache.put(key, value);
            log.info("Key: {} has been put into local cache", key);
        }

        redisClient.delete(key);
        redisClient.set(key, value);
    }

    /**
     * Bulk loads cache data from the database.
     * <p>
     * This method performs the following operations:
     * </p>
     * <ol>
     *   <li>Deletes all existing cache entries with the given prefix</li>
     *   <li>Bulk sets the new cache entries in Redis</li>
     *   <li>Loads the entries into local cache (if enabled)</li>
     * </ol>
     *
     * @param keyPrefix   the cache key prefix to use for batch operations
     * @param getCacheMap the function that returns the cache data as a Map
     */
    @SuppressWarnings("unchecked")
    public void loadCacheFromDB(String keyPrefix, Supplier<Map<String, Object>> getCacheMap) {
        Map<String, Object> map = getCacheMap.get();

        redisClient.deleteByPrefixScan(keyPrefix);
        redisClient.batchSet(map);

        if (enableLocalCache) {
            Cache<String, Object> localCache = getLocalCache();
            localCache.putAll(map);
            log.info("Local cache loaded with {} entries", map.size());
        }
    }

    /**
     * 获取本地缓存实例
     *
     * @return Caffeine 本地缓存
     */
    @SuppressWarnings("unchecked")
    private Cache<String, Object> getLocalCache() {
        return ApplicationContextUtil.getBean(Cache.class);
    }

    /**
     * 清除本地缓存中的指定 key
     * <p>
     * 当修改配置后，需要清除本地缓存以确保数据一致性
     * </p>
     *
     * @param key 要清除的缓存 key
     */
    public void clearLocalCache(String key) {
        if (enableLocalCache) {
            Cache<String, Object> localCache = getLocalCache();
            localCache.invalidate(key);
            log.info("Local cache invalidated for key: {}", key);
        }
    }

    /**
     * 批量清除本地缓存中的指定 keys
     * <p>
     * 当批量修改配置后，需要清除本地缓存以确保数据一致性
     * </p>
     *
     * @param keys 要清除的缓存 key 列表
     */
    public void clearLocalCache(List<String> keys) {
        if (enableLocalCache && keys != null && !keys.isEmpty()) {
            Cache<String, Object> localCache = getLocalCache();
            for (String key : keys) {
                localCache.invalidate(key);
            }
            log.info("Local cache invalidated for keys: {}", keys);
        }
    }
}
