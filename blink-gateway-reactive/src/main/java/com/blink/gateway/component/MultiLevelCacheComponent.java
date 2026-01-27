package com.blink.gateway.component;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
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
                .switchIfEmpty(Mono.defer(()->getFromRedis(key, clazz))
                        .doOnNext(value -> setLocalCache(key, value))
                        .switchIfEmpty(Mono.defer(()-> service.call(key, clazz))));
    }

    /**
     * 从本地缓存获取数据
     */
    private <T> Mono<T> getFromLocalCache(String key, Class<T> clazz) {
        log.info("尝试从本地缓存获取 缓存参数 key:{}", key);

        return Mono.fromCallable(() -> {
            Boolean configEnable = properties.getCache().getLocalCacheEnable();
            boolean enableLocalCache =  configEnable != null ? configEnable : false;
            // 未开启
            if(!enableLocalCache){
                return null;
            }
            Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
            if (localCache != null) {
                Cache.ValueWrapper valueWrapper = localCache.get(key);
                if (valueWrapper != null) {
                    T value = clazz.cast(valueWrapper.get());
                    String valueStr = value.toString();
                    if(valueStr.length()>1000){
                         valueStr = value.toString().substring(0, 1000) + "......";
                    }
                    log.info("从本地缓存获取缓存参数 成功! key:{},value:{}", key, valueStr);
                    return value;
                }
            }
            return null;
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
                    if(value.toString().length() < 1000){
                        log.info("从Redis获取缓存参数 成功! key:{},value:{}", key, value);
                    }else{
                        log.info("从Redis获取缓存参数 成功! key:{},value length:{} 超过设置值 省略", key, value.toString().length());
                    }
                    return value;
                })
                .onErrorResume(e -> {
//                    e.printStackTrace();
                    log.error("{}",e.getMessage());
                    log.error("从Redis获取缓存参数 失败! key:{}", key);
                    return Mono.empty();
                });
    }

    /**
     * 设置本地缓存
     */
    public <T> void setLocalCache(String key, T value) {
        try {
            Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
            if (localCache != null) {
                localCache.put(key, value);
            }
        } catch (Exception e) {
            Mono.error(new BlinkException(e.getMessage(), e.getCause(), BlinkErrorCodeEnum.BLINK_ERROR.getCode()));
        }
    }

    /**
     * 设置Redis缓存
     */
    public <T> Mono<Boolean> setRedisCache(String key, T value) {
        return redisClient
                .set(key, value)
                .onErrorResume(e -> Mono.just(false));
    }


    /**
     * 删除缓存（多级）
     */
    public Mono<Void> evict(String key) {
        return Mono.fromRunnable(() -> evictLocalCache(key))
                .then(evictRedisCache(key))
                .onErrorResume(throwable -> {
                    log.error("删除缓存失败, key: {}", key, throwable);
                    return Mono.empty();
                });
    }

    /**
     * 删除本地缓存
     */
    public void evictLocalCache(String key) {
        try {
            Cache localCache = localCacheManager.getCache(LOCAL_CACHE_NAME);
            if (localCache != null) {
                localCache.evict(key);
                log.debug("本地缓存删除成功, key: {}", key);
            }
        } catch (Exception e) {
            log.warn("删除本地缓存失败, key: {}", key, e);
            // 不抛出异常，继续执行Redis删除
        }
    }

    /**
     * 事务性删除缓存（多级）
     */
    public Mono<Void> evictTransactional(String key) {
        return Mono.defer(() -> {
            try {
                // 先删除本地缓存
                evictLocalCache(key);

                // 再删除Redis缓存，如果失败会回滚（通过异常传播）
                return redisClient.del(key)
                        .doOnSuccess(count -> {
                            log.info("缓存删除成功, key: {}", key);
                        })
                        .doOnError(throwable -> {
                            log.error("Redis缓存删除失败，可能需要手动清理, key: {}", key, throwable);
                            // 这里可以添加补偿逻辑，比如重试或记录到死信队列
                        })
                        .then();
            } catch (Exception e) {
                log.error("本地缓存删除失败，整体操作中止, key: {}", key, e);
                return Mono.error(e);
            }
        });
    }

    /**
     * 删除Redis缓存
     */
    private Mono<Void> evictRedisCache(String key) {
        return redisClient.del(key)
                .doOnSuccess(b -> {
                    if (b) {
                        log.debug("Redis缓存删除成功, key: {}", key);
                    } else {
                        log.debug("Redis缓存键不存在, key: {}", key);
                    }
                })
                .then();
    }
}