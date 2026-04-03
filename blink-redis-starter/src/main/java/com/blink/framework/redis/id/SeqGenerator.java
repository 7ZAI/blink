package com.blink.framework.redis.id;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.factory.BlinkNamedThreadFactory;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.config.prop.BlinkRedisProperties;
import com.blink.framework.redis.serializer.LongRedisSerializer;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Distributed sequence number generator using the segment allocation pattern.
 * <p>
 * This generator ensures global uniqueness in distributed environments by allocating
 * segments of sequence numbers from Redis. Each service instance maintains a local
 * cache of sequence numbers and asynchronously prefetches new segments when the
 * current segment is running low.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Segment-based allocation for high performance</li>
 *   <li>Asynchronous prefetching to minimize latency</li>
 *   <li>Lock-free design using CAS operations</li>
 *   <li>Automatic retry with exponential backoff</li>
 *   <li>Graceful shutdown support</li>
 * </ul>
 *
 * @author binblink
 * @see BlinkRedisProperties
 */
@Slf4j
public class SeqGenerator {

    /**
     * Local cache storage for sequence segments.
     */
    private static final Map<String, SeqSegment> SEQ_CACHE = new ConcurrentHashMap<>();

    /**
     * Status map for tracking loading and prefetching states.
     */
    private static final Map<String, AtomicStatus> STATUS_MAP = new ConcurrentHashMap<>();

    /**
     * Prefix for prefetch cache keys.
     */
    private static final String PRE_FETCH_SEQ_PREFIX = "seq:prefetch:";

    /**
     * Default prefix for Redis keys.
     */
    private static final String DEFAULT_KEY_PREFIX = "seq:";

    private final RedisClient redisClient;

    private final BlinkRedisProperties properties;

    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(
            new BlinkNamedThreadFactory.Builder("seq-prefetch").build());

    private static final int MAX_RETRIES = 3;

    private static final long INITIAL_BACKOFF = 100;

    /**
     * Constructs a SeqGenerator with the specified Redis client and properties.
     *
     * @param redisClient the Redis client for sequence generation
     * @param properties  the configuration properties
     */
    public SeqGenerator(RedisClient redisClient, BlinkRedisProperties properties) {
        this.redisClient = redisClient;
        this.properties = properties;
    }

