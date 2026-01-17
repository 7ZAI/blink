package com.blink.framework.redis.id;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.factory.BlinkNamedThreadFactory;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
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


/**
 * 顺序号生成器 同步方案
 * 能在分布式模式下保证 全局唯一性
 *
 * @author binblink
 */
@Slf4j
public class SeqGenerator {


    /**
     * seq本地缓存对象存储容器
     */
    private static final Map<String, SeqSegment> SEQ_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, AtomicStatus> STATUS_MAP = new ConcurrentHashMap<>();

    /**
     * seq预取缓存对象key前缀 完整为前缀+key
     */
    private static final String PRE_FETCH_SEQ_PREFIX = "seq:prefetch:";

    private static final String DEFAULT_KEY_PREFIX = "seq:";

    private final RedisClient redisClient;

    private final BlinkRedisProperties properties;
    //执行异步预存的线程池
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(new BlinkNamedThreadFactory
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

        Map<String, BlinkRedisProperties.IdGenerator.SeqParam> map = properties.getIdGenerator().getSeqParam();

        for (Map.Entry<String, BlinkRedisProperties.IdGenerator.SeqParam> entry : map.entrySet()) {
            String key = entry.getKey();
            SEQ_CACHE.put(key, new SeqSegment(0, 0));
            STATUS_MAP.put(key, new AtomicStatus());
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

        //未配置的key
        if(!SEQ_CACHE.containsKey(key)) {
            log.error("当前key：{} 未被配置！请先配置",key);
            throw new RuntimeException("key为配置");
        }

        //未配置默认为1000
        final Integer steps = this.properties.getIdGenerator().getkeySteps(key);



        while (true) {
            SeqSegment seqSegment = SEQ_CACHE.get(key);
            AtomicStatus status = STATUS_MAP.get(key);
            long nextSeq = seqSegment.getNextSeq();
            //缓存充足
            if (nextSeq != -1) {
                // 达到阈值 异步请求redis获取段号
                if (shouldPrefetch(seqSegment, key, steps)) {
                    //CAS 竞争 异步预取 只有一个线程能成功
                    if (status.prefetching.compareAndSet(false, true)) {
                        log.debug(" 顺序号本地缓存消费达到阈值 执行异步任务获取顺序号 预存！当前couter:{},max:{}", seqSegment.current.get(), seqSegment.max);
                        asyncGetSeqFromRedis(seqSegment, status, key, maxValue, steps);
                    }
                }
                //返回值
                return nextSeq;
            }
            //号段用尽了 缓存为空，准备发起刷新
            CompletableFuture<SeqSegment> newFuture = new CompletableFuture<>();
            //  CAS 竞争：尝试成为 Leader 只有一个线程能成功
            if (status.loadingFuture.compareAndSet(null, newFuture)) {
                log.info("抢占成功 准备发起远程调用刷新本地缓存");
                try {
                    //防止允许刷新瞬间 立马有线程抢到执行权（概率非常低）
                    // 双重检查 竞争线程获得执行权 但是第一个线程已经刷新计数器了 防止重复进行远程调用 覆盖未使用的缓存seq
                    long doubleCheckVal = seqSegment.getNextSeq();
                    if (doubleCheckVal != -1) {
                        //首次成功的线程已经刷新缓存
                        log.debug("非首次竞争成功的线程，从缓存对象中获取顺序号");
                        // 告知所有等待者：任务完成！
                        newFuture.complete(seqSegment);
                        return doubleCheckVal;
                    }
                    //刷新
                    seqSegment = getSeqFromRedis(status, key, maxValue, steps);
                    SEQ_CACHE.put(key, seqSegment);
                    // 告知所有等待者：任务完成！
                    newFuture.complete(seqSegment);
                } catch (Exception e) {
                    // 异常处理
                    log.error("执行刷新Seq缓存的CompletableFuture 失败！", e);
                    newFuture.completeExceptionally(e);
                } finally {
                    //清除刷新任务 重新允许刷新
                    status.loadingFuture.set(null);
                }
                //这里结束之后 跳while循环获取值
            } else {
                // Follwer 线程 CAS失败的线程 说明已经有人在跑了，获取那个正在跑的 Future
                CompletableFuture<SeqSegment> currentFuture = status.loadingFuture.get();
                if (currentFuture != null) {
                    try {
                        // 阻塞等待结果（这里利用了 Future 的等待机制，比锁更轻量）
                        // 注意：这里我们不需要返回值，只需要等它结束 因为一得到值 会进入while 重新取值
                        currentFuture.join();
                    } catch (Exception e) {
                        // 如果 Leader 挂了，这里会抛出异常，忽略并重试
                        log.error("执行缓存刷新的CompletableFuture 未完成！", e);
                    }
                }
            }
            //执行到这里结束之后 代表已刷新缓存seq while循环获取值
        }
    }


    /**
     * 从 redis获取段号 远程调用
     *
     * @param key      key
     * @param maxValue 最大值
     * @param step     步进值 即增量值 本地缓存数量
     */
    private SeqSegment getSeqFromRedis(AtomicStatus status, String key, Long maxValue, Integer step) {
        String preKey = PRE_FETCH_SEQ_PREFIX + key;
        //如果预取完成了拿预取的对象刷新
        if (SEQ_CACHE.containsKey(preKey)) {
            SeqSegment newSeqSegment = SEQ_CACHE.get(preKey);
            SEQ_CACHE.remove(preKey);
            log.info("完成预存的缓存对象刷新到本地顺序号缓存！new start:{},max:{}", newSeqSegment.current.get(), newSeqSegment.max);
            //放开preFetch限制
            status.prefetching.set(false);

            return newSeqSegment;
        }

        log.info("非预取 执行 Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);

        long seq = executeLuaFromRedisWithRetry(key, maxValue, step);
        long start = seq - step + 1;

        SeqSegment seqSegment = new SeqSegment(start, seq);

        log.info("已刷新段号缓存 seqSegment  counter: \"{}\"，max:\"{}\"", start, seq);

        return seqSegment;
    }


    /**
     * 调用 redis 执行lua脚本获取id下一个段号
     *
     * @param key      redis key
     * @param maxValue 该key全局最大值
     * @param step     步长
     * @return Long 缓存最大值
     */
    private Long executeLuaFromRedis(String key, Long maxValue, Integer step) {

        log.info("使用Redis lua脚本生成序列号 with key: \"{}\" maxValue: \"{}\" step: \"{}\".", key, maxValue, step);

        List<String> keys = new ArrayList<>();
        keys.add(DEFAULT_KEY_PREFIX + key);
        RedisScript<Long> redisScript = RedisScript.of(this.properties.getIdGenerator().getLuaScript(), Long.class);
        return redisClient.execute(redisScript, new LongRedisSerializer(), keys, String.valueOf(maxValue), String.valueOf(step));
    }

    /**
     * 带重试机制的调用
     *
     * @param key      redis key
     * @param maxValue 该key全局最大值
     * @param step     步长
     * @return Long 缓存最大值
     */
    private Long executeLuaFromRedisWithRetry(String key, Long maxValue, Integer step) {
        int attempt = 0;
        while (true) {
            try {
                return executeLuaFromRedis(key, maxValue, step);
            } catch (Exception e) {
                // 只对特定异常重试
                if (e instanceof RedisConnectionException || e instanceof RedisCommandTimeoutException || e instanceof RedisCommandExecutionException) {
                    attempt++;
                    if (attempt > MAX_RETRIES) {
                        log.error("Redis生成序列号重试{}次后仍然失败, key: {}", MAX_RETRIES, key);
                        // 超过重试次数，抛出异常
                        throw e;
                        //TODO 生产环境  抛自定义异常 全局获取后 通知维护人员 最高等级警告
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

    /**
     * 异步调用 预存seq
     *
     * @param key      redis key
     * @param maxValue 最大值
     * @param step     步长
     */
    private void asyncGetSeqFromRedis(SeqSegment seqSegment, AtomicStatus status, String key, Long maxValue, Integer step) {

        CompletableFuture.supplyAsync(() -> {
                    log.info("prefetching 执行 lua脚本预取 序列号 with key: \"{}\" counter:{} maxValue: \"{}\" step: \"{}\".",
                            key, seqSegment.current.get(), maxValue, step);
                    return executeLuaFromRedisWithRetry(key, maxValue, step);
                }, singleThreadExecutor)
                .exceptionally(ex -> {
                    // exceptionally: 捕获异常，并返回一个默认值替代（类似 try-catch，返回兜底值）
                    log.error("prefetching fail! 预取 seq 失败！错误信息：{}", ex.getMessage());
                    status.prefetching.set(false);
                    return null;
                }).thenAccept(nextSeq -> {
                    SeqSegment preFetch = new SeqSegment(nextSeq - step + 1, nextSeq);
                    SEQ_CACHE.put(PRE_FETCH_SEQ_PREFIX + key, preFetch);
                    log.info("预取成功并保存 prefetching success! key:{},counter:{},max:{}", key, nextSeq - step, nextSeq);
                });
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

        public long getNextSeq() {
            // 1. 原子递增并获取旧值 (底层是 CPU 原子指令)
            long val = current.getAndIncrement();

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
