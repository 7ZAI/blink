package com.blink.gateway.admin.sse;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 实例状态推送载荷
 * 用于 SSE 推送实例状态变化
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class InstanceStatusPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例列表（摘要信息）
     */
    private List<InstanceSummary> instances;

    /**
     * 汇总统计
     */
    private InstanceSummaryStats stats;

    /**
     * 是否有状态变化
     */
    private Boolean hasChange;

    /**
     * 变化的实例ID列表（用于前端高亮或特殊处理）
     */
    private List<String> changedInstanceIds;

    /**
     * 实例摘要信息
     */
    @Data
    public static class InstanceSummary implements Serializable {
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
         * 状态（0-在线 1-离线 2-下线）
         */
        private Integer status;

        /**
         * 健康状态（UP/DOWN）
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
         * 采集时间戳
         */
        private Long timestamp;
    }

    /**
     * 汇总统计
     */
    @Data
    public static class InstanceSummaryStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 总实例数
         */
        private Integer total;

        /**
         * 在线实例数
         */
        private Integer online;

        /**
         * 健康实例数
         */
        private Integer healthy;

        /**
         * 平均CPU使用率（%）
         */
        private Double avgCpuUsage;
    }
}
