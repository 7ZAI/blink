package com.blink.framework.redis.id;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.factory.BlinkNamedThreadFactory;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 顺序号生成器 同步方案
 *
 * @author binblink
 */
@Slf4j
public class SeqGenerator {


    /**
     * 本地缓存对象存储容器
     */
    private static final Map<String, SeqSegment> SEQ_CACHE_MAP = new ConcurrentHashMap<>();

    private final RedisClient redisClient;

    private final BlinkRedisProperties properties;
    // 预取得到 缓存对象key前缀 实际key 为 前缀+缓存key
    private final String PRE_FETCH_SEQ_CACHE_KEY_PREFIX = "prefetch_seq_cache";

    //执行异步预存的线程池
    private final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1, new BlinkNamedThreadFactory
            .Builder("seq-prefetch").build());
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 初始延迟 100ms
    private static final long INITIAL_BACKOFF = 100;

    public SeqGenerator(RedisClient redisClient, BlinkRedisProperties properties) {
        this.redisClient = redisClient;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        Map<String, Integer> map = properties.getIdGenerator().getKeySteps();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            SEQ_CACHE_MAP.put(key, new SeqSegment());

        }
    }

    /**
     * 生成序列号
     *
     * @param key      redis中的key值
     * @param maxValue 最大值
     * @return 数字
     */
    public Long generateSeq(String key, Long maxValue) {

        if (StrUtil.isBlank(key)) {
            log.error("generateSeq key is blank");
            throw new RuntimeException("key 不能为空");
        }
        Integer steps = this.properties.getIdGenerator().getKeySteps(key);

        while (true) {
            SeqSegment seqSegment = SEQ_CACHE_MAP.get(key);
            long seq, max;
            //可重入读锁 在写锁发生时阻塞
            seqSegment.readWriteLock.readLock().lock();
            try {
                seq = seqSegment.counter.incrementAndGet();
                max = seqSegment.max.get();
            } finally {
                seqSegment.readWriteLock.readLock().unlock();
            }
            //缓存充足
            if (seq <= max) {
                // 达到阈值 异步请求redis获取段号
                if (shouldPrefetch(seqSegment, steps)) {
                    //CAS 竞争 异步预取 只有一个线程能成功
                    if (seqSegment.prefetching.compareAndSet(false, true)) {
                        log.debug(" 顺序号本地缓存消费达到阈值 执行异步任务获取顺序号 预存！");
                        asyncGetSeqFromRedis(seqSegment, key, maxValue, steps);
                    }
                }
                //返回值
                return seq;
            }
            //号段用尽了 缓存为空，准备发起刷新
            CompletableFuture<SeqSegment> newFuture = new CompletableFuture<>();
            //  CAS 竞争：尝试成为 Leader 只有一个线程能成功
            if (seqSegment.loadingFuture.compareAndSet(null, newFuture)) {
                log.info("抢占成功 准备发起远程调用刷新本地缓存");
                try {
                    //防止允许刷新瞬间 立马有线程抢到执行权（概率非常低）  双重检查 竞争线程获得执行权 但是第一个线程已经刷新计数器了 防止重复进行远程调用 覆盖未使用的缓存seq
                    long doubleCheckVal = seqSegment.counter.incrementAndGet();
                    if (doubleCheckVal < seqSegment.max.get()) {
                        //首次成功的线程已经刷新缓存
                        log.debug("非首次竞争成功的线程，从缓存对象中获取顺序号");
                        // 告知所有等待者：任务完成！
                        newFuture.complete(seqSegment);
                        return doubleCheckVal;
                    }
                    getSeqFromRedis(seqSegment, key, maxValue, steps);
                    SEQ_CACHE_MAP.put(key, seqSegment);
                    // 告知所有等待者：任务完成！
                    newFuture.complete(seqSegment);
                } catch (Exception e) {
                    // 异常处理
                    newFuture.completeExceptionally(e);
                } finally {
                    //清除任务 重新允许刷新
                    seqSegment.loadingFuture.set(null);
                }
                //这里结束之后 跳while循环获取值
            } else {
                // Follwer 线程 CAS失败的线程 说明已经有人在跑了，获取那个正在跑的 Future
                CompletableFuture<SeqSegment> currentFuture = seqSegment.loadingFuture.get();
                if (currentFuture != null) {
                    try {
                        // 阻塞等待结果（这里利用了 Future 的等待机制，比锁更轻量）
                        // 注意：这里我们不需要返回值，只需要等它结束
                        currentFuture.join();
                    } catch (Exception e) {
                        // 如果 Leader 挂了，这里会抛出异常，忽略并重试
                        log.error("执行缓存刷新的CompletableFuture 未完成！", e);
                    }
                }
            }
            //这里结束之后 代表已刷新缓存seq 跳while循环获取值
        }
    }


    /**
     * 从 redis获取段号 远程调用
     *
     * @param key      key
     * @param maxValue 最大值
     * @param step     步进值 即增量值 本地缓存数量
     * @return
     */
    private SeqSegment getSeqFromRedis(SeqSegment seqSegment, String key, Long maxValue, Integer step) {

        String preCacheKey = PRE_FETCH_SEQ_CACHE_KEY_PREFIX + key;
        //如果预取完成了拿预取的对象刷新
        if (SEQ_CACHE_MAP.containsKey(preCacheKey)) {
            SeqSegment preFetchCache = SEQ_CACHE_MAP.get(preCacheKey);

            seqSegment.readWriteLock.writeLock().lock();
            try {
                seqSegment.counter.set(preFetchCache.counter.get());
                seqSegment.max.set(preFetchCache.max.get());
                SEQ_CACHE_MAP.remove(preCacheKey);
                log.info("完成预存的缓存对象刷新到本地顺序号缓存中！new start:{},max:{}", seqSegment.counter.get(), preFetchCache.max.get());
                //放开preFetch限制
                seqSegment.prefetching.set(false);
            } finally {
                seqSegment.readWriteLock.writeLock().unlock();
            }
            return seqSegment;
        }


        log.info("初始直接获取 Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);

        long seq = executeLuaFromRedisWithRetry(key, maxValue, step);
        long start = seq - step;

        seqSegment.readWriteLock.writeLock().lock();
        try {
            seqSegment.max.set(seq);
            seqSegment.counter.set(start);
        } finally {
            seqSegment.readWriteLock.writeLock().unlock();
        }


        log.info("Redis生成的序列号 起始值 seq: \"{}\" ", start);
        return seqSegment;
    }


    /**
     * 调用 redis 执行lua脚本获取id下一个段号
     *
     * @param key
     * @param maxValue
     * @param step
     * @return
     */
    private Long executeLuaFromRedis(String key, Long maxValue, Integer step) {

        log.info("使用Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);

        List<String> keys = new ArrayList<>();
        keys.add(key);
        RedisScript<Long> redisScript = RedisScript.of(this.properties.getIdGenerator().getLuaScript(), Long.class);
        return redisClient.execute(redisScript, new LongRedisSerializer(), keys, String.valueOf(maxValue), String.valueOf(step));
    }

    /**
     * 带重试机制的调用
     *
     * @param key
     * @param maxValue
     * @param step
     * @return
     */
    private Long executeLuaFromRedisWithRetry(String key, Long maxValue, Integer step) {
        int attempt = 0;
        while (true) {
            try {
                return executeLuaFromRedis(key, maxValue, step);
            } catch (Exception e) {
                attempt++;
                if (attempt > MAX_RETRIES) {
                    log.error("Redis生成序列号重试{}次后仍然失败, key: {}", MAX_RETRIES, key);
                    // 超过重试次数，抛出异常
                    throw e;
                }

                // 指数退避计算：100ms, 200ms, 400ms...
                long backoff = INITIAL_BACKOFF * (long) Math.pow(2, attempt - 1);
                log.warn("Redis执行失败，正在进行第 {} 次重试，等待 {}ms. Key: {}", attempt, backoff, key);

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }
    }

    /**
     * 判断是否要预存 获取新的seq
     *
     * @param seqSegment 缓存对象
     * @return
     */
    private boolean shouldPrefetch(SeqSegment seqSegment, Integer step) {
        double useRate = seqSegment.usageRate(step);
        //百分比
        double percent = properties.getIdGenerator().getFetchPercent();
        //使用数量超阈值并且没有预取对象存在缓存中
        return useRate >= percent && !seqSegment.prefetching.get();
    }

    /**
     * 异步调用 预存seq
     *
     * @param key      redis key
     * @param maxValue 最大值
     * @param step     步长
     */
    private void asyncGetSeqFromRedis(SeqSegment seqSegment, String key, Long maxValue, Integer step) {

        CompletableFuture.supplyAsync(() -> {
                    log.info("prefetching 执行 lua脚本预取 序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);
                    long seq = executeLuaFromRedisWithRetry(key, maxValue, step);
                    SeqSegment preFetch = new SeqSegment();
                    preFetch.max.set(seq);
                    preFetch.counter.set(seq - step);
                    return preFetch;

                }, fixedThreadPool)
                .exceptionally(ex -> {
                    // exceptionally: 捕获异常，并返回一个默认值替代（类似 try-catch，返回兜底值）
                    log.error("prefetching fail! 预取 seq 失败！错误信息：{}", ex.getMessage());
                    seqSegment.prefetching.set(false);
                    return null;
                }).thenAccept(seqCache -> {
                    String prefetchKey = PRE_FETCH_SEQ_CACHE_KEY_PREFIX + key;
                    SEQ_CACHE_MAP.put(prefetchKey, seqCache);
                });

    }

    /**
     * 本地缓存类
     */
    public static class SeqSegment {
        // 起始值
        private final AtomicLong counter = new AtomicLong(0);
        // 最大值
        private final AtomicLong max = new AtomicLong(0);

        //正在预取 true 正在
        private final AtomicBoolean prefetching = new AtomicBoolean(false);
        //无锁化 核心：存放正在进行的加载任务。如果为 null，说明当前没有线程在加载。
        private final AtomicReference<CompletableFuture<SeqSegment>> loadingFuture = new AtomicReference<>(null);

        //读写锁保证最低限度竞争 只在
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        // 计算剩余百分比，用于触发预取counter 读写时上锁
        public double usageRate(long step) {
            return (double) (step - (max.get() - counter.get())) / step;
        }


    }

}
