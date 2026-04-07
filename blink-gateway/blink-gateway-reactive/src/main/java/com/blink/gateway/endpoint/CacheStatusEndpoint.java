package com.blink.gateway.endpoint;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.dto.CacheItemDTO;
import com.blink.gateway.dto.CacheStatusResponse;
import com.blink.gateway.route.RedisRouteDefinitionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.constant.RedisConstans.*;

/**
 * 缓存状态 Actuator 端点
 * 用于一致性检查，返回各类型缓存的 key 和 checksum
 *
 * @author binblink
 */
@Endpoint(id = "cache-status")
@Component
@Slf4j
public class CacheStatusEndpoint {

    @Resource
    private ReactiveRedisClient redisClient;

//    @Resource
//    private RedisRouteDefinitionRepository routeRepository;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 获取指定类型的缓存状态
     *
     * @param type 缓存类型: channel / route / config
     * @return 缓存状态响应
     */
    @ReadOperation
    public Mono<CacheStatusResponse> cacheStatus(@Selector String type) {
        String instanceId = getInstanceId();

        return switch (type.toLowerCase()) {
            case "channel" -> getChannelCacheStatus(instanceId);
//            case "route" -> getRouteCacheStatus(instanceId);
            case "config" -> getConfigCacheStatus(instanceId);
            default -> Mono.just(CacheStatusResponse.builder()
                    .instanceId(instanceId)
                    .type(type)
                    .timestamp(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build());
        };
    }

    /**
     * 获取渠道缓存状态
     */
    private Mono<CacheStatusResponse> getChannelCacheStatus(String instanceId) {
        String pattern = BLINK_CHANNEL_PREFIX + "*";
        return redisClient.keys(pattern)
                .flatMap(key -> {
                    return redisClient.get(key)
                            .map(value -> {
                                String checksum = DigestUtil.md5Hex(value.toString());
                                return CacheItemDTO.builder()
                                        .key(extractChannelKey(key))
                                        .checksum(checksum)
                                        .updateTime(LocalDateTime.now())
                                        .build();
                            });
                })
                .collectList()
                .map(items -> CacheStatusResponse.builder()
                        .instanceId(instanceId)
                        .type("channel")
                        .timestamp(LocalDateTime.now())
                        .items(items)
                        .build())
                .onErrorResume(e -> {
                    log.error("[CacheStatusEndpoint] 获取渠道缓存状态失败 | error: {}", e.getMessage(), e);
                    return Mono.just(CacheStatusResponse.builder()
                            .instanceId(instanceId)
                            .type("channel")
                            .timestamp(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build());
                });
    }




    /**
     * 获取配置缓存状态
     */
    private Mono<CacheStatusResponse> getConfigCacheStatus(String instanceId) {
        String pattern = GATEWAY_CONFIG_KEY_PREFIX + "*";
        return redisClient.keys(pattern)
                .flatMap(key -> {
                    return redisClient.get(key)
                            .map(value -> {
                                String checksum = DigestUtil.md5Hex(value.toString());
                                return CacheItemDTO.builder()
                                        .key(extractConfigKey(key))
                                        .checksum(checksum)
                                        .updateTime(LocalDateTime.now())
                                        .build();
                            });
                })
                .collectList()
                .map(items -> CacheStatusResponse.builder()
                        .instanceId(instanceId)
                        .type("config")
                        .timestamp(LocalDateTime.now())
                        .items(items)
                        .build())
                .onErrorResume(e -> {
                    log.error("[CacheStatusEndpoint] 获取配置缓存状态失败 | error: {}", e.getMessage(), e);
                    return Mono.just(CacheStatusResponse.builder()
                            .instanceId(instanceId)
                            .type("config")
                            .timestamp(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build());
                });
    }

    /**
     * 计算路由 checksum
     */
    private String calculateRouteChecksum(RouteDefinition route) {
        String json = JacksonUtil.toJson(route);
        return DigestUtil.md5Hex(json);
    }

    /**
     * 从 Redis key 中提取渠道标识
     */
    private String extractChannelKey(String redisKey) {
        // blink:channel:appKey -> appKey
        return redisKey.replace(BLINK_CHANNEL_PREFIX, "");
    }

    /**
     * 从 Redis key 中提取配置 key
     */
    private String extractConfigKey(String redisKey) {
        // blink:config:gateway:keyName -> keyName
        return redisKey.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
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