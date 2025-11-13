package com.blink.framework.redis.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class LocalCacheConfig {

    @Bean
    @ConditionalOnProperty(name = "blink.redis.enableLocalCache",havingValue = "true")
    public Cache<String,Object> caffeineCache(){
        return  Caffeine.newBuilder()
                //最后一次写入1个小时候 失效
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .initialCapacity(200)
                .maximumSize(3000)
                .build();
    }

}
