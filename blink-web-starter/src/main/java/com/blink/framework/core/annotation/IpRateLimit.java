package com.blink.framework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 基于IP的限流注解
 * 
 * 针对单个IP地址进行限流，适用于公开接口的安全防护
 * 
 * 使用示例：
 * <pre>
 * // 基本用法 - 每秒最多10次请求
 * @IpRateLimit(name = "publicApi")
 * public Result<Data> publicApi() { ... }
 * 
 * // 严格模式 - 每秒最多3次请求
 * @IpRateLimit(name = "loginConfig", limitForPeriod = 3, limitRefreshPeriod = 1)
 * public Result<Config> getLoginConfig() { ... }
 * 
 * // 带降级方法
 * @IpRateLimit(name = "api", fallbackMethod = "fallback")
 * public Result<Data> api() { ... }
 * </pre>
 * 
 * @author blink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IpRateLimit {

    /**
     * 限流器名称（必填）
     * 用于标识和监控
     */
    String name();

    /**
     * 一个周期内允许的请求数
     * 默认每秒10次
     */
    int limitForPeriod() default 10;

    /**
     * 周期刷新时间（秒）
     * 默认1秒
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
