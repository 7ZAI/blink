package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 仪表盘数据推送载荷
 * 用于 SSE 推送仪表盘完整数据
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class DashboardDataPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 统计汇总信息
     */
    private StatisticsSummary statistics;

    /**
     * 实例列表
     */
    private List<InstanceInfo> instances;

    /**
     * 最新流量数据点（前端追加到历史数组）
     */
    private TrafficPoint latestTraffic;

    /**
     * 推送时间戳
     */
    private Long timestamp;

    /**
     * 统计汇总信息
     */
    @Data
    public static class StatisticsSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 总实例数
         */
        private Integer totalInstances;

        /**
         * 健康实例数
         */
        private Integer healthyInstances;

        /**
         * 总请求数
         */
        private Long totalRequests;

        /**
         * 成功请求数
         */
        private Long successRequests;

        /**
         * 失败请求数
         */
        private Long failedRequests;

        /**
         * 平均响应时间（毫秒）
         */
        private Long avgResponseTime;

        /**
         * 成功率（百分比字符串，如 "98.5%"）
         */
        private String successRate;
    }

    /**
     * 实例信息
     */
    @Data
    public static class InstanceInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 服务ID
         */
        private String serviceId;

        /**
         * 主机地址
         */
        private String host;

        /**
         * 端口
         */
        private Integer port;

        /**
         * 健康状态：true-健康，false-不健康
         */
        private Boolean healthy;

        /**
         * 健康状态描述（UP/DOWN）
         */
        private String healthStatus;

        /**
         * CPU使用率（%）
         */
        private Double cpuUsage;

        /**
         * 堆内存使用率（%）
         */
        private Double heapUsagePercent;

        /**
         * 总请求数
         */
        private Long totalRequests;

        /**
         * 平均响应时间（毫秒）
         */
        private Long avgResponseTime;
    }

    /**
     * 流量数据点
     */
    @Data
    public static class TrafficPoint implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 时间（格式化的时间字符串，如 "10:30:45"）
         */
        private String time;

        /**
         * 请求数量
         */
        private Long count;

        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;
    }
}