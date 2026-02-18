package com.blink.gateway.cache;

import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.constant.RedisConstans;
import com.blink.gateway.service.BaseAppService;
import com.blink.gateway.util.ReactiveCacheUtil;
import com.github.benmanes.caffeine.cache.AsyncCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 网关缓存数据预热
 *
 * @author binblink
 */
//@Component
@Slf4j
public class CachePreHeating {

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private CacheManager localCacheManager;

    @Resource
    private BaseAppService baseAppService;

    @Value("${blink.gateway.localCacheEnable:false}")
    private Boolean localCacheEnable;

    @PostConstruct
    public void init() {

        //开启本地缓存
        if (localCacheEnable) {
            //获取所有权限管理的url 缓存url和权限标识的映射关系
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
                    .onErrorResume(e -> {
                        log.error("预缓存接口权限失败{}", e.getMessage(), e);
                        return Mono.empty();
                    })
                    .doOnComplete(() -> log.info("接口权限标识缓存加载完毕！缓存名称：{}", GatewayConstant.CONSISTENT_CACHE))
                    .subscribe();
        }
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

        return baseAppService.getAllApiPermissions()
                .flux()
                .map(GetAllApiPermissionsRsp::getPermissionList)
                .flatMap(list ->
                        Flux.fromIterable(list)
                                .map(perm -> new AbstractMap.SimpleEntry<>(RedisConstans.URL_PERMISSION + perm.getUrl(), perm.getAcIdentity()))
                );
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
        return keyFlux.flatMap(key ->
                redisClient.getTemplate().opsForValue().get(key).map(value -> new AbstractMap.SimpleEntry<>(key, value.toString()))
        ).onErrorResume(e -> {
            log.error("请求redis 执行scan 失败");
            return Mono.empty();
        });
    }
}
