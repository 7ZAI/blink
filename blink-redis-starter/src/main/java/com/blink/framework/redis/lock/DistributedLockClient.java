package com.blink.framework.redis.lock;

import lombok.extern.slf4j.Slf4j;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.config.prop.DistributedLockProperties;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁客户端，提供生产级的分布式锁能力。
 * <p>
 * 该客户端封装了 Redisson 的 RLock，提供简化易用的 API 进行分布式锁操作。
 * 支持阻塞和非阻塞锁获取、自动锁释放以及看门狗机制。
 * </p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>阻塞和非阻塞锁获取</li>
 *   <li>通过 try-with-resources 自动释放锁</li>
 *   <li>看门狗机制实现自动续期</li>
 *   <li>异常安全的锁操作</li>
 *   <li>支持公平锁、读写锁</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 阻塞锁，自动释放
 * lockClient.executeWithLock("my-resource", () -> {
 *     // 临界区代码
 *     return result;
 * });
 *
 * // 尝试获取锁，带超时时间
 * boolean acquired = lockClient.tryLock("my-resource", Duration.ofSeconds(5));
 * if (acquired) {
 *     try {
 *         // 临界区代码
 *     } finally {
 *         lockClient.unlock("my-resource");
 *     }
 * }
 * </pre>
 *
 * <p><b>依赖说明：</b></p>
 * <p>
 * Redisson 是可选依赖，使用分布式锁功能需要在项目中添加 Redisson 依赖：
 * </p>
 * <pre>
 * implementation 'org.redisson:redisson-spring-boot-starter:3.23.1'
 * </pre>
 *
 * @author binblink
 * @see DistributedLockProperties
 */
@Slf4j
public class DistributedLockClient {

    /**
     * Redisson 客户端实例
     */
    private final RedissonClient redissonClient;

    /**
     * 分布式锁配置属性
     */
    private final DistributedLockProperties properties;

