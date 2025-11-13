package com.blink.framework.redis.id;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive 版 顺序号生成器
 *
 * @Author binblink
 * @Date 2025/8/9
 */
public class ReactiveSeqGenerator {

    private final ReactiveRedisClient reactiveRedisClient;

    private final BlinkRedisProperties properties;

    /**
     * 每个 key 对应一个本地缓存对象
     */
    private final Map<String, KeySequenceCache> cacheMap = new ConcurrentHashMap<>();


    public ReactiveSeqGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties properties) {
        this.reactiveRedisClient = reactiveRedisClient;
        this.properties = properties;
    }

    /**
     * 获取某个 key 的下一个序列号
     *
     */
    public Mono<Long> nextId(String key,String maxSeq) {
        KeySequenceCache cache = cacheMap.computeIfAbsent(key, k -> new KeySequenceCache());

        // 本地还有缓存
        if (cache.localCounter.get() < cache.localMax.get()) {
            return Mono.just(cache.localCounter.incrementAndGet());
        }

        // 本地用完，去 Redis 拉取下一批
        return getOrFetchBatch(cache, key,maxSeq)
                .map(maxValue -> {
                    Integer step = properties.getIdGenerator().getKeySteps(key);
                    long batchStart = maxValue - step + 1;
                    cache.localCounter.set(batchStart);
                    cache.localMax.set(maxValue);
                    return batchStart;
                });
    }

    /**
     * 并发安全地获取批次
     */
    private Mono<Long> getOrFetchBatch(KeySequenceCache cache, String key,String maxSeq) {
        Mono<Long> existing = cache.fetchingBatch.get();
        if (existing != null) {
            return existing;
        }
        List<String> list = new ArrayList<>();
        list.add(maxSeq);
        list.add(String.valueOf(properties.getIdGenerator().getKeySteps(key)));

        Mono<Long> newMono = fetchNextBatch(key,list)
                .doFinally(signal -> cache.fetchingBatch.set(null))
                .cache();

        if (cache.fetchingBatch.compareAndSet(null, newMono)) {
            return newMono;
        } else {
            return cache.fetchingBatch.get();
        }
    }

    /**
     * 调用 Lua 从 Redis 获取下一个批次的最大值
     */
    private Mono<Long> fetchNextBatch(String key,List<String> vals) {
        return reactiveRedisClient.execute(
                properties.getIdGenerator().getLuaScript(),
                Collections.singletonList(key),
                Long.class,
                new LongRedisSerializer(),
                vals
        ).single();
    }

    /**
     * 保存单个 key 的缓存数据
     */
    private static class KeySequenceCache {
        final AtomicLong localCounter = new AtomicLong(0);
        final AtomicLong localMax = new AtomicLong(0);
        final AtomicReference<Mono<Long>> fetchingBatch = new AtomicReference<>();
    }
}
