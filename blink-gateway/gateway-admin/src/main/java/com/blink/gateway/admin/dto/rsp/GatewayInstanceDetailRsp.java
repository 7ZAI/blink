package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关实例详情响应DTO
 * 提供单个实例的完整监控指标
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class GatewayInstanceDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 实例基本信息 ====================

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
     * URI地址
     */
    private String uri;

    /**
     * 健康状态（UP/DOWN）
     */
    private String healthStatus;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 上报时间戳
     */
    private Long timestamp;

    // ==================== JVM 内存指标 ====================

    /**
     * 已用堆内存（bytes）
     */
    private Long heapUsed;

    /**
     * 最大堆内存（bytes）
     */
    private Long heapMax;

    /**
     * 堆内存使用率（%）
     */
    private Double heapUsagePercent;

    /**
     * 已用非堆内存（bytes）
     */
    private Long nonHeapUsed;

    /**
     * CPU使用率（%）
     */
    private Double cpuUsage;

    /**
     * 内存使用率（%）
     */
    private Double memoryUsage;

    // ==================== GC 统计指标 ====================

    /**
     * 年轻代GC次数
     */
    private Long youngGcCount;

    /**
     * 年轻代GC时间（ms）
     */
    private Long youngGcTime;

    /**
     * 老年代GC次数
     */
    private Long oldGcCount;

    /**
     * 老年代GC时间（ms）
     */
    private Long oldGcTime;

    /**
     * GC总次数
     */
    private Long totalGcCount;

    /**
     * GC总时间（ms）
     */
    private Long totalGcTime;

    // ==================== 线程指标 ====================

    /**
     * 活跃线程数
     */
    private Integer liveThreads;

    /**
     * 峰值线程数
     */
    private Integer peakThreads;

    /**
     * 守护线程数
     */
    private Integer daemonThreads;

    // ==================== HTTP 统计指标 ====================

    /**
     * 请求总数
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
     * 成功率（%）
     */
    private Double successRate;

    /**
     * 平均响应时间（ms）
     */
    private Long avgResponseTime;

    /**
     * P50 响应时间（ms）
     */
    private Long p50ResponseTime;

    /**
     * P95 响应时间（ms）
     */
    private Long p95ResponseTime;

    /**
     * P99 响应时间（ms）
     */
    private Long p99ResponseTime;

    /**
     * 最大响应时间（ms）
     */
    private Long maxResponseTime;

    /**
     * 当前 QPS
     */
    private Integer currentQps;

    /**
     * 活跃连接数
     */
    private Integer activeConnections;
}