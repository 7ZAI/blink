package com.blink.framework.redis.lock;

/**
 * 分布式锁获取失败异常。
 * <p>
 * 当以下情况发生时抛出此异常：
 * <ul>
 *   <li>等待获取锁超时</li>
 *   <li>获取锁被中断</li>
 *   <li>获取锁过程中发生意外错误</li>
 * </ul>
 * </p>
 *
 * @author binblink
 */
public class LockAcquisitionException extends RuntimeException {

    /**
     * 构造带有错误消息的异常实例。
     *
     * @param message 错误消息
     */
    public LockAcquisitionException(String message) {
        super(message);
    }

    /**
     * 构造带有错误消息和原因的异常实例。
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
