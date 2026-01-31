package com.blink.gateway.component;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.service.RemoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 多级缓存基础组件封装
 * 依次从本地 redis 远程服务数据源获取数据
 * 适用于不频繁改动的数据
 *
 * @author binblink
 */
@Component
@Slf4j
public class MultiLevelCacheComponent {


    @Resource(name = "gatewayLocalCache")
    private CacheManager localCacheManager;

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private BlinkGatewayProperties properties;

    // 本地缓存名称
    private static final String LOCAL_CACHE_NAME = "localCache";

    /**
     * 多级缓存获取数据
     *
     * @param key     缓存键
     * @param clazz   返回数据类型
     * @param service 远程服务URI（当需要远程获取时使用）
     * @return 数据结果
     */
    public <T> Mono<T> get(String key, Class<T> clazz, RemoteService<T> service) {
        return getFromLocalCache(key, clazz)
                //本地缓存为空
                .switchIfEmpty(Mono.defer(() -> getFromRedis(key, clazz)
                        //成功则设置本地值
                        .flatMap(value -> setLocalCache(key, value)
                                //redis 也为空 远程服务调用获取
                                .switchIfEmpty(Mono.defer(() -> service.call(key, clazz))
                                        //获取成功写回缓存
                                        .flatMap(cache -> setLocalAndRedisCache(key, cache))
                                        .switchIfEmpty(Mono.empty())
                                        .doOnNext(val -> log.info("远程调用base-app服务成功 返回value:{}", val))
                                        .onErrorResume(e -> {
                                            log.error("远程调用异常！{}", e.getMessage(), e);
                                            return Mono.empty();
                                        }))
                        )));
    }

    /**
     * 从本地缓存获取数据
     */
    private <T> Mono<T> getFromLocalCache(String key, Class<T> clazz) {
        log.info("尝试从本地缓存获取 缓存参数 key:{}", key);

        return Mono.fromCallable(() -> {
            Boolean configEnable = properties.getLocalCacheEnable();
            boolean enableLocalCache = configEnable != null ? configEnable : false;
            // 未开启
            if (!enableLocalCache) {
                throw new BlinkException("本地缓存未开启关闭！");
            }
            Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
            if (localCache != null) {
                Cache.ValueWrapper valueWrapper = localCache.get(key);
                if (valueWrapper != null) {
                    T value = clazz.cast(valueWrapper.get());
                    assert value != null;
                    String valueStr = value.toString();
                    if (valueStr.length() > 1000) {
                        valueStr = value.toString().substring(0, 1000) + "......";
                    }
                    log.info("从本地缓存获取缓存参数 成功! key:{},value:{}", key, valueStr);
                    return value;
                }
            }
            throw new BlinkException("本地缓存配置错误！");
        }).onErrorResume(e -> Mono.empty());
    }

    /**
     * 从Redis获取数据
     */
    private <T> Mono<T> getFromRedis(String key, Class<T> clazz) {
        log.info("尝试从Redis获取 缓存参数 key:{}", key);
        return redisClient.get(key)
                .map(e -> {
                    T value = clazz.cast(e);
                    if (value.toString().length() < 1000) {
                        log.info("从Redis获取缓存参数 成功! key:{},value:{}", key, value);
                    } else {
                        log.info("从Redis获取缓存参数 成功! key:{},value length:{} 超过设置值 省略", key, value.toString().length());
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
     * 设置本地缓存
     */
    public <T> Mono<T> setLocalCache(String key, T value) {

        Boolean configEnable = properties.getLocalCacheEnable();
        boolean enableLocalCache = configEnable != null ? configEnable : false;
        // 未开启
        if (!enableLocalCache) {
            return Mono.just(value);
        }
        return Mono.fromCallable(() -> {
            try {
                Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
                if (localCache != null) {
                    localCache.put(key, value);
                    return value;
                }
                return null;
            } catch (Exception e) {
                log.error("设置本地缓存异常 请检查配置 {}", e.getMessage(), e);
                return null;
            }
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


    private <T> Mono<T> setLocalAndRedisCache(String key, T value) {
        log.info("从远程服务获取参数 成功！key:{},value:{}", key, value);
        return setRedisCache(key, value)
                .then(setLocalCache(key, value));

    }


    /**
     * 删除本地缓存
     *
     * @param key
     */
    public Mono<Boolean> evictLocalCache(String key) {
        return Mono.fromCallable(() -> {
            try {
                Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
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
     *
     * @param key
     */
    public Mono<Boolean> evictRedisCache(String key) {
        // 再删除Redis缓存，如果失败会回滚（通过异常传播）
        return redisClient.delete(key)
                // 这里的 r 通常是 Boolean (表示是否存在并删除) 或 Long (删除的数量)
                // 无论 r 是 true 还是 false，只要没进 doOnError，业务上都视为成功
                .map(r -> true)
                .doOnSuccess(r -> {
                    // 此时 r 永远是 true
                    log.info("Redis缓存清理任务完成（无论原键是否存在）, key: {}", key);
                })
                .onErrorResume(throwable -> {
                    log.error("Redis缓存删除失败，可能需要手动清理, key: {}", key, throwable);
                    // 这里可以添加补偿逻辑，比如重试或记录到死信队列
                    return Mono.just(false);
                });
    }


    /**
     * 事务性删除缓存（多级）
     */
    public Mono<Boolean> evictTransactional(String key) {
        return Mono.defer(() -> {
            // 先删除本地缓存
            return evictLocalCache(key).then(evictRedisCache(key));
        }).onErrorResume(thr -> {
            log.error("删除多级缓存失败，整体操作中止, key: {}", key, thr);
            return Mono.just(false);
        });
    }

}