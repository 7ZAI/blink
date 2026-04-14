package com.blink.gateway.monitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控配置持有者（gateway-reactive）
 *
 * 存储从 gateway-admin 通过 Redis Stream 推送过来的监控配置
 * 本地内存存储，配置变更时实时更新
 *
 * 配置来源：
 * 1. 初始化时从配置文件读取默认值
 * 2. 运行时通过 Redis Stream 接收 gateway-admin 推送的配置变更
 *
 * @author binblink
 * @since 2026-04-14
 */
@Component
@Slf4j
public class MonitorConfigHolder {

    /**
     * 监控开关（默认开启）
     * 控制是否推送监控指标到 Redis Stream
     */
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    /**
     * 指标推送间隔（毫秒）
     * 默认 5 秒，实现准实时监控
     */
    private final AtomicLong intervalMs = new AtomicLong(5000L);

    /**
     * 首次推送延迟（毫秒）
     */
    private final AtomicLong initialDelayMs = new AtomicLong(5000L);

    public MonitorConfigHolder(
            @Value("${blink.gateway.monitor.metrics-push.interval-ms:5000}") Long defaultIntervalMs,
            @Value("${blink.gateway.monitor.metrics-push.initial-delay-ms:5000}") Long defaultInitialDelayMs) {
        this.intervalMs.set(defaultIntervalMs);
        this.initialDelayMs.set(defaultInitialDelayMs);
        log.info("[MonitorConfig] 初始化 | enabled: {}, intervalMs: {}, initialDelayMs: {}",
                enabled.get(), intervalMs.get(), initialDelayMs.get());
    }

    /**
     * 获取监控开关状态
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 设置监控开关状态
     */
    public void setEnabled(boolean enabled) {
        boolean oldValue = this.enabled.getAndSet(enabled);
        if (oldValue != enabled) {
            log.info("[MonitorConfig] 监控开关变更 | {} -> {}", oldValue, enabled);
        }
    }

    /**
     * 获取指标推送间隔
     */
    public long getIntervalMs() {
        return intervalMs.get();
    }

    /**
     * 设置指标推送间隔
     */
    public void setIntervalMs(long intervalMs) {
        long oldValue = this.intervalMs.getAndSet(intervalMs);
        if (oldValue != intervalMs) {
            log.info("[MonitorConfig] 推送间隔变更 | {}ms -> {}ms", oldValue, intervalMs);
        }
    }

    /**
     * 获取首次推送延迟
     */
    public long getInitialDelayMs() {
        return initialDelayMs.get();
    }

    /**
     * 设置首次推送延迟
     */
    public void setInitialDelayMs(long initialDelayMs) {
        long oldValue = this.initialDelayMs.getAndSet(initialDelayMs);
        if (oldValue != initialDelayMs) {
            log.info("[MonitorConfig] 首次延迟变更 | {}ms -> {}ms", oldValue, initialDelayMs);
        }
    }
}
