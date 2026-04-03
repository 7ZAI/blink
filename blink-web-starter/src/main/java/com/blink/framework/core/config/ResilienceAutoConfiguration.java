package com.blink.framework.core.config;

import com.blink.framework.core.config.prop.ResilienceProperties;
import com.blink.framework.core.resilience.CircuitBreakerAspect;
import com.blink.framework.core.resilience.IpRateLimitAspect;
import com.blink.framework.core.resilience.RateLimitAspect;
import com.blink.framework.core.resilience.RetryAspect;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Resilience4j 自动配置类
 * 
 * 提供限流、熔断、重试等弹性能力
 * 
 * @author binblink
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ResilienceProperties.class)
@ConditionalOnProperty(prefix = "blink.web.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceAutoConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        log.info("初始化 RateLimiterRegistry");
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("初始化 CircuitBreakerRegistry");
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public RetryRegistry retryRegistry() {
        log.info("初始化 RetryRegistry");
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public RateLimitAspect blinkRateLimitAspect(RateLimiterRegistry rateLimiterRegistry, ResilienceProperties properties) {
        log.info("初始化 RateLimitAspect");
        return new RateLimitAspect(rateLimiterRegistry, properties);
    }

    @Bean
    public IpRateLimitAspect blinkIpRateLimitAspect(RateLimiterRegistry rateLimiterRegistry) {
        log.info("初始化 IpRateLimitAspect");
        return new IpRateLimitAspect(rateLimiterRegistry);
    }

    @Bean
    public CircuitBreakerAspect blinkCircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry, ResilienceProperties properties) {
        log.info("初始化 CircuitBreakerAspect");
        return new CircuitBreakerAspect(circuitBreakerRegistry, properties);
    }

    @Bean
    public RetryAspect blinkRetryAspect(RetryRegistry retryRegistry, ResilienceProperties properties) {
        log.info("初始化 RetryAspect");
        return new RetryAspect(retryRegistry, properties);
    }
}
