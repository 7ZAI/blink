package com.blink.framework.core.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Resilience4j 配置属性
 * 
 * 提供多种预定义策略：
 * - default: 默认策略，适合大多数场景
 * - strict: 严格策略，适合核心业务
 * - lenient: 宽松策略，适合非核心业务
 * 
 * @author binblink
 */
@Data
@ConfigurationProperties(prefix = "blink.web.resilience")
public class ResilienceProperties {

    /**
     * 是否启用限流熔断功能
     */
    private Boolean enabled = true;

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class CircuitBreakerConfig {
        /**
         * 默认熔断器配置
         */
        private CircuitBreakerTemplate defaultConfig = createDefaultConfig();

        /**
         * 严格模式配置
         */
        private CircuitBreakerTemplate strictConfig = createStrictConfig();

        /**
         * 宽松模式配置
         */
        private CircuitBreakerTemplate lenientConfig = createLenientConfig();

        private static CircuitBreakerTemplate createDefaultConfig() {
            CircuitBreakerTemplate config = new CircuitBreakerTemplate();
            config.setSlidingWindowSize(10);
            config.setMinimumNumberOfCalls(5);
            config.setFailureRateThreshold(50);
            config.setSlowCallRateThreshold(100);
            config.setSlowCallDurationThreshold(Duration.ofSeconds(8));
            config.setWaitDurationInOpenState(Duration.ofSeconds(60));
            config.setPermittedNumberOfCallsInHalfOpenState(3);
            return config;
        }

        private static CircuitBreakerTemplate createStrictConfig() {
            CircuitBreakerTemplate config = new CircuitBreakerTemplate();
            config.setSlidingWindowSize(10);
            config.setMinimumNumberOfCalls(5);
            config.setFailureRateThreshold(30);
            config.setSlowCallRateThreshold(50);
            config.setSlowCallDurationThreshold(Duration.ofSeconds(3));
            config.setWaitDurationInOpenState(Duration.ofSeconds(120));
            config.setPermittedNumberOfCallsInHalfOpenState(2);
            return config;
        }

        private static CircuitBreakerTemplate createLenientConfig() {
            CircuitBreakerTemplate config = new CircuitBreakerTemplate();
            config.setSlidingWindowSize(20);
            config.setMinimumNumberOfCalls(10);
            config.setFailureRateThreshold(70);
            config.setSlowCallRateThreshold(100);
            config.setSlowCallDurationThreshold(Duration.ofSeconds(15));
            config.setWaitDurationInOpenState(Duration.ofSeconds(30));
            config.setPermittedNumberOfCallsInHalfOpenState(5);
            return config;
        }
    }

    @Data
    public static class CircuitBreakerTemplate {
        private Integer slidingWindowSize;
        private Integer minimumNumberOfCalls;
        private Integer failureRateThreshold;
        private Integer slowCallRateThreshold;
        private Duration slowCallDurationThreshold;
        private Duration waitDurationInOpenState;
        private Integer permittedNumberOfCallsInHalfOpenState;
    }

    @Data
    public static class RateLimiterConfig {
        /**
         * 默认限流器配置
         */
        private RateLimiterTemplate defaultConfig = createDefaultConfig();

        /**
         * 严格模式配置
         */
        private RateLimiterTemplate strictConfig = createStrictConfig();

        /**
         * 宽松模式配置
         */
        private RateLimiterTemplate lenientConfig = createLenientConfig();

        private static RateLimiterTemplate createDefaultConfig() {
            RateLimiterTemplate config = new RateLimiterTemplate();
            config.setLimitForPeriod(100);
            config.setLimitRefreshPeriod(Duration.ofSeconds(1));
            config.setTimeoutDuration(Duration.ZERO);
            return config;
        }

        private static RateLimiterTemplate createStrictConfig() {
            RateLimiterTemplate config = new RateLimiterTemplate();
            config.setLimitForPeriod(50);
            config.setLimitRefreshPeriod(Duration.ofSeconds(1));
            config.setTimeoutDuration(Duration.ZERO);
            return config;
        }

        private static RateLimiterTemplate createLenientConfig() {
            RateLimiterTemplate config = new RateLimiterTemplate();
            config.setLimitForPeriod(200);
            config.setLimitRefreshPeriod(Duration.ofSeconds(1));
            config.setTimeoutDuration(Duration.ofMillis(100));
            return config;
        }
    }

    @Data
    public static class RateLimiterTemplate {
        private Integer limitForPeriod;
        private Duration limitRefreshPeriod;
        private Duration timeoutDuration;
    }

    @Data
    public static class RetryConfig {
        /**
         * 默认重试配置
         */
        private RetryTemplate defaultConfig = createDefaultConfig();

        /**
         * 快速重试配置
         */
        private RetryTemplate quickConfig = createQuickConfig();

        /**
         * 慢速重试配置
         */
        private RetryTemplate slowConfig = createSlowConfig();

        private static RetryTemplate createDefaultConfig() {
            RetryTemplate config = new RetryTemplate();
            config.setMaxAttempts(3);
            config.setWaitDuration(Duration.ofMillis(500));
            return config;
        }

        private static RetryTemplate createQuickConfig() {
            RetryTemplate config = new RetryTemplate();
            config.setMaxAttempts(2);
            config.setWaitDuration(Duration.ofMillis(200));
            return config;
        }

        private static RetryTemplate createSlowConfig() {
            RetryTemplate config = new RetryTemplate();
            config.setMaxAttempts(5);
            config.setWaitDuration(Duration.ofMillis(1000));
            return config;
        }
    }

    @Data
    public static class RetryTemplate {
        private Integer maxAttempts;
        private Duration waitDuration;
    }
}
