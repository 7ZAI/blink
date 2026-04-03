package com.blink.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 配置类
 * 提供熔断器、限流器、重试等配置
 * 目前注释掉 使用yml取代
 * 但是未来会配置事件监听 监听熔断器各种指标输出到监控系统
 */
@Slf4j
//@Configuration
public class Resilience4jConfig {

    /**
     * 自定义 ReactiveResilience4JCircuitBreaker 配置
     * 这个配置会应用到所有使用 CircuitBreaker 过滤器的路由
     */
//    @Bean
//    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
//        return factory -> factory.configureDefault(id -> {
//            log.info(" 配置默认熔断器: {}", id);
//
//            return new Resilience4JConfigBuilder(id)
//                    // 熔断器配置
//                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
//                            // 滑动窗口类型：基于计数
//                            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
//                            // 滑动窗口大小：10次调用
//                            .slidingWindowSize(10)
//                            // 最小调用次数：5次
//                            .minimumNumberOfCalls(5)
//                            // 失败率阈值：50%
//                            .failureRateThreshold(50)
//                            // 慢调用率阈值：100%
//                            .slowCallRateThreshold(100)
//                            // 慢调用时间阈值：60秒
//                            .slowCallDurationThreshold(Duration.ofSeconds(60))
//                            // OPEN 到 HALF_OPEN 的等待时间：60秒
//                            .waitDurationInOpenState(Duration.ofSeconds(60))
//                            // HALF_OPEN 状态允许的调用数：3次
//                            .permittedNumberOfCallsInHalfOpenState(3)
//                            // 自动转换到 HALF_OPEN
//                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
//                            // 记录所有异常
//                            .recordExceptions(
//                                    Exception.class
//                            )
//                            .build())
//                    // 超时配置
//                    .timeLimiterConfig(TimeLimiterConfig.custom()
//                            // 超时时间：3秒
//                            .timeoutDuration(Duration.ofSeconds(3))
//                            // 取消正在运行的 Future
//                            .cancelRunningFuture(true)
//                            .build())
//                    .build();
//        });
//    }
//
//    /**
//     * 自定义特定熔断器的配置
//     * 可以为不同的服务配置不同的熔断策略
//     */
//    @Bean
//    public Customizer<ReactiveResilience4JCircuitBreakerFactory> specificCustomizer() {
//        return factory -> {
//            // 为 myCircuitBreaker 配置特定策略
//            factory.configure(builder -> builder
//                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
//                            .slidingWindowSize(20)
//                            .minimumNumberOfCalls(10)
//                            .failureRateThreshold(60)
//                            .waitDurationInOpenState(Duration.ofSeconds(30))
//                            .build())
//                    .timeLimiterConfig(TimeLimiterConfig.custom()
//                            .timeoutDuration(Duration.ofSeconds(2))
//                            .build()),
//                    "myCircuitBreaker");
//
//            // 为 strictCircuitBreaker 配置更严格的策略
//            factory.configure(builder -> builder
//                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
//                            .slidingWindowSize(10)
//                            .minimumNumberOfCalls(5)
//                            .failureRateThreshold(30)  // 更低的失败率阈值
//                            .waitDurationInOpenState(Duration.ofSeconds(120))  // 更长的等待时间
//                            .build())
//                    .timeLimiterConfig(TimeLimiterConfig.custom()
//                            .timeoutDuration(Duration.ofSeconds(1))  // 更短的超时时间
//                            .build()),
//                    "strictCircuitBreaker");
//
//            // 为 lenientCircuitBreaker 配置更宽松的策略
//            factory.configure(builder -> builder
//                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
//                            .slidingWindowSize(50)
//                            .minimumNumberOfCalls(20)
//                            .failureRateThreshold(80)  // 更高的失败率阈值
//                            .waitDurationInOpenState(Duration.ofSeconds(30))  // 更短的等待时间
//                            .build())
//                    .timeLimiterConfig(TimeLimiterConfig.custom()
//                            .timeoutDuration(Duration.ofSeconds(5))  // 更长的超时时间
//                            .build()),
//                    "lenientCircuitBreaker");
//        };
//    }

    /**
     * 熔断器事件监听器（可选）
     * 用于监控和日志记录
     */
    @Bean
    public CircuitBreakerEventListener circuitBreakerEventListener() {
        return new CircuitBreakerEventListener();
    }
}

/**
 * 熔断器事件监听器
 * 监听熔断器的状态变化
 */
@Slf4j
class CircuitBreakerEventListener {
    
    public CircuitBreakerEventListener() {
        log.info(" 初始化熔断器事件监听器");
    }
    
    // 注意：实际的事件监听需要在 application.yml 中配置
    // 或者使用 @EventListener 注解
}