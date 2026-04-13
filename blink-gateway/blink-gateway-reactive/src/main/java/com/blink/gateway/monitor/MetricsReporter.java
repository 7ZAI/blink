package com.blink.gateway.monitor;

import com.blink.gateway.monitor.dto.MetricsMessage;

/**
 * 指标上报服务接口
 * 负责采集本地指标并异步推送到 Redis Stream
 *
 * @author binblink
 * @since 2026-04-14
 */
public interface MetricsReporter {

    /**
     * 采集并上报指标
     * 异步执行，不阻塞主线程
     */
    void reportMetrics();

    /**
     * 发送实例注册消息
     * 实例启动时调用
     */
    void sendRegisterMessage();

    /**
     * 发送实例注销消息
     * 实例关闭时调用
     */
    void sendUnregisterMessage();
}
