package com.blink.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @Author binblink
 * @Date 2025/9/6
 */
@Configuration
public class GatewayLocalCacheConfig {

    @Bean("gatewayLocalCache")
    public CacheManager cacheManager() {
        CaffeineCacheManager  cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .recordStats());

        return cacheManager;
    }
}