    /**
     * Initializes the sequence generator by pre-allocating cache entries for configured keys.
     */
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
     * Gracefully shuts down the executor service.
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down SeqGenerator executor service...");
        singleThreadExecutor.shutdown();
        try {
            if (!singleThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate in time, forcing shutdown...");
                singleThreadExecutor.shutdownNow();
                if (!singleThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            log.error("Executor service shutdown interrupted", e);
            singleThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SeqGenerator executor service shutdown completed");
    }

    /**
     * Generates the next sequence number for the given key.
     * <p>
     * This method uses a lock-free design with CAS operations to ensure thread safety.
     * When the local segment is exhausted, it triggers a remote call to Redis to fetch
     * a new segment. Multiple threads can safely call this method concurrently.
     * </p>
     *
     * @param key      the Redis key for the sequence
     * @param maxValue the maximum allowed value for this sequence
     * @return the next sequence number
     * @throws RuntimeException    if the key is blank or not configured
     * @throws BlinkException      if segment refresh fails after maximum retries
     */
    public Long generateSeq(String key, Long maxValue) {
        if (StrUtil.isBlank(key)) {
            log.error("generateSeq key is blank");
            throw new BlinkException(BlinkErrorCodeEnum.ILLEGAL_PARAMETER.getCode());
        }

        if (!SEQ_CACHE.containsKey(key)) {
            log.error("Key: {} is not configured, please configure it first", key);
            throw new BlinkException(BlinkErrorCodeEnum.SEQ_KEY_NOT_CONFIG.getCode());
        }

        final Integer steps = this.properties.getIdGenerator().getkeySteps(key);

        final int MAX_RETRY = 3;
        int retryCount = 0;

        while (true) {
            SeqSegment seqSegment = SEQ_CACHE.get(key);
            AtomicStatus status = STATUS_MAP.get(key);
            long nextSeq = seqSegment.getNextSeq();

            if (nextSeq != -1) {
                if (shouldPrefetch(seqSegment, key, steps)) {
                    if (status.prefetching.compareAndSet(false, true)) {
                        log.debug("Local sequence cache reached threshold, triggering async prefetch. Current: {}, max: {}",
                                seqSegment.current.get(), seqSegment.max);
                        asyncGetSeqFromRedis(seqSegment, status, key, maxValue, steps);
                    }
                }
                return nextSeq;
            }

            retryCount++;
            if (retryCount > MAX_RETRY) {
                throw new BlinkException("Segment refresh failed " + MAX_RETRY + " times consecutively, aborting request");
            }

            CompletableFuture<SeqSegment> newFuture = new CompletableFuture<>();

            if (status.loadingFuture.compareAndSet(null, newFuture)) {
                log.info("Successfully acquired lock, preparing to refresh local cache from remote");
                try {
                    SeqSegment latest = SEQ_CACHE.get(key);
                    if (latest != null) {
                        long doubleCheckVal = latest.getNextSeq();
                        if (doubleCheckVal != -1) {
                            log.debug("Non-first thread to acquire lock, getting sequence from cache");
                            newFuture.complete(seqSegment);
                            return doubleCheckVal;
                        }
                    }

                    seqSegment = getSeqFromRedis(status, key, maxValue, steps);
                    SEQ_CACHE.put(key, seqSegment);
                    newFuture.complete(seqSegment);
                } catch (Exception e) {
                    log.error("Failed to execute CompletableFuture for Seq cache refresh", e);
                    newFuture.completeExceptionally(e);
                } finally {
                    status.loadingFuture.set(null);
                }
            } else {
                CompletableFuture<SeqSegment> currentFuture = status.loadingFuture.get();
                if (currentFuture != null) {
                    try {
                        currentFuture.join();
                    } catch (Exception e) {
                        log.error("CompletableFuture for cache refresh did not complete", e);
                    }
                }
            }
        }
    }

    /**
     * Fetches a new sequence segment from Redis.
     * <p>
     * If a prefetched segment is available, it will be used directly.
     * Otherwise, a synchronous call to Redis is made to allocate a new segment.
     * </p>
     *
     * @param status   the atomic status for this key
     * @param key      the sequence key
     * @param maxValue the maximum allowed value
     * @param step     the segment size
     * @return a new sequence segment
     */
    private SeqSegment getSeqFromRedis(AtomicStatus status, String key, Long maxValue, Integer step) {
        String preKey = PRE_FETCH_SEQ_PREFIX + key;

        if (SEQ_CACHE.containsKey(preKey)) {
            SeqSegment newSeqSegment = SEQ_CACHE.get(preKey);
            SEQ_CACHE.remove(preKey);
            log.info("Prefetched segment promoted to local cache. New start: {}, max: {}",
                    newSeqSegment.current.get(), newSeqSegment.max);
            status.prefetching.set(false);
            return newSeqSegment;
        }

        log.info("Executing Redis Lua script to generate sequence. Key: {}, maxValue: {}, step: {}", key, maxValue, step);

        long seq = executeLuaFromRedisWithRetry(key, maxValue, step);
        long start = seq - step + 1;

        SeqSegment seqSegment = new SeqSegment(start, seq);

        log.info("Segment cache refreshed. Counter: {}, max: {}", start, seq);

        return seqSegment;
    }

    /**
     * Executes the Redis Lua script to allocate a new sequence segment.
     *
     * @param key      the Redis key
     * @param maxValue the maximum allowed value for this sequence
     * @param step     the segment size
     * @return the maximum value of the allocated segment
     */
    private Long executeLuaFromRedis(String key, Long maxValue, Integer step) {
        log.info("Using Redis Lua script to generate sequence. Key: {}, maxValue: {}, step: {}", key, maxValue, step);

        List<String> keys = new ArrayList<>();
        keys.add(DEFAULT_KEY_PREFIX + key);
        RedisScript<Long> redisScript = RedisScript.of(this.properties.getIdGenerator().getLuaScript(), Long.class);
        return redisClient.execute(redisScript, new LongRedisSerializer(), keys, String.valueOf(maxValue), String.valueOf(step));
    }

    /**
     * Executes the Redis Lua script with retry and exponential backoff.
     *
     * @param key      the Redis key
     * @param maxValue the maximum allowed value for this sequence
     * @param step     the segment size
     * @return the maximum value of the allocated segment
     */
    private Long executeLuaFromRedisWithRetry(String key, Long maxValue, Integer step) {
        int attempt = 0;
        while (true) {
            try {
                return executeLuaFromRedis(key, maxValue, step);
            } catch (Exception e) {
                if (e instanceof RedisConnectionException || e instanceof RedisCommandTimeoutException || e instanceof RedisCommandExecutionException) {
                    attempt++;
                    if (attempt > MAX_RETRIES) {
                        log.error("Redis sequence generation failed after {} retries. Key: {}", MAX_RETRIES, key);
                        throw e;
                    }

                    long backoff = INITIAL_BACKOFF * (long) Math.pow(2, attempt - 1);
                    log.warn("Redis execution failed, retry attempt {} with {}ms backoff. Key: {}", attempt, backoff, key);

                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlinkException(ie, BlinkErrorCodeEnum.SEQ_KEY_NOT_CONFIG.getCode());
                    }
                }
            }
        }
    }

    /**
     * Determines whether prefetching should be triggered.
     *
     * @param seqSegment the current segment
     * @param key        the sequence key
     * @param step       the segment size
     * @return true if prefetching should be triggered
     */
    private boolean shouldPrefetch(SeqSegment seqSegment, String key, Integer step) {
        double useRate = seqSegment.usageRate(step);
        double percent = properties.getIdGenerator().getSeqParam().get(key).getFetchPercent();
        return useRate >= percent && !SEQ_CACHE.containsKey(PRE_FETCH_SEQ_PREFIX + key);
    }

    /**
     * Asynchronously prefetches a new sequence segment.
     *
     * @param seqSegment the current segment
     * @param status     the atomic status for this key
     * @param key        the sequence key
     * @param maxValue   the maximum allowed value
     * @param step       the segment size
     */
    private void asyncGetSeqFromRedis(SeqSegment seqSegment, AtomicStatus status, String key, Long maxValue, Integer step) {
        CompletableFuture.supplyAsync(() -> {
                    log.info("Prefetching sequence via Lua script. Key: {}, counter: {}, maxValue: {}, step: {}",
                            key, seqSegment.current.get(), maxValue, step);
                    return executeLuaFromRedisWithRetry(key, maxValue, step);
                }, singleThreadExecutor)
                .exceptionally(ex -> {
                    log.error("Prefetch failed! Error: {}", ex.getMessage());
                    status.prefetching.set(false);
                    return null;
                }).thenAccept(nextSeq -> {
                    if (nextSeq != null) {
                        SeqSegment preFetch = new SeqSegment(nextSeq - step + 1, nextSeq);
                        SEQ_CACHE.put(PRE_FETCH_SEQ_PREFIX + key, preFetch);
                        log.info("Prefetch successful. Key: {}, counter: {}, max: {}", key, nextSeq - step, nextSeq);
                    }
                });
    }

    /**
     * Represents a segment of sequence numbers.
     */
    private static class SeqSegment {
        private final AtomicLong current;
        private final long max;

        /**
         * Constructs a segment with the specified start and max values.
         *
         * @param start the starting value (inclusive)
         * @param max   the maximum value (inclusive)
         */
        public SeqSegment(long start, long max) {
            this.current = new AtomicLong(start);
            this.max = max;
        }

        /**
         * Gets the next sequence number from this segment.
         *
         * @return the next sequence number, or -1 if the segment is exhausted
         */
        public long getNextSeq() {
            long val = current.getAndIncrement();

            if (val <= max) {
                return val;
            }
            return -1;
        }

        /**
         * Calculates the usage rate of this segment.
         *
         * @param step the total segment size
         * @return the usage rate as a percentage (0.0 to 1.0)
         */
        public double usageRate(long step) {
            return (double) (step - (max - current.get())) / step;
        }
    }

    /**
     * Holds the atomic status for a sequence key.
     */
    private static class AtomicStatus {
        final AtomicBoolean prefetching = new AtomicBoolean(false);
        final AtomicReference<CompletableFuture<SeqSegment>> loadingFuture = new AtomicReference<>(null);
    }
}
