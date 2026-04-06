package com.blink.gateway.component;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.service.RemoteService;
import com.blink.gateway.util.ReactiveCacheUtil;
import com.github.benmanes.caffeine.cache.AsyncCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多级缓存基础组件封装
 * 依次从本地-> redis -> 远程服务数据源 获取数据
 * 获取成功自动写入缓存 写入为同步写入 写入结果影响缓存读取结果 这样为了保证缓存一致性
 *
 * @author binblink
 */
@Component
@Slf4j
public class MultiLevelCacheComponent {

    @Resource
    private CacheManager localCacheManager;

    @Resource
    private ReactiveRedisClient redisClient;

    /**
     * 分布式锁等待时间
     */
    private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 本地缓存重建中的 key 计数器（防击穿）
     * key -> 等待中的请求数
     */
    private final ConcurrentHashMap<String, AtomicInteger> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 多级缓存获取数据
     *
     * @param localCacheName 缓存名称
     * @param key            缓存键
     * @param clazz          返回数据类型
     * @param service        远程服务URI（当需要远程获取时使用）
     * @return 数据结果
     */
    public <T> Mono<T> get(String localCacheName, String key, Class<T> clazz, RemoteService<T> service) {

        Cache localCache = localCacheManager.getCache(localCacheName);
        AsyncCache<String, T> asyncCache = ReactiveCacheUtil.toAsyncCache(localCache);

        // 本地缓存未开启或配置错误
        if (asyncCache == null) {
            return getFromRemoteService(key, clazz, service);
        }
        return ReactiveCacheUtil.getMono(asyncCache, key, () -> getFromRemoteService(key, clazz, service));
    }

