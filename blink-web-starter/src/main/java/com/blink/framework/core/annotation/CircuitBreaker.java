package com.blink.framework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 熔断注解
 * 
 * 基于 Resilience4j CircuitBreaker 实现
 * 
 * 使用示例：
 * <pre>
 * // 基本用法
 * @CircuitBreaker(name = "myApi")
 * public Result<Data> callExternalApi() { ... }
 * 
 * // 使用预定义策略
 * @CircuitBreaker(name = "paymentApi", configName = "strict")
 * public Result<Payment> processPayment() { ... }
 * 
 * // 带降级方法
 * @CircuitBreaker(name = "userApi", fallbackMethod = "getUserFallback")
 * public Result<User> getUser(Long id) { ... }
 * 
 * public Result<User> getUserFallback(Long id, Throwable t) {
 *     return Result.fail("服务暂时不可用");
 * }
 * </pre>
 * 
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {

    /**
     * 熔断器名称（必填）
     */
    String name();

    /**
     * 配置名称
     * 引用预定义的配置模板（strict/lenient/default）
     */
    String configName() default "default";

    /**
     * 熔断时的降级方法名
     */
    String fallbackMethod() default "";
}
