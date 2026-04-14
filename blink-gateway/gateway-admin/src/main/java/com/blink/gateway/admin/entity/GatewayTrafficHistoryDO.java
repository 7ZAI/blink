package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关流量历史记录持久化对象
 * 用于存储聚合后的流量趋势数据（分钟级、小时级）
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
@TableName("gateway_traffic_history")
public class GatewayTrafficHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 时间桶（分钟/小时级别）
     */
    @TableField("time_bucket")
    private LocalDateTime timeBucket;

    /**
     * 粒度：MINUTE/HOUR
     */
    @TableField("granularity")
    private String granularity;

    /**
     * 请求增量
     */
    @TableField("request_count")
    private Long requestCount;

    /**
     * 成功请求增量
     */
    @TableField("success_count")
    private Long successCount;

    /**
     * 失败请求增量
     */
    @TableField("failed_count")
    private Long failedCount;

    /**
     * 平均响应时间（毫秒）
     */
    @TableField("avg_response_time")
    private Long avgResponseTime;

    /**
     * 峰值QPS（秒级最大值）
     */
    @TableField("peak_qps")
    private Integer peakQps;

    /**
     * P50响应时间（毫秒）
     */
    @TableField("p50_response_time")
    private Long p50ResponseTime;

    /**
     * P95响应时间（毫秒）
     */
    @TableField("p95_response_time")
    private Long p95ResponseTime;

    /**
     * P99响应时间（毫秒）
     */
    @TableField("p99_response_time")
    private Long p99ResponseTime;

    /**
     * 最大响应时间（毫秒）
     */
    @TableField("max_response_time")
    private Long maxResponseTime;

    /**
     * 4xx错误数
     */
    @TableField("error_4xx_count")
    private Long error4xxCount;

    /**
     * 5xx错误数
     */
    @TableField("error_5xx_count")
    private Long error5xxCount;

    /**
     * 错误率（%）
     */
    @TableField("error_rate")
    private Double errorRate;

    /**
     * 实时QPS
     */
    @TableField("current_qps")
    private Integer currentQps;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}