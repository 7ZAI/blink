package com.blink.framework.redis.id;


import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Reactive 版 顺序号生成器
 *
 * @Author binblink
 */
@Slf4j
public class ReactiveSeqGenerator {


    /**
     * 每个 key 对应一个本地缓存对象
     */
    private final Map<String, SeqSegment> SEQ_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, AtomicStatus> STATUS_MAP = new ConcurrentHashMap<>();

    /**
     * seq预取缓存对象key前缀 完整为前缀+key
     */
    private static final String PRE_FETCH_SEQ_PREFIX = "seq:prefetch:";

    private static final String DEFAULT_KEY_PREFIX = "seq:";

    private final ReactiveRedisClient reactiveRedisClient;

    private final BlinkRedisProperties properties;


    @PostConstruct
    public void init() {
        Map<String, BlinkRedisProperties.IdGenerator.SeqParam> map = properties.getIdGenerator().getSeqParam();

        for (Map.Entry<String, BlinkRedisProperties.IdGenerator.SeqParam> entry : map.entrySet()) {
            String key = entry.getKey();
            SEQ_CACHE.put(key, new SeqSegment(0, 0));
            STATUS_MAP.put(key, new AtomicStatus());

        }
    }


    public ReactiveSeqGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties properties) {
        this.reactiveRedisClient = reactiveRedisClient;
        this.properties = properties;
    }


    /**
     * 获取某个 key 的下一个序列号
     *
     * @param key
     * @param maxSeq 最大值限制 字符串格式
     * @return
     */
    public Mono<Long> nextSeq(String key, String maxSeq) {

        if (StrUtil.isBlank(key)) {
            log.error("generateSeq key is blank");
            return Mono.error(new BlinkException("key 不能为空"));
        }

        //未配置的key
        if(!SEQ_CACHE.containsKey(key)) {
            log.error("当前key：{} 未被配置！请先配置",key);
            throw new RuntimeException("key为配置");
        }

        //未配置默认为1000
        final Integer steps = this.properties.getIdGenerator().getkeySteps(key);

        //获取顺序号缓存对象
        SeqSegment cache = SEQ_CACHE.get(key);

        //获取顺序号缓存对象
        AtomicStatus status = STATUS_MAP.get(key);

        long nextSeq = cache.getNextId();
        // 本地缓存未达到上限
        if (nextSeq != -1) {
            //达到阈值
            if (shouldPrefetch(cache, key, steps)) {
                //CAS 只允许一个线程执行预取
                if (status.prefetching.compareAndSet(false, true)) {
                    preFetch(key, maxSeq, steps)
                            .doOnError(fe -> status.prefetching.set(false))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }
                //CAS 失败线程什么都不用做
            }
            //
            return Mono.just(nextSeq);
        }

        return dealWithSeqCacheRunOut(key, maxSeq, steps);

    }


    /**
     * 处理seq缓存使用完的情况
     *
     * @param key
     * @param maxSeq
     * @param steps
     * @return
     */
    private Mono<Long> dealWithSeqCacheRunOut(String key, String maxSeq, Integer steps) {
        SeqSegment seqSegment = SEQ_CACHE.get(key);
        AtomicStatus status = STATUS_MAP.get(key);
        //号段用尽了 缓存为空，准备发起刷新
        CompletableFuture<SeqSegment> newFuture = new CompletableFuture<>();
        //  CAS 竞争：尝试成为 Leader 只有一个线程能成功
        if (status.loadingFuture.compareAndSet(null, newFuture)) {
            log.info("抢占成功 准备发起远程调用刷新本地缓存");
            //防止允许刷新瞬间 立马有线程抢到执行权（概率非常低）
            // 双重检查 竞争线程获得执行权 但是第一个线程已经刷新计数器了 防止重复进行远程调用 覆盖未使用的缓存seq
            long doubleCheckVal = seqSegment.getNextId();
            if (doubleCheckVal != -1) {
                //首次成功的线程已经刷新缓存
                log.debug("非首次竞争成功的线程，从缓存对象中获取顺序号");
                // 告知所有等待者：任务完成！
                return Mono.just(doubleCheckVal)
                        .doOnError(newFuture::completeExceptionally)
                        .doFinally(s -> {
                            //清除刷新任务 重新允许刷新
                            newFuture.complete(seqSegment);
                            status.loadingFuture.set(null);
                        });
            }
            String preKey = PRE_FETCH_SEQ_PREFIX + key;
            //如果预取完成了拿预取的对象刷新
            if (SEQ_CACHE.containsKey(preKey)) {
                return refreshWithPreFetchCache(status, key, maxSeq)
                        .doOnError(newFuture::completeExceptionally)
                        .doFinally(s -> {
                            //清除刷新任务 重新允许刷新
                            newFuture.complete(seqSegment);
                            status.loadingFuture.set(null);
                        });
            }
            //未预取 则发起新redis请求 获取下一段号
            return refreshWithNewCallRedis(status, key, maxSeq, steps, newFuture)
                    .doOnError(newFuture::completeExceptionally)
                    .doFinally(s -> {
                        //清除刷新任务 重新允许刷新
                        newFuture.complete(seqSegment);
                        status.loadingFuture.set(null);
                    });

        } else {
            // Follwer 线程 CAS失败的线程 说明已经有人在跑了，获取那个正在跑的 Future
            CompletableFuture<SeqSegment> currentFuture = status.loadingFuture.get();
            if (currentFuture != null) {
                try {
                    // 阻塞等待结果（这里利用了 Future 的等待机制，比锁更轻量）
                    // 注意：这里我们不需要返回值，只需要等它结束 因为一得到值 会进入while 重新取值
                    currentFuture.join();
                } catch (Exception joinE) {
                    // 如果 Leader 挂了，这里会抛出异常，忽略并重试
                    log.error("执行缓存刷新的CompletableFuture 未完成！", joinE);
                }
            }
            //递归获取
            return nextSeq(key, maxSeq);
        }
    }

    /**
     * 使用预取的对象刷新段号缓存
     *
     * @param status 段号缓存对象
     * @param key    key值
     * @param maxSeq 最大值
     * @return Mono<Long>
     */
    private Mono<Long> refreshWithPreFetchCache(AtomicStatus status, String key, String maxSeq) {

        String preKey = PRE_FETCH_SEQ_PREFIX + key;
        SeqSegment seqSegment = SEQ_CACHE.get(preKey);

        SEQ_CACHE.put(key, seqSegment);
        SEQ_CACHE.remove(preKey);
        //放开预取preFetch限制
        status.prefetching.set(false);

        return Mono.just(seqSegment.getNextId());
    }

    /**
     * 发起新的请求到redis 拿到新结果刷新缓存
     *
     * @param key       key值
     * @param maxSeq    最大值
     * @param steps     步长
     * @param newFuture 当同步器用
     * @return Mono<Long>
     */
    private Mono<Long> refreshWithNewCallRedis(AtomicStatus status, String key, String maxSeq, Integer steps, CompletableFuture<SeqSegment> newFuture) {

        return executeLuaFromRedisWithRetry(key, maxSeq, steps)
                //拿到上限值
                .map(maxValue -> {
                    //计算起始值 因为redis脚本一直是递增的并保证单线程执行脚本 不存在撞区间的情况
                    long batchStart = maxValue - steps;
                    //刷新值
                    SeqSegment newSeq = new SeqSegment(batchStart, maxValue);

                    log.info("非预取 执行 Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, steps);

                    SEQ_CACHE.put(key, newSeq);
                    return newSeq.getNextId();
                })
                .doOnError(error -> {
                    // 异常处理
                    log.error("执行刷新Seq缓存的CompletableFuture 失败！", error);
                    newFuture.completeExceptionally(error);
                }).doFinally(signal -> {
                    //清除刷新任务 重新允许刷新
                    status.loadingFuture.set(null);
                });
    }


    /**
     * 预取 seq段号 缓存（只有一个线程能执行）
     *
     * @param key
     * @param maxSeq
     * @param steps
     * @return -1L preFetch 运行在 doOnNext中不影响原本流的结果
     */
    private Mono<SeqSegment> preFetch(String key, String maxSeq, Integer steps) {


        log.info("预取下一个段号对象并缓存！key:{}", key);

        return executeLuaFromRedisWithRetry(key, maxSeq, steps)
                //拿到上限值
                .flatMap(maxValue -> {
                    //新的预存
                    SeqSegment newCache = new SeqSegment(maxValue - steps, maxValue);
                    //计算起始值 因为redis脚本一直是递增的并保证单线程执行脚本 不存在撞区间的情况
                    SEQ_CACHE.put(PRE_FETCH_SEQ_PREFIX + key, newCache);
                    return Mono.just(newCache);
                });
    }

    /**
     * 带重试机制的 执行Lua 脚本 获取段号
     *
     * @param key    业务key
     * @param maxSeq 最大值
     * @param steps  步长 区间
     * @return Mono<Long> 区间上限值
     */
    private Mono<Long> executeLuaFromRedisWithRetry(String key, String maxSeq, Integer steps) {
        //脚本参数 listParam
        List<String> listParam = new ArrayList<>();
        listParam.add(maxSeq);
        listParam.add(String.valueOf(steps));

        log.info("调用redis服务执行lua脚本获取段 缓存区间！key:{}，steps:{}", key, steps);

        return reactiveRedisClient.execute(
                        properties.getIdGenerator().getLuaScript(),
                        Collections.singletonList(DEFAULT_KEY_PREFIX + key),
                        Long.class,
                        new LongRedisSerializer(),
                        listParam
                )
                .single()
                .cache()
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        // 最大退避时间
                        .maxBackoff(Duration.ofSeconds(1))
                        // 添加抖动，避免惊群效应
                        .jitter(0.5)
                        .filter(throwable ->
                                // 只对特定异常重试
                                throwable instanceof RedisConnectionException ||
                                        throwable instanceof RedisCommandTimeoutException ||
                                        throwable instanceof RedisCommandExecutionException
                        )
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            // 重试耗尽时的自定义异常
                            return new BlinkException(retrySignal.failure(),
                                    "Redis操作失败，重试" + retrySignal.totalRetries() + "次后仍失败"
                            );
                        })
                        .doBeforeRetry(retrySignal ->
                                log.warn("Redis操作重试: 第{}次, 异常: {}",
                                        retrySignal.totalRetries() + 1,
                                        retrySignal.failure().getClass().getSimpleName())
                        )
                );
    }

    /**
     * 判断是否要预存 获取新的seq
     *
     * @param seqSegment 缓存对象
     * @return
     */
    private boolean shouldPrefetch(SeqSegment seqSegment, String key, Integer step) {
        double useRate = seqSegment.usageRate(step);
        //百分比
        double percent = properties.getIdGenerator().getSeqParam().get(key).getFetchPercent();
        //使用数量超阈值并且没有预取对象存在缓存中
        return useRate >= percent && !SEQ_CACHE.containsKey(PRE_FETCH_SEQ_PREFIX + key);
    }

    private static class SeqSegment {
        // 原子变量
        private final AtomicLong current;
        // 号段最大边界
        private final long max;

        public SeqSegment(long start, long max) {
            this.current = new AtomicLong(start);
            this.max = max;
        }

        public long getNextId() {
            // 1. 原子递增并获取旧值 (底层是 CPU 原子指令)
            // getAndIncrement 返回的未加一前的值  incrementAndGet()返回的是加一后的值 注意区别
            long val = current.incrementAndGet();
            // 2. 边界检查：只有在当前值未超过最大值时才认为有效
            if (val <= max) {
                return val;
            }
            // 3. 超过号段，返回错误标识，触发外部的切换逻辑
            return -1;
        }

        // 计算剩余百分比，用于触发预取counter
        public double usageRate(long step) {
            //这里读取不上锁 即使读取时有可能并发写 误差影响不大
            return (double) (step - (max - current.get())) / step;
        }
    }

    private static class AtomicStatus {
        //正在预取 true正在预取或者已经预取没消费 false未预取或者已消费
        final AtomicBoolean prefetching = new AtomicBoolean(false);
        //无锁化 核心：存放正在进行的加载任务。如果为 null，说明当前没有线程在加载。
        final AtomicReference<CompletableFuture<SeqSegment>> loadingFuture = new AtomicReference<>(null);
    }

}
