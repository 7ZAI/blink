package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 网关指标历史记录持久化对象
 * 用于存储 gateway-reactive 实例的监控指标历史数据
 *
 * @author binblink
 */
@Data
@TableName("gateway_metrics_history")
public class GatewayMetricsHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 实例 ID
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * CPU 使用率 (%)
     */
    @TableField("cpu_usage")
    private BigDecimal cpuUsage;

    /**
     * 已用内存 (bytes)
     */
    @TableField("memory_used")
    private Long memoryUsed;

    /**
     * 最大内存 (bytes)
     */
    @TableField("memory_max")
    private Long memoryMax;

    /**
     * 请求总数
     */
    @TableField("total_requests")
    private Long totalRequests;

    /**
     * 成功请求数
     */
    @TableField("success_requests")
    private Long successRequests;

    /**
     * 失败请求数
     */
    @TableField("failed_requests")
    private Long failedRequests;

    /**
     * 平均响应时间 (ms)
     */
    @TableField("avg_response_time")
    private Long avgResponseTime;

    /**
     * 健康状态
     */
    @TableField("health_status")
    private String healthStatus;

    /**
     * 熔断器状态
     */
    @TableField("circuit_breaker_state")
    private String circuitBreakerState;

    /**
     * 采集时间
     */
    @TableField("collect_time")
    private LocalDateTime collectTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}