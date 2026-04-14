package com.blink.gateway.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关监控配置属性
 *
 * 统一配置项，控制 gateway-admin 的监控消息消费
 *
 * 注意：监控开关已改为从数据库 sys_config 表读取
 * 此类仅作为配置文件默认值的补充
 *
 * @author binblink
 * @since 2026-04-14
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "blink.gateway.monitor")
public class MonitorProperties {

    /**
     * 是否启用网关监控消息消费（配置文件默认值）
     * 实际值从数据库 sys_config 表读取：monitor.enabled
     */
    private Boolean enabled = true;

    /**
     * 历史数据保留天数
     */
    private Integer historyRetentionDays = 7;

    /**
     * CPU 使用率变化阈值（百分比）
     */
    private Integer cpuChangeThreshold = 10;

    /**
     * 堆内存使用率变化阈值（百分比）
     */
    private Integer heapChangeThreshold = 10;

    /**
     * Stream 消费配置
     */
    private StreamConsumeConfig streamConsume = new StreamConsumeConfig();

    @Getter
    @Setter
    public static class StreamConsumeConfig {

        /**
         * Redis Stream Key
         */
        private String streamKey = "blink:gateway:metrics:stream";

        /**
         * 消费者组名称
         */
        private String consumerGroup = "gateway-admin";

        /**
         * 消费者名称前缀
         */
        private String consumerNamePrefix = "consumer-";

        /**
         * 批量读取数量
         */
        private Integer batchSize = 10;

        /**
         * 轮询间隔（毫秒）
         */
        private Long pollIntervalMs = 1000L;
    }
}
