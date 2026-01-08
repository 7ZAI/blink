package com.blink.framework.redis.component;

import com.blink.framework.common.utils.ApplicationContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 缓存组件 只在web sync 下生效 reactive 无法使用
 * 
 * @author binblink
 */
@Slf4j
public class CacheComponent {

    @Resource
    private  RedisClient redisClient;

    private Boolean enableLocalCache = false;

    public CacheComponent(Boolean enableLocalCache){
        this.enableLocalCache = enableLocalCache;
    }

    /**
     * 从所有层级缓存中获取 缓存对象
     *
     * @param key
     * @return 缓存对象
     */
    public Object getFromAllCache(String key) {

        Object value = null;
        //先从本地缓存获取
        if (enableLocalCache) {
            Cache localCache = ApplicationContextUtil.getBean(Cache.class);
            value = localCache.getIfPresent(key);
        }

        if (Objects.nonNull(value)) {
            return value;
        }
        //再从redis缓存获取
        value = redisClient.get(key);

        return value;
    }

    /**
     * 透过缓存 获取数据 缓存没有 会查询数据库
     *
     * @param key      键
     * @param supplier sql 执行函数
     * @return 返回对象
     */
    public Object getFromCacheOrDB(String key, Supplier supplier) {

        //先从缓存获取
        Object value = getFromAllCache(key);

        if (Objects.nonNull(value)) {
            return value;
        }
        //数据库获取
        value = supplier.get();

        log.info("key:{} Missed cache,get from database", key);

        if (Objects.nonNull(value)) {
            //刷新缓存
            resetCache(key, value);
        }

        return value;
    }

    /**
     * 异步刷新缓存
     */
    @Async
    public void resetCache(String key, Object value) {

        if (enableLocalCache) {
            Cache localCache = ApplicationContextUtil.getBean(Cache.class);
            localCache.put(key, value);
            log.info("key:{} have put to the localCache", key);
        }

        redisClient.delete(key);
        redisClient.set(key, value);

    }


    /**
     * 从数据库中加载缓存
     *
     * @param keyPrefix   缓存key前缀
     * @param getCacheMap 获取缓存map函数
     */
    public void loadCacheFromDB(String keyPrefix, Supplier<Map<String, Object>> getCacheMap) {

        Map<String, Object> map = getCacheMap.get();
        //批量删除
        redisClient.deleteByPrefixScan(keyPrefix);
        //批量添加
        redisClient.batchSet(map);
        //开启本地缓存则存入
        if (enableLocalCache) {
            Cache localCache = ApplicationContextUtil.getBean(Cache.class);
            localCache.putAll(map);

            log.info("localCache loading cache cacheSize:{}", map.size());
        }
    }


}
