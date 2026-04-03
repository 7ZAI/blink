package com.blink.gateway.cache;

import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.constant.RedisConstans;
import com.blink.gateway.service.BaseAppRemoteService;
import com.blink.gateway.util.ReactiveCacheUtil;
import com.github.benmanes.caffeine.cache.AsyncCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 网关缓存数据预热
 *
 * @author binblink
 */
@Component
@Slf4j
public class CachePreHeating {

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private CacheManager localCacheManager;

    @Resource
    private BaseAppRemoteService baseAppRemoteService;

    @Value("${blink.gateway.localCacheEnable:false}")
    private Boolean localCacheEnable;

    //之前使用@PostConstruct
    // 预热过程中访问了 Redis 和远程服务，这些操作可能触发了负载均衡器、服务发现客户端的初始化，
    // 而这些初始化过程在 WeightCalculatorWebFilter 中可能被同步等待，导致死锁或长时间阻塞。
    // WeightCalculatorWebFilter的里的路由刷新是block操作 造成卡死，
    // 改成所有bean初始化后 系统启动后执行
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // 外层 try-catch 作为最后保障，确保任何异常都不影响启动
        try {
            doInit();
        } catch (Exception e) {
            log.error("缓存预热初始化异常，不影响应用启动: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行缓存预热
     */
    private void doInit() {
        // 开启本地缓存
        if (!localCacheEnable) {
            return;
        }

        // 获取所有权限管理的url 缓存url和权限标识的映射关系
        Cache cache = localCacheManager.getCache(GatewayConstant.CONSISTENT_CACHE);
        AsyncCache<String, Object> asyncCache = ReactiveCacheUtil.toAsyncCache(cache);
        if (Objects.isNull(asyncCache)) {
            log.error("获取本地缓存失败！ 缓存名称：{}", GatewayConstant.CONSISTENT_CACHE);
            return;
        }

        String keyPrefix = RedisConstans.URL_PERMISSION;
        getKeyValueEntriesByPrefix(keyPrefix)
                .doOnNext(entry -> writeCache(asyncCache, entry))
                .switchIfEmpty(getAllApiPermissionByRpc()
                        .doOnNext(r -> writeCache(asyncCache, r)))
                // 添加超时机制，避免长时间阻塞
                .timeout(Duration.ofSeconds(20), Mono.empty())
                .onErrorResume(e -> {
                    log.error("预缓存接口权限失败: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .doOnComplete(() -> log.info("接口权限标识缓存加载完毕！缓存名称：{}", GatewayConstant.CONSISTENT_CACHE))
                .subscribe(
                        // 正常消费回调（空实现，逻辑已在 doOnNext 中处理）
                        data -> {},
                        // 错误回调 - 确保 subscribe 阶段的异常不影响启动
                        error -> log.error("缓存预热订阅失败，不影响应用启动: {}", error.getMessage(), error),
                        // 完成回调
                        () -> log.debug("缓存预热流程结束")
                );
    }

    private void writeCache(AsyncCache<String, Object> asyncCache, AbstractMap.SimpleEntry<String, String> entry) {
        if (entry.getKey() != null) {
            //已完成状态的future
            CompletableFuture<String> future = CompletableFuture.completedFuture(entry.getValue());
            asyncCache.put(entry.getKey(), future);
        }
    }

    /**
     * 远程服务获取
     */
    private Flux<AbstractMap.SimpleEntry<String, String>> getAllApiPermissionByRpc() {

        return baseAppRemoteService.getAllApiPermissions()
                .flux()
                .map(GetAllApiPermissionsRsp::getPermissionList)
                // 忽略 null 列表
                .filter(Objects::nonNull)
                .flatMap(list ->
                        Flux.fromIterable(list)
                                .map(perm -> new AbstractMap.SimpleEntry<>(RedisConstans.URL_PERMISSION + perm.getUrl(), perm.getAcIdentity()))
                ).onErrorResume(e -> {
                    log.error("RPC远程调用查询接口权限失败{}", e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * redis获取
     * 根据前缀获取所有匹配的 key-value，以 Flux<Map.Entry<String, String>> 形式返回
     */
    private Flux<AbstractMap.SimpleEntry<String, String>> getKeyValueEntriesByPrefix(String prefix) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(prefix + "*")
                // 每次扫描的近似数量
                .count(100)
                .build();
        // 扫描 key
        Flux<String> keyFlux = redisClient.getTemplate().scan(scanOptions);

        // 对每个 key 异步获取 value，组合成 Entry
        return keyFlux.flatMap(key -> redisClient.getTemplate().opsForValue().get(key).map(value -> new AbstractMap.SimpleEntry<>(key, value.toString()))
                        , 16).switchIfEmpty(Mono.empty())
                .onErrorResume(e -> {
                    log.error("请求redis 执行scan 失败");
                    //返回空 不抛异常 不影响启动 错误仅作日志处理
                    return Mono.empty();
                });
    }
}