    /**
     * 从远程数据源中获取（带分布式锁保护）
     *
     * @param key     缓存键
     * @param clazz   返回数据类型
     * @param service 远程服务URI（当需要远程获取时使用）
     * @return 数据结果
     */
    private <T> Mono<T> getFromRemoteService(String key, Class<T> clazz, RemoteService<T> service) {

        return getFromRedis(key, clazz)
                // Redis 也为空，尝试获取分布式锁后调用远程服务
                .switchIfEmpty(Mono.defer(() ->
                        getWithDistributedLock(key, clazz, service)))
                .onErrorResume(e -> {
                    log.error("[MultiLevelCache] 获取远程数据异常 | key: {}, error: {}", key, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * 带分布式锁保护的远程数据获取
     * 防止多个实例同时调用远程服务
     *
     * @param key     缓存键
     * @param clazz   返回数据类型
     * @param service 远程服务
     * @return 数据结果
     */
    private <T> Mono<T> getWithDistributedLock(String key, Class<T> clazz, RemoteService<T> service) {
        String lockKey = key + ":lock";

        // 尝试获取分布式锁
        return tryAcquireLock(lockKey)
                .flatMap(acquired -> {
                    if (acquired) {
                        // 获取锁成功，调用远程服务
                        log.info("[MultiLevelCache] 获取分布式锁成功，调用远程服务 | key: {}", key);
                        return service.call(key, clazz)
                                .flatMap(cache -> {
                                    // 写入 Redis 缓存
                                    return setRedisCache(key, cache);
                                })
                                .doFinally(signal -> {
                                    // 释放锁
                                    releaseLock(lockKey);
                                });
                    } else {
                        // 未获取锁，等待其他实例写入后从 Redis 获取
                        log.info("[MultiLevelCache] 未获取锁，等待其他实例写入 | key: {}", key);
                        return waitForRedisValue(key, clazz);
                    }
                });
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey 锁 key
     * @return 是否获取成功
     */
    private Mono<Boolean> tryAcquireLock(String lockKey) {
        // 使用 Redis SETNX 实现分布式锁，过期时间 5 秒
        return redisClient.setIfAbsentWithExpire(lockKey, "1", Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("[MultiLevelCache] 获取锁失败 | lockKey: {}, error: {}", lockKey, e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁 key
     */
    private void releaseLock(String lockKey) {
        redisClient.delete(lockKey)
                .subscribe(
                        result -> log.debug("[MultiLevelCache] 释放锁成功 | lockKey: {}", lockKey),
                        error -> log.error("[MultiLevelCache] 释放锁失败 | lockKey: {}, error: {}", lockKey, error.getMessage())
                );
    }

    /**
     * 等待其他实例写入 Redis 后获取值
     *
     * @param key   缓存键
     * @param clazz 返回类型
     * @return 数据结果
     */
    private <T> Mono<T> waitForRedisValue(String key, Class<T> clazz) {
        // 轮询等待 Redis 有值，最多等待 3 秒
        return Mono.defer(() -> getFromRedis(key, clazz))
                .repeatWhenEmpty(10, flux -> flux.delayElements(Duration.ofMillis(300)))
                .timeout(LOCK_WAIT_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("[MultiLevelCache] 等待 Redis 值超时 | key: {}", key);
                    return Mono.empty();
                });
    }

    /**
     * 从 Redis 获取数据
     */
    private <T> Mono<T> getFromRedis(String key, Class<T> clazz) {
        return redisClient.get(key)
                .mapNotNull(e -> {
                    T value = JacksonUtil.convert(e, clazz);
                    if (value != null && log.isDebugEnabled()) {
                        log.debug("[MultiLevelCache] 从 Redis 获取缓存成功 | key: {}", key);
                    }
                    return value;
                })
                .switchIfEmpty(Mono.empty())
                .onErrorResume(e -> {
                    log.error("[MultiLevelCache] 请求 Redis 失败 | key: {}, error: {}", key, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * 设置 Redis 缓存
     */
    public <T> Mono<T> setRedisCache(String key, T value) {
        return redisClient.set(key, value)
                .flatMap(b -> {
                    if (b) {
                        return Mono.just(value);
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("[MultiLevelCache] 设置 Redis 缓存失败 | key: {}, error: {}", key, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * 删除本地缓存
     *
     * @param cacheName 缓存对象名称
     * @param key       缓存 key 值
     */
    public Mono<Boolean> evictLocalCache(String cacheName, String key) {
        return Mono.fromCallable(() -> {
                    try {
                        Cache localCache = localCacheManager.getCache(cacheName);
                        if (localCache != null) {
                            localCache.evictIfPresent(key);
                        }
                        return true;
                    } catch (Exception e) {
                        log.error("[MultiLevelCache] 删除本地缓存异常 | key: {}, error: {}", key, e.getMessage(), e);
                        throw new BlinkException(e, e.getMessage());
                    }
                })
                .doOnSuccess(r -> log.info("[MultiLevelCache] 删除本地缓存成功 | key: {}", key))
                .onErrorResume(e -> Mono.just(false));
    }

    /**
     * 设置本地缓存
     *
     * @param cacheName 缓存对象名称
     * @param key       缓存 key 值
     * @param value     缓存值
     * @return 成功/失败
     */
    public Mono<Boolean> setLocalCache(String cacheName, String key, Object value) {
        return Mono.fromCallable(() -> {
                    try {
                        Cache localCache = localCacheManager.getCache(cacheName);
                        if (localCache != null) {
                            localCache.put(key, value);
                        }
                        return true;
                    } catch (Exception e) {
                        log.error("[MultiLevelCache] 设置本地缓存异常 | key: {}, error: {}", key, e.getMessage(), e);
                        throw new BlinkException(e, e.getMessage());
                    }
                })
                .doOnSuccess(r -> log.info("[MultiLevelCache] 设置本地缓存成功 | key: {}", key))
                .onErrorResume(e -> Mono.just(false));
    }

    /**
     * 同时更新本地缓存和 Redis 缓存
     * 用于 operator="M" 场景，保证缓存一致性
     *
     * @param cacheName 缓存名称
     * @param key       缓存 key
     * @param value     缓存值
     * @return 是否成功
     */
    public Mono<Boolean> setLocalAndRedisCache(String cacheName, String key, Object value) {
        return setRedisCache(key, value)
                .flatMap(redisResult -> setLocalCache(cacheName, key, value))
                .onErrorResume(e -> {
                    log.error("[MultiLevelCache] 同时更新缓存失败 | key: {}, error: {}", key, e.getMessage(), e);
                    return Mono.just(false);
                });
    }

    /**
     * 删除 Redis 缓存
     *
     * @param key 缓存 key 值
     */
    public Mono<Boolean> evictRedisCache(String key) {
        return redisClient.delete(key)
                .map(r -> true)
                .doOnSuccess(r -> log.info("[MultiLevelCache] 删除 Redis 缓存成功 | key: {}", key))
                .onErrorResume(throwable -> {
                    log.error("[MultiLevelCache] 删除 Redis 缓存失败 | key: {}, error: {}", key, throwable.getMessage(), throwable);
                    return Mono.just(false);
                });
    }

    /**
     * 删除缓存（多级）
     */
    public Mono<Boolean> evictTransactional(String cacheName, String key) {
        return Mono.defer(() -> evictLocalCache(cacheName, key).then(evictRedisCache(key)))
                .onErrorResume(thr -> {
                    log.error("[MultiLevelCache] 删除多级缓存失败 | key: {}, error: {}", key, thr.getMessage(), thr);
                    return Mono.just(false);
                });
    }
}