    /**
     * 构造分布式锁客户端实例。
     *
     * @param redissonClient Redisson 客户端实例
     * @param properties     分布式锁配置属性
     */
    public DistributedLockClient(RedissonClient redissonClient, DistributedLockProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 获取分布式锁并执行给定的操作。
     * <p>
     * 该方法会阻塞直到获取锁或等待超时，操作完成后自动释放锁。
     * </p>
     *
     * @param lockKey 锁键名
     * @param action  持有锁期间要执行的操作
     * @param <T>     操作返回类型
     * @return 操作执行结果
     * @throws LockAcquisitionException 无法获取锁时抛出
     * @throws RuntimeException         操作执行异常时抛出
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, properties.getDefaultWaitTime(), properties.getDefaultLeaseTime(), action);
    }

    /**
     * 获取分布式锁（自定义等待和持有时间）并执行给定操作。
     * <p>
     * 注意：leaseTime 使用 Duration.ofMillis(-1) 启用看门狗自动续期机制。
     * </p>
     *
     * @param lockKey   锁键名
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁持有时间（使用 Duration.ofMillis(-1) 启用看门狗）
     * @param action    持有锁期间要执行的操作
     * @param <T>       操作返回类型
     * @return 操作执行结果
     * @throws LockAcquisitionException 无法获取锁时抛出
     */
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        String fullKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullKey);

        boolean acquired = tryAcquire(lock, fullKey, waitTime, leaseTime);
        if (!acquired) {
            throw new LockAcquisitionException("获取分布式锁失败: " + fullKey);
        }

        try {
            return action.get();
        } finally {
            safeUnlock(lock, fullKey);
        }
    }

    /**
     * 获取分布式锁并执行无返回值的操作。
     *
     * @param lockKey 锁键名
     * @param action  要执行的操作
     * @throws LockAcquisitionException 无法获取锁时抛出
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, properties.getDefaultWaitTime(), properties.getDefaultLeaseTime(), action);
    }

    /**
     * 获取分布式锁（自定义等待和持有时间）并执行无返回值的操作。
     *
     * @param lockKey   锁键名
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁持有时间
     * @param action    要执行的操作
     * @throws LockAcquisitionException 无法获取锁时抛出
     */
    public void executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable action) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁（非阻塞）。
     *
     * @param lockKey 锁键名
     * @return 获取成功返回 true，否则返回 false
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, Duration.ZERO, properties.getDefaultLeaseTime());
    }

    /**
     * 尝试获取锁（指定等待时间）。
     *
     * @param lockKey  锁键名
     * @param waitTime 获取锁的最大等待时间
     * @return 获取成功返回 true，否则返回 false
     */
    public boolean tryLock(String lockKey, Duration waitTime) {
        return tryLock(lockKey, waitTime, properties.getDefaultLeaseTime());
    }

    /**
     * 尝试获取锁（指定等待和持有时间）。
     *
     * @param lockKey   锁键名
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁持有时间
     * @return 获取成功返回 true，否则返回 false
     */
    public boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        String fullKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullKey);
        return tryAcquire(lock, fullKey, waitTime, leaseTime);
    }

    /**
     * 使用指定的 RLock 实例尝试获取锁。
     * <p>
     * 主要用于公平锁场景，确保加锁和解锁使用同一个锁实例。
     * </p>
     *
     * @param lock      锁实例
     * @param lockKey   锁键名（不带前缀，仅用于日志）
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁持有时间
     * @return 获取成功返回 true，否则返回 false
     */
    public boolean tryLock(RLock lock, String lockKey, Duration waitTime, Duration leaseTime) {
        return tryAcquire(lock, buildLockKey(lockKey), waitTime, leaseTime);
    }

    /**
     * 释放指定的锁实例。
     * <p>
     * 用于切面在公平锁场景下解锁同一把锁实例，避免重新 getLock() 取错对象。
     * </p>
     *
     * @param lock    锁实例
     * @param lockKey 锁键名（不带前缀，仅用于日志）
     */
    public void unlock(RLock lock, String lockKey) {
        safeUnlock(lock, buildLockKey(lockKey));
    }

    /**
     * 尝试获取锁的内部实现方法。
     * <p>
     * 支持看门狗机制：当 leaseTime 为负值且启用了看门狗时，使用 Redisson 的自动续期功能。
     * </p>
     *
     * @param lock      锁实例
     * @param fullKey   完整的锁键名（包含前缀，用于日志）
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁持有时间
     * @return 获取成功返回 true，否则返回 false
     */
    private boolean tryAcquire(RLock lock, String fullKey, Duration waitTime, Duration leaseTime) {
        try {
            long waitMillis = waitTime.toMillis();
            long leaseMillis = leaseTime.toMillis();

            boolean acquired;
            // leaseTime 为负值时启用看门狗自动续期机制
            if (leaseMillis < 0 && properties.isWatchdogEnabled()) {
                acquired = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
            } else {
                acquired = lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
            }

            if (acquired) {
                log.debug("[分布式锁] 获取成功 | key: {}", fullKey);
            } else {
                log.warn("[分布式锁] 获取失败 | key: {}, 等待时间: {}ms", fullKey, waitMillis);
            }
            return acquired;
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            log.error("[分布式锁] 获取被中断 | key: {}", fullKey, e);
            return false;
        }
    }

    /**
     * 释放锁。
     *
     * @param lockKey 锁键名（不带前缀）
     */
    public void unlock(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullKey);
        safeUnlock(lock, fullKey);
    }

    /**
     * 检查锁是否被任何线程持有。
     *
     * @param lockKey 锁键名
     * @return 锁被持有时返回 true，否则返回 false
     */
    public boolean isLocked(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullKey);
        return lock.isLocked();
    }

    /**
     * 检查当前线程是否持有锁。
     *
     * @param lockKey 锁键名
     * @return 当前线程持有锁时返回 true，否则返回 false
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 获取公平锁实例。
     * <p>
     * 公平锁保证按请求顺序获取锁，避免线程饥饿问题。
     * </p>
     *
     * @param lockKey 锁键名（不带前缀）
     * @return 公平锁实例
     */
    public RLock getFairLock(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        return redissonClient.getFairLock(fullKey);
    }

    /**
     * 获取读锁实例。
     * <p>
     * 读锁是共享锁，多个线程可以同时持有读锁。
     * </p>
     *
     * @param lockKey 锁键名
     * @return 读锁实例
     */
    public RLock getReadLock(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(fullKey);
        return readWriteLock.readLock();
    }

    /**
     * 获取写锁实例。
     * <p>
     * 写锁是排他锁，同一时间只能有一个线程持有写锁。
     * </p>
     *
     * @param lockKey 锁键名
     * @return 写锁实例
     */
    public RLock getWriteLock(String lockKey) {
        String fullKey = buildLockKey(lockKey);
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(fullKey);
        return readWriteLock.writeLock();
    }

    /**
     * 构建完整的锁键名（添加前缀）。
     *
     * @param lockKey 原始锁键名
     * @return 包含前缀的完整锁键名
     * @throws IllegalArgumentException 锁键名为空时抛出
     */
    private String buildLockKey(String lockKey) {
        if (lockKey == null || lockKey.isEmpty()) {
            throw new IllegalArgumentException("锁键名不能为空");
        }
        return properties.getKeyPrefix() + lockKey;
    }

    /**
     * 安全释放锁，包含完善的错误处理。
     * <p>
     * 释放前会检查当前线程是否持有锁，避免 IllegalMonitorStateException。
     * </p>
     *
     * @param lock    要释放的锁实例
     * @param lockKey 完整的锁键名（用于日志记录）
     */
    private void safeUnlock(RLock lock, String lockKey) {
        try {
            // 检查当前线程是否持有该锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[分布式锁] 释放成功 | key: {}", lockKey);
            }
        } catch (IllegalMonitorStateException e) {
            log.warn("[分布式锁] 锁已释放或非当前线程持有 | key: {}", lockKey);
        }
    }
}