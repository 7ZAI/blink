package com.blink.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

//@Configuration
public class RateLimiterConfig {
    
    /**
     * 基于IP地址的全局限流
     * 同一IP地址的请求共享一个限流计数器[citation:1][citation:8]
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
    
    /**
     * 基于请求路径的全局限流
     * 同一API路径的请求共享限流计数器[citation:1][citation:8]
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getURI().getPath()
        );
    }
    
    /**
     * 基于用户ID的全局限流
     * 从请求参数中获取用户ID[citation:5]
     */
//    @Bean
//    public KeyResolver userKeyResolver() {
//        return exchange -> Mono.just(
//            exchange.getRequest().getQueryParams().getFirst("userId")
//        );
//    }
}