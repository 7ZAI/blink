package com.blink.framework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 重试注解
 * 
 * 基于 Resilience4j Retry 实现
 * 
 * 使用示例：
 * <pre>
 * // 基本用法
 * @Retry(name = "myApi")
 * public Result<Data> callExternalApi() { ... }
 * 
 * // 自定义重试参数
 * @Retry(
 *     name = "paymentApi",
 *     maxAttempts = 3,
 *     waitDuration = 1000
 * )
 * public Result<Payment> processPayment() { ... }
 * 
 * // 使用预定义策略
 * @Retry(name = "userApi", configName = "quick")
 * public Result<User> getUser(Long id) { ... }
 * </pre>
 * 
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {

    /**
     * 重试器名称（必填）
     */
    String name();

    /**
     * 配置名称
     * 引用预定义的配置模板（quick/slow/default）
     */
    String configName() default "default";

    /**
     * 最大重试次数（包含首次调用）
     */
    int maxAttempts() default 3;

    /**
     * 重试等待时间（毫秒）
     */
    long waitDuration() default 500;

    /**
     * 重试时的降级方法名
     */
    String fallbackMethod() default "";
}
