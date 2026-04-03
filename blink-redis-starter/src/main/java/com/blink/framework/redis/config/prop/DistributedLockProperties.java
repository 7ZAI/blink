package com.blink.framework.redis.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 分布式锁配置属性。
 * <p>
 * 通过配置前缀 blink.redis.distributed-lock 进行配置。
 * </p>
 *
 * <p>配置示例：</p>
 * <pre>
 * blink:
 *   redis:
 *     distributed-lock:
 *       enabled: true
 *       default-wait-time: 3s
 *       default-lease-time: 30s
 *       watchdog-enabled: true
 *       watchdog-timeout: 30s
 *       retry-interval: 100ms
 *       retry-count: 3
 *       key-prefix: "lock:"
 * </pre>
 *
 * @author binblink
 */
@ConfigurationProperties(prefix = "blink.redis.distributed-lock")
public class DistributedLockProperties {

    /**
     * 是否启用分布式锁功能
     */
    private boolean enabled = false;

    /**
     * 获取锁的默认等待时间
     */
    private Duration defaultWaitTime = Duration.ofSeconds(3);

    /**
     * 锁的默认持有时间
     */
    private Duration defaultLeaseTime = Duration.ofSeconds(30);

    /**
     * 是否启用看门狗自动续期机制
     */
    private boolean watchdogEnabled = true;

    /**
     * 看门狗超时时间
     */
    private Duration watchdogTimeout = Duration.ofSeconds(30);

    /**
     * 重试间隔时间
     */
    private Duration retryInterval = Duration.ofMillis(100);

    /**
     * 锁键前缀
     */
    private String keyPrefix = "lock:";

    /**
     * 重试次数
     */
    private int retryCount = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDefaultWaitTime() {
        return defaultWaitTime;
    }

    public void setDefaultWaitTime(Duration defaultWaitTime) {
        this.defaultWaitTime = defaultWaitTime;
    }

    public Duration getDefaultLeaseTime() {
        return defaultLeaseTime;
    }

    public void setDefaultLeaseTime(Duration defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    public boolean isWatchdogEnabled() {
        return watchdogEnabled;
    }

    public void setWatchdogEnabled(boolean watchdogEnabled) {
        this.watchdogEnabled = watchdogEnabled;
    }

    public Duration getWatchdogTimeout() {
        return watchdogTimeout;
    }

    public void setWatchdogTimeout(Duration watchdogTimeout) {
        this.watchdogTimeout = watchdogTimeout;
    }

    public Duration getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
