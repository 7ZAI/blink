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

        //本地缓存未开启 或配置错误
        if (asyncCache == null) {
            return getFromRemoteService(key, clazz, service);
        }
        return ReactiveCacheUtil.getMono(asyncCache, key, () -> getFromRemoteService(key, clazz, service));
    }

    /**
     * 从远程数据源中获取
     *
     * @param key     缓存键
     * @param clazz   返回数据类型
     * @param service 远程服务URI（当需要远程获取时使用）
     * @return 数据结果
     */
    private <T> Mono<T> getFromRemoteService(String key, Class<T> clazz, RemoteService<T> service) {

        return getFromRedis(key, clazz)
                //redis 也为空 远程服务调用获取
                .switchIfEmpty(Mono.defer(() -> service.call(key, clazz))
                        //获取成功写回redis缓存 本地缓存写回由Caffeine保证
                        .flatMap(cache -> setRedisCache(key, cache))
                        .doOnNext(val -> log.info("调用远程服务成功 返回value:{}", val))
                        .switchIfEmpty(Mono.empty())
                        .onErrorResume(e -> {
                            log.error("远程调用异常！{}", e.getMessage(), e);
                            return Mono.empty();
                        }));
    }


    /**
     * 从Redis获取数据
     */
    private <T> Mono<T> getFromRedis(String key, Class<T> clazz) {
        log.info("尝试从Redis获取 缓存参数 key:{}", key);
        return redisClient.get(key)
                .mapNotNull(e -> {
                    T value = JacksonUtil.convert(e, clazz);
                    if (value != null) {
                        if (value.toString().length() < 1000) {
                            log.info("从Redis获取缓存参数 成功! key:{},value:{}", key, value);
                        } else {
                            log.info("从Redis获取缓存参数 成功! key:{},value length:{} 超过设置值 省略", key, value.toString().length());
                        }
                    }
                    return value;
                })
                .switchIfEmpty(Mono.empty())
                .onErrorResume(e -> {
                    log.error("请求redis失败{},key:{}", e.getMessage(), key, e);
                    return Mono.empty();
                });
    }

    /**
     * 设置Redis缓存
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
                    log.error("设置redis缓存失败！{}", e.getMessage(), e);
                    return Mono.empty();
                });
    }


    /**
     * 删除本地缓存
     * 注意 key未命中也会返回true
     * 删除操作成功仅代表当前key不存在，所以特别注意传递的key值
     *
     * @param key
     */
    public Mono<Boolean> evictLocalCache(String cacheName, String key) {
        return Mono.fromCallable(() -> {
            try {
                Cache localCache = localCacheManager.getCache(cacheName);
                if (localCache != null) {
                    localCache.evictIfPresent(key);
                }
                //不抛异常 即认为成功 即使key 不存在
                return true;
            } catch (Exception e) {
                log.error("删除本地缓存 出现异常！" + e.getMessage(), e);
                throw new BlinkException(e, e.getMessage());
            }
        }).doOnSuccess(r -> log.info("本地缓存删除成功, key: {}", key)).onErrorResume(e -> Mono.just(false));
    }

    /**
     * 删除Redis缓存
     * 注意 key未命中也会返回true
     * 删除操作成功仅代表当前key不存在，所以特别注意传递的key值
     *
     * @param key
     */
    public Mono<Boolean> evictRedisCache(String key) {
        // 再删除Redis缓存，如果失败会回滚（通过异常传播）
        return redisClient.delete(key)
                // 这里的 r 通常是 Boolean (表示是否存在并删除) 或 Long (删除的数量)
                // 无论 r 是 true 还是 false，只要没进 onErrorResume，业务上都视为成功
                .map(r -> true)
                .doOnSuccess(r -> {
                    // 此时 r 永远是 true
                    log.info("Redis cache cleanup task completed (regardless of whether the original key existed), key: {}", key);
                })
                .onErrorResume(throwable -> {
                    log.error("Redis cache deletion failed, manual cleanup may be required., key: {}", key, throwable);
                    // 这里可以添加补偿逻辑，比如重试或记录到死信队列
                    return Mono.just(false);
                });
    }


    /**
     * 删除缓存（多级）
     */
    public Mono<Boolean> evictTransactional(String cacheName, String key) {
        return Mono.defer(() -> {
            // 先删除本地缓存
            return evictLocalCache(cacheName, key).then(evictRedisCache(key));
        }).onErrorResume(thr -> {
            log.error("Deletion of multi-level cache failed, the entire operation aborted. key: {}", key, thr);
            return Mono.just(false);
        });
    }

}