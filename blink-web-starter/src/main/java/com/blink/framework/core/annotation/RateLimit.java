package com.blink.framework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 
 * 基于 Resilience4j RateLimiter 实现
 * 
 * 使用示例：
 * <pre>
 * // 基本用法
 * @RateLimit(name = "myApi")
 * public Result<Data> getData() { ... }
 * 
 * // 自定义限流参数
 * @RateLimit(
 *     name = "orderApi",
 *     limitForPeriod = 100,
 *     limitRefreshPeriod = 1,
 *     timeoutDuration = 500
 * )
 * public Result<Order> createOrder() { ... }
 * 
 * // 使用预定义策略
 * @RateLimit(name = "strictApi", configName = "strict")
 * public Result<Data> strictApi() { ... }
 * </pre>
 * 
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流器名称（必填）
     * 用于标识和监控
     */
    String name();

    /**
     * 配置名称
     * 引用预定义的配置模板（strict/lenient/default）
     */
    String configName() default "default";

    /**
     * 一个周期内允许的请求数
     */
    int limitForPeriod() default 100;

    /**
     * 周期刷新时间（秒）
     */
    int limitRefreshPeriod() default 1;

    /**
     * 等待获取许可的超时时间（毫秒）
     * 0表示不等待，直接拒绝
     */
    long timeoutDuration() default 0;

    /**
     * 限流时的降级方法名
     */
    String fallbackMethod() default "";
}
