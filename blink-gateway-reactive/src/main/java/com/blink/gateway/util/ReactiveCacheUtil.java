package com.blink.gateway.util;

import com.blink.framework.common.exception.BlinkException;
import com.github.benmanes.caffeine.cache.AsyncCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cloud.gateway.support.TimeoutException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 本地缓存转换为成流式调用
 *
 * @author binblink
 */
@Slf4j
public class ReactiveCacheUtil {

    /**
     * 从 AsyncCache 中获取 Mono 数据，如果缓存未命中，则通过 loader 加载。
     * 此方法确保了加载逻辑在独立的调度器上执行，不会阻塞。
     */
    public static <K, V> Mono<V> getMono(AsyncCache<K, V> cache, K key, Supplier<Mono<V>> loader) {

        // get 方法会立即返回 CompletableFuture。如果缓存未命中，loader 会被异步执行。
        CompletableFuture<V> future = cache.get(key, (k, executor) -> {
            // 这里的 loader.get() 返回的是 Mono，我们需要将其转换为 CompletableFuture
            return loader.get()
                    // 强制让 Mono 的订阅发生在指定的 Scheduler 上，确保数据加载的线程安全
                    //这里和配置上的executor并不冲突 这里会使用Scheduler线程
                    .subscribeOn(Schedulers.boundedElastic())
                    // 【关键】Mono 转 CompletableFuture
                    .toFuture();
        });

        // 将 CompletableFuture 转换回 Mono，无缝衔接反应式流
        return Mono.fromFuture(future)
                // 当缓存中的 Future 因异常完成时，我们需要将异常也传递到反应式流中
                .onErrorMap(e -> new BlinkException(e,"Failed to load from cache"));
    }

    /**
     * 增强版 getMono，支持缓存命中/未命中的监控 支持重试
     */
    public static <K, V> Mono<V> getMono(
            AsyncCache<K, V> cache,
            K key,
            Supplier<Mono<V>> loader,
            Runnable onHit,
            Runnable onMiss) {

        CompletableFuture<V> future = cache.get(key, (k, executor) -> {
            onMiss.run();  // 记录缓存未命中

            return loader.get()
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnError(e -> log.error("Failed to load {}: {}", key, e.getMessage()))
                    .timeout(Duration.ofSeconds(5),
                            Mono.error(new TimeoutException("Cache load timeout for: " + key)))
                    .retry(2)  // 失败重试 2 次
                    .toFuture();
        });

        return Mono.fromFuture(future)
                // 记录缓存命中或加载成功
                .doOnNext(v -> onHit.run())
                .onErrorMap(e -> new BlinkException(e,"Failed to load: " + key));
    }

    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    public static <K, V> AsyncCache<K, V> toAsyncCache(Cache cache) {
        if (!(cache instanceof AsyncCache)) {
            return null;
        }
        return (AsyncCache<K, V>) cache;
    }


    private void logCacheHit(String id) {
        log.debug("Cache hit : {}", id);
    }

    private void logCacheMiss(String id) {
        log.debug("Cache miss : {}, loading from DB", id);
    }
}