package com.blink.framework.redis.lock;

/**
 * 分布式锁获取失败处理策略。
 * <p>
 * 定义当分布式锁在指定等待时间内无法获取时的不同处理方式。
 * </p>
 *
 * @author binblink
 */
public enum LockFailureStrategy {

    /**
     * 抛出 LockAcquisitionException 异常。
     * <p>
     * 默认策略，适用于需要严格保证锁获取的场景。
     * </p>
     */
    THROW_EXCEPTION,

    /**
     * 返回 null（有返回值的方法）或跳过执行（void 方法）。
     * <p>
     * 适用于当锁不可用时希望静默跳过操作的场景。
     * </p>
     */
    RETURN_NULL,

    /**
     * 不加锁直接执行方法。
     * <p>
     * 警告：此策略可能导致并发问题，请谨慎使用。适用于对并发要求不高的场景。
     * </p>
     */
    EXECUTE_WITHOUT_LOCK,

    /**
     * 重试获取锁。
     * <p>
     * 按配置的重试次数和间隔进行重试，重试次数和间隔可在 DistributedLockProperties 中配置。
     * </p>
     */
    RETRY
}
