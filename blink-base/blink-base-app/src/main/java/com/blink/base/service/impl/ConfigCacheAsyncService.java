package com.blink.base.service.impl;

import com.blink.base.constants.CommonConstants;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 配置缓存异步清理服务
 * <p>
 * 使用单独的 Service 类处理异步缓存删除，避免 Spring AOP 同类调用限制
 * @Async 方法必须通过代理调用才能生效
 * </p>
 *
 * @author binblink
 */
@Slf4j
@Service
public class ConfigCacheAsyncService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private CacheComponent cacheComponent;

    /**
     * 异步延迟删除单项配置缓存
     * <p>
     * 使用异步方式执行延迟双删，避免阻塞主线程
     * 延迟时间 > 请求时间 + redis 设置值的时间
     * </p>
     *
     * @param cacheKey 缓存 Key
     */
    @Async
    public void asyncDelayedDelete(String cacheKey) {
        try {
            Thread.sleep(CommonConstants.CACHE_DELAY_DELETE_MS);
            deleteConfigCache(cacheKey);
            log.info("[ConfigCache] 异步延迟删除缓存成功 | cacheKey: {}", cacheKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[ConfigCache] 异步延迟删除缓存被中断 | cacheKey: {}", cacheKey, e);
        }
    }

    /**
     * 异步批量延迟删除缓存
     *
     * @param cacheKeys 缓存 Key 列表
     */
    @Async
    public void asyncDelayedDeleteBatch(List<String> cacheKeys) {
        try {
            Thread.sleep(CommonConstants.CACHE_DELAY_DELETE_MS);
            deleteConfigCaches(cacheKeys);
            log.info("[ConfigCache] 异步批量延迟删除缓存成功 | keys: {}", cacheKeys);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[ConfigCache] 异步批量延迟删除缓存被中断 | keys: {}", cacheKeys, e);
        }
    }

    /**
     * 删除单项配置缓存（Redis + 本地缓存）
     *
     * @param cacheKey 缓存 Key
     */
    public void deleteConfigCache(String cacheKey) {
        redisClient.delete(cacheKey);
        cacheComponent.clearLocalCache(cacheKey);
        log.info("[ConfigCache] 删除配置缓存 | cacheKey: {}", cacheKey);
    }

    /**
     * 批量删除配置缓存（Redis + 本地缓存）
     *
     * @param cacheKeys 缓存 Key 列表
     */
    public void deleteConfigCaches(List<String> cacheKeys) {
        redisClient.deleteKeys(cacheKeys);
        cacheComponent.clearLocalCache(cacheKeys);
        log.info("[ConfigCache] 批量删除配置缓存 | keys: {}", cacheKeys);
    }
}