package com.blink.framework.redis.lock;

import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于redis的分布式锁
 */
@Slf4j
public class RedisDistributeLock {

 
    /**
     * 默认锁过期时间 - 15分钟
     */
    private static final long DEFAULT_EXPIRE_TIME_SECOND = 60000;
    /**
     * 默认重试取锁间隔 - 毫秒
     */
    private static final long DEFAULT_RETRY_FIXED_TIME = 150;
    /**
     * 默认加锁浮动时间 - 毫秒
     */
    private static final int DEFAULT_RETRY_TIME_RANGE = 50;
    /**
     * 默认获取锁次数
     */
    private static final int DEFAULT_RETRY_COUNT = 30;

    private final RedissonClient redisson;

    public RedisDistributeLock(RedissonClient redisson){
        this.redisson = redisson;
    }

    /**
     * 加锁
     *
     * @param lockName 锁的名称
     * @return 加锁结果
     */
    public boolean lock(String lockName) {
        return lock(lockName, DEFAULT_RETRY_FIXED_TIME + DEFAULT_RETRY_TIME_RANGE, DEFAULT_EXPIRE_TIME_SECOND);
    }

    /**
     * 加锁
     *
     * @param lockName 锁的名称
     * @param waitTime 等待时间
     * @return 加锁结果
     */
    public boolean lock(String lockName, long waitTime) {
        return lock(lockName, waitTime, DEFAULT_EXPIRE_TIME_SECOND);
    }

    /**
     * 加锁
     *
     * @param lockName   锁的名称
     * @param waitTime   等待时间
     * @param expireTime 过期时间
     * @return 加锁结果
     */
    public boolean lock(String lockName, long waitTime, long expireTime) {
        try {
            RLock rLock = redisson.getLock(lockName);
            return rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("获取分布式锁异常", e);

        }
        return false;
    }

    /**
     * 尝试加锁
     *
     * @param lockName 锁的名称
     * @return 加锁结果:返回uuid;加锁失败:返回null
     */
    public boolean lockAndRetry(String lockName) {
        return lockAndRetry(lockName, DEFAULT_RETRY_FIXED_TIME + DEFAULT_RETRY_TIME_RANGE);
    }

    /**
     * 尝试加锁
     *
     * @param lockName 锁的名称
     * @param waitTime 等待时间
     * @return 加锁结果
     */
    public boolean lockAndRetry(String lockName, long waitTime) {
        return lockAndRetry(lockName, waitTime, DEFAULT_EXPIRE_TIME_SECOND);
    }

    /**
     * 尝试加锁
     *
     * @param lockName   锁的名称
     * @param waitTime   等待时间
     * @param expireTime 过期时间
     * @return 加锁结果
     */
    public boolean lockAndRetry(String lockName, long waitTime, long expireTime) {
        return lockAndRetry(lockName, waitTime, expireTime, DEFAULT_RETRY_COUNT);
    }

    /**
     * 尝试加锁
     *
     * @param lockName   锁的名称
     * @param waitTime   等待时间
     * @param expireTime 过期时间
     * @param retryCount 尝试次数
     * @return 加锁结果
     */
    public boolean lockAndRetry(String lockName, long waitTime, long expireTime, int retryCount) {
        if (retryCount <= 0) {
            // 无限次数尝试
            while (true) {
                boolean result = retryLock(lockName, waitTime, expireTime);
                if (result) {
                    return true;
                }
            }
        } else {
            // 指定次数尝试获
            for (int i = 0; i < retryCount; i++) {
                boolean result = retryLock(lockName, waitTime, expireTime);
                if (result) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 尝试加锁
     *
     * @param lockName   锁的名称
     * @param waitTime   等待时间
     * @param expireTime 过期时间
     * @return 加锁结果
     */
    private boolean retryLock(String lockName, long waitTime, long expireTime) {
        boolean result = lock(lockName, waitTime, expireTime);
        if (result) {
            return true;
        }
        try {
            Thread.sleep(RandomUtil.randomLong(DEFAULT_RETRY_FIXED_TIME - DEFAULT_RETRY_TIME_RANGE,
                    DEFAULT_RETRY_FIXED_TIME + DEFAULT_RETRY_TIME_RANGE));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 释放锁
     *
     * @param lockName 锁的名称
     */
    public void unLock(String lockName) {
        RLock rLock = redisson.getLock(lockName);
        rLock.unlock();
    }
}
