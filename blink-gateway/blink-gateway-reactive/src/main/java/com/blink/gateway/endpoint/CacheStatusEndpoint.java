package com.blink.gateway.endpoint;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.dto.CacheItemDTO;
import com.blink.gateway.dto.CacheStatusResponse;
import com.github.benmanes.caffeine.cache.AsyncCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.blink.gateway.constant.GatewayConstant.*;
import static com.blink.gateway.constant.RedisConstans.*;

/**
 * 缓存状态 Actuator 端点
 * 用于一致性检查，返回本地 Caffeine 缓存的 key 和 checksum
 *
 * @author binblink
 */
@Endpoint(id = "cache-status")
@Component
@Slf4j
public class CacheStatusEndpoint {

    @Resource
    private CacheManager localCacheManager;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 缓存类型与缓存名称的映射
     */
    private static final Map<String, String> CACHE_TYPE_MAPPING = Map.of(
            "channel", CONSISTENT_CACHE,
            "config", CONSISTENT_CACHE,
            "route", CONSISTENT_CACHE
    );

    /**
     * 缓存类型与 Redis key 前缀的映射（用于从本地缓存 key 中提取业务 key）
     */
    private static final Map<String, String> CACHE_KEY_PREFIX_MAPPING = Map.of(
            "channel", BLINK_CHANNEL_PREFIX,
            "config", GATEWAY_CONFIG_KEY_PREFIX,
            "route", GATEWAY_DYNAMIC_ROUTES
    );

    /**
     * 获取指定类型的本地缓存状态
     *
     * @param type 缓存类型: channel / route / config
     * @return 缓存状态响应
     */
    @ReadOperation
    public CacheStatusResponse cacheStatus(@Selector String type) {
        String instanceId = getInstanceId();

        if (StrUtil.isBlank(type)) {
            return buildEmptyResponse(instanceId, type);
        }

        String cacheName = CACHE_TYPE_MAPPING.get(type.toLowerCase());
        if (cacheName == null) {
            return buildEmptyResponse(instanceId, type);
        }

        try {
            Cache localCache = localCacheManager.getCache(cacheName);
            if (localCache == null) {
                log.warn("[CacheStatusEndpoint] 本地缓存未启用 | cacheName: {}", cacheName);
                return buildEmptyResponse(instanceId, type);
            }

            List<CacheItemDTO> items = getLocalCacheItems(localCache, type.toLowerCase());
            return CacheStatusResponse.builder()
                    .instanceId(instanceId)
                    .type(type.toLowerCase())
                    .timestamp(LocalDateTime.now())
                    .items(items)
                    .build();
        } catch (Exception e) {
            log.error("[CacheStatusEndpoint] 获取本地缓存状态失败 | type: {}, error: {}", type, e.getMessage(), e);
            return buildEmptyResponse(instanceId, type);
        }
    }

    /**
     * 从本地 Caffeine 缓存获取所有缓存项
     *
     * @param localCache 本地缓存对象
     * @param type       缓存类型
     * @return 缓存项列表
     */
    private List<CacheItemDTO> getLocalCacheItems(Cache localCache, String type) {
        List<CacheItemDTO> items = new ArrayList<>();

        // 获取 Caffeine AsyncCache
        AsyncCache<Object, Object> asyncCache = (AsyncCache<Object, Object>) localCache.getNativeCache();
        com.github.benmanes.caffeine.cache.Cache<Object, Object> synchronousCache = asyncCache.synchronous();

        // 获取缓存 key 前缀
        String keyPrefix = CACHE_KEY_PREFIX_MAPPING.get(type);

        // 遍历本地缓存
        Map<Object, Object> cacheMap = synchronousCache.asMap();
        if (CollUtil.isEmpty(cacheMap)) {
            return items;
        }

        for (Map.Entry<Object, Object> entry : cacheMap.entrySet()) {
            String key = String.valueOf(entry.getKey());

            // 过滤：只返回匹配当前类型的缓存项
            if (StrUtil.isNotBlank(keyPrefix) && !key.startsWith(keyPrefix)) {
                continue;
            }

            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            // 计算 checksum
            String checksum = calculateChecksum(value);

            // 提取业务 key（去掉 Redis key 前缀）
            String businessKey = extractBusinessKey(key, type);

            items.add(CacheItemDTO.builder()
                    .key(businessKey)
                    .checksum(checksum)
                    .updateTime(LocalDateTime.now())
                    .build());
        }

        log.info("[CacheStatusEndpoint] 获取本地缓存项 | type: {}, count: {}", type, items.size());
        return items;
    }

    /**
     * 计算对象 checksum
     *
     * @param value 缓存值
     * @return MD5 checksum
     */
    private String calculateChecksum(Object value) {
        try {
            String json = JacksonUtil.toJson(value);
            return DigestUtil.md5Hex(json);
        } catch (Exception e) {
            log.error("[CacheStatusEndpoint] 计算 checksum 失败 | error: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从缓存 key 中提取业务 key
     *
     * @param redisKey Redis 格式的缓存 key
     * @param type     缓存类型
     * @return 业务 key
     */
    private String extractBusinessKey(String redisKey, String type) {
        String prefix = CACHE_KEY_PREFIX_MAPPING.get(type);
        if (StrUtil.isNotBlank(prefix) && redisKey.startsWith(prefix)) {
            return redisKey.substring(prefix.length());
        }
        return redisKey;
    }

    /**
     * 构建空响应
     *
     * @param instanceId 实例 ID
     * @param type       缓存类型
     * @return 空响应
     */
    private CacheStatusResponse buildEmptyResponse(String instanceId, String type) {
        return CacheStatusResponse.builder()
                .instanceId(instanceId)
                .type(type)
                .timestamp(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
    }

    /**
     * 获取实例 ID
     */
    private String getInstanceId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return hostAddress + ":" + serverPort;
        } catch (Exception e) {
            return "unknown:" + serverPort;
        }
    }
}