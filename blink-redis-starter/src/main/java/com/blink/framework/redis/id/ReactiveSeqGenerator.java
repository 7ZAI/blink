package com.blink.framework.redis.id;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactive 版 顺序号生成器
 *
 * @Author binblink
 * @Date 2025/8/9
 */
@Slf4j
public class ReactiveSeqGenerator {

    private final ReactiveRedisClient reactiveRedisClient;

    private final BlinkRedisProperties properties;

    /**
     * 每个 key 对应一个本地缓存对象
     */
    private final Map<String, KeySequenceCache> cacheMap = new ConcurrentHashMap<>();

    /**
     * 预取 保存预取的顺序号缓存对象
     */
    private final  Map<String, KeySequenceCache> cachePrefetch = new ConcurrentHashMap<>();


    public ReactiveSeqGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties properties) {
        this.reactiveRedisClient = reactiveRedisClient;
        this.properties = properties;
    }


    /**
     *  获取某个 key 的下一个序列号
     * @param key
     * @param maxSeq 最大值限制 字符串格式
     * @return
     */
    public Mono<Long> nextId(String key, String maxSeq) {
        //获取顺序号缓存对象 不存在则创建
        KeySequenceCache cache = cacheMap.computeIfAbsent(key, k -> new KeySequenceCache());
        long current = cache.localCounter.get();
        long maxL = cache.localMax.get();

        // 本地缓存未达到上限
        if (current < maxL) {
            log.debug("生产id组件，从缓存对象中获取顺序号");
            //达到阈值 异步请求redis获取段号
            if(shouldPrefetch(maxL,current,properties.getIdGenerator().getKeySteps(key),key)){
                preFetch(key,maxSeq).subscribe();
            }
            //边界： 当cache.localMax -1 时 正好incrementAndGet 完成 +1
            return Mono.just(cache.localCounter.incrementAndGet());
        }

        //用完从预取缓存中获取
        if(cache.localCounter.get() >= cache.localMax.get()){
           KeySequenceCache newCache = cachePrefetch.get(key);
            //预取缓存存在
            if(Objects.nonNull(newCache)){
                cache = newCache;
                cacheMap.put(key,newCache);
                cachePrefetch.remove(key);
                return Mono.just(cache.localCounter.incrementAndGet());
            }
        }

        // 本地用完，去 Redis 拉取下一区间顺序号 缓存到本地
        return getOrFetchBatch(cache,key,maxSeq);
    }

    private synchronized Mono<Long> preFetch(String key, String maxSeq) {
        //脚本参数 listParam
        List<String> listParam = new ArrayList<>();
        listParam.add(maxSeq);
        listParam.add(String.valueOf(properties.getIdGenerator().getKeySteps(key)));

        log.debug("生产id组件调用redis服务，预存缓存区间！key:{}",key);


        return reactiveRedisClient.execute(
                        properties.getIdGenerator().getLuaScript(),
                        Collections.singletonList(key),
                        Long.class,
                        new LongRedisSerializer(),
                        listParam
                )
                .single()
                //拿到上限值
                .map(maxValue -> {
                    Integer step = properties.getIdGenerator().getKeySteps(key);
                    //新的预存
                    KeySequenceCache cache = new KeySequenceCache();
                    //计算起始值 因为redis脚本一直是递增的并保证单线程执行脚本 不存在撞区间的情况
                    long batchStart = maxValue - step + 1;
                    cache.localCounter.set(batchStart);
                    cache.localMax.set(maxValue);
                    cachePrefetch.put(key,cache);
                    return batchStart;
                });
    }

    /**
     * 并发安全地获取批次
     */
    private  Mono<Long> getOrFetchBatch(KeySequenceCache cache, String key, String maxSeq) {

        //二次判断 其他等待竞争线程获得执行权后
        if(cache.localCounter.get() < cache.localMax.get()){
            return Mono.just(cache.localCounter.incrementAndGet());
        }

        //脚本参数 listParam
        List<String> listParam = new ArrayList<>();
        listParam.add(maxSeq);
        listParam.add(String.valueOf(properties.getIdGenerator().getKeySteps(key)));

        log.debug("生产id组件调用redis服务，获取新的缓存区间！key:{}",key);

        return reactiveRedisClient.execute(
                        properties.getIdGenerator().getLuaScript(),
                        Collections.singletonList(key),
                        Long.class,
                        new LongRedisSerializer(),
                        listParam
                )
                .single()
                //拿到上限值
                .map(maxValue -> {
                    Integer step = properties.getIdGenerator().getKeySteps(key);
                    //计算起始值 因为redis脚本一直是递增的并保证单线程执行脚本 不存在撞区间的情况
                    long batchStart = maxValue - step + 1;
                    cache.localCounter.set(batchStart);
                    cache.localMax.set(maxValue);
                    return batchStart;
                });
    }

    // 当剩余ID少于阈值并且预取缓存为空时，提前获取新区间
    private boolean shouldPrefetch(long max,long current,long batchSize,String key) {
        long remaining = max - current;
        // 剩余20%时预取
        return remaining <= batchSize * 0.2 && !cachePrefetch.containsKey(key);
    }

    /**
     * 保存单个 key 的缓存数据
     */
    private static class KeySequenceCache {
        //递增计数器
        final AtomicLong localCounter = new AtomicLong(0);
        //最大值限制
        final AtomicLong localMax = new AtomicLong(0);

    }
}
