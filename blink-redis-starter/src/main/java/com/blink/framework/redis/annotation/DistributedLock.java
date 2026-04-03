package com.blink.framework.redis.annotation;

import com.blink.framework.redis.lock.LockFailureStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解。
 * <p>
 * 应用于方法上，在方法执行前自动获取分布式锁，执行完成后释放锁。
 * </p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持 SpEL 表达式动态生成锁键</li>
 *   <li>可配置等待时间和持有时间</li>
 *   <li>多种获取锁失败处理策略</li>
 *   <li>支持公平锁</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 简单锁，使用默认配置
 * &#64;DistributedLock("my-resource")
 * public void doSomething() { ... }
 *
 * // 使用 SpEL 表达式
 * &#64;DistributedLock(key = "'user:' + #userId", waitTime = 5)
 * public void updateUser(Long userId) { ... }
 *
 * // 自定义持有时间和失败策略
 * &#64;DistributedLock(key = "order:#orderId", leaseTime = 60, timeUnit = TimeUnit.SECONDS,
 *                   failureStrategy = LockFailureStrategy.THROW_EXCEPTION)
 * public void processOrder(String orderId) { ... }
 * </pre>
 *
 * @author binblink
 * @see LockFailureStrategy
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁键，支持 SpEL 表达式。
     * <p>
     * 示例：
     * <ul>
     *   <li>"my-resource" - 静态键</li>
     *   <li>"user:#userId" - 方法参数引用</li>
     *   <li>'user:' + #user.id - 嵌套属性访问</li>
     * </ul>
     * </p>
     *
     * @return 锁键表达式
     */
    String key() default "";

    /**
     * key() 的别名，提供更简洁的使用方式。
     *
     * @return 锁键
     */
    String value() default "";

    /**
     * 获取锁的最大等待时间。
     * <p>
     * 默认值为 -1，表示使用配置的默认值。
     * </p>
     *
     * @return 等待时间值
     */
    long waitTime() default -1;

    /**
     * 锁持有时间。
     * <p>
     * 超过此时间锁将自动释放。默认值为 -1，表示启用看门狗自动续期机制。
     * </p>
     *
     * @return 持有时间值
     */
    long leaseTime() default -1;

    /**
     * waitTime 和 leaseTime 的时间单位。
     * <p>
     * 默认为秒。
     * </p>
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否使用公平锁。
     * <p>
     * 公平锁保证按请求顺序获取锁。默认为 false。
     * </p>
     *
     * @return 使用公平锁返回 true
     */
    boolean fairLock() default false;

    /**
     * 获取锁失败时的处理策略。
     * <p>
     * 默认为 THROW_EXCEPTION。
     * </p>
     *
     * @return 失败策略
     */
    LockFailureStrategy failureStrategy() default LockFailureStrategy.THROW_EXCEPTION;

    /**
     * 获取锁失败时的自定义异常消息。
     * <p>
     * 仅当 failureStrategy 为 THROW_EXCEPTION 时使用。
     * </p>
     *
     * @return 异常消息
     */
    String errorMessage() default "获取分布式锁失败";
}
