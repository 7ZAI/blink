package com.blink.gateway.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网关实例状态管理器
 *
 * 管理网关实例的上下线状态，支持优雅下线时的流量排空
 *
 * @author binblink
 * @since 2026-04-16
 */
@Component
@Slf4j
public class GatewayInstanceStateManager {

    /**
     * 实例是否正在接收请求
     * true - 正常接收请求
     * false - 拒绝新请求（优雅下线中）
     */
    @Getter
    private final AtomicBoolean acceptingRequests = new AtomicBoolean(true);

    /**
     * 实例是否正在排空流量
     */
    @Getter
    private final AtomicBoolean draining = new AtomicBoolean(false);

    /**
     * 下线开始时间戳
     */
    @Getter
    private final AtomicLong drainStartTime = new AtomicLong(0);

    /**
     * 排空等待时间（秒）
     */
    @Getter
    private volatile int drainWaitSeconds = 30;

    /**
     * 下线原因
     */
    @Getter
    private volatile String offlineReason;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 获取当前实例标识
     *
     * @return 实例标识，格式：host:port
     */
    public String getInstanceIdentifier() {
        String host = getLocalIp();
        return host + ":" + serverPort;
    }

    /**
     * 开始优雅下线
     *
     * @param waitSeconds 排空等待时间（秒）
     * @param reason      下线原因
     * @return 是否成功开始下线
     */
    public boolean startGracefulOffline(int waitSeconds, String reason) {
        // 已经在下线中
        if (!acceptingRequests.compareAndSet(true, false)) {
            log.warn("[InstanceState] 实例已在下线中，忽略重复下线指令");
            return false;
        }

        this.drainWaitSeconds = waitSeconds;
        this.offlineReason = reason;
        this.draining.set(true);
        this.drainStartTime.set(System.currentTimeMillis());

        log.info("[InstanceState] 开始优雅下线 | instance: {}, waitSeconds: {}s, reason: {}",
                getInstanceIdentifier(), waitSeconds, reason);

        return true;
    }

    /**
     * 开始强制下线
     * 立即停止接收请求，不等待流量排空
     *
     * @param reason 下线原因
     * @return 是否成功
     */
    public boolean startForceOffline(String reason) {
        acceptingRequests.set(false);
        draining.set(false);
        this.offlineReason = reason;
        this.drainStartTime.set(System.currentTimeMillis());

        log.warn("[InstanceState] 强制下线 | instance: {}, reason: {}", getInstanceIdentifier(), reason);

        return true;
    }

    /**
     * 恢复上线
     *
     * @return 是否成功恢复
     */
    public boolean online() {
        if (acceptingRequests.compareAndSet(false, true)) {
            draining.set(false);
            drainStartTime.set(0);
            String oldReason = offlineReason;
            offlineReason = null;

            log.info("[InstanceState] 实例已上线 | instance: {}, previousReason: {}",
                    getInstanceIdentifier(), oldReason);
            return true;
        }

        log.info("[InstanceState] 实例已在线，忽略重复上线指令");
        return false;
    }

    /**
     * 检查是否应该接收请求
     * 用于网关过滤器判断是否放行请求
     *
     * @return true - 接收请求; false - 拒绝请求
     */
    public boolean shouldAcceptRequest() {
        return acceptingRequests.get();
    }

    /**
     * 检查排空是否完成
     *
     * @return true - 排空完成; false - 排空中
     */
    public boolean isDrainComplete() {
        if (!draining.get()) {
            return true;
        }

        long elapsed = System.currentTimeMillis() - drainStartTime.get();
        return elapsed >= (drainWaitSeconds * 1000L);
    }

    /**
     * 获取剩余排空时间（毫秒）
     *
     * @return 剩余时间，如果不在排空状态返回 0
     */
    public long getRemainingDrainTimeMs() {
        if (!draining.get()) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - drainStartTime.get();
        long total = drainWaitSeconds * 1000L;
        return Math.max(0, total - elapsed);
    }

    /**
     * 获取本机 IP 地址
     */
    private String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
