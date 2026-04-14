package com.blink.gateway.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 指标消息 DTO
 * 用于推送到 Redis Stream
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class MetricsMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 实例信息 ====================

    /**
     * 实例ID（格式：serviceId@host:port）
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
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息类型
     *
     * @see MessageType
     */
    private String type;

    // ==================== JVM 指标 ====================

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
     * CPU 使用率（%）
     */
    private Double cpuUsage;

    // ==================== GC 指标 ====================

    /**
     * 年轻代 GC 次数
     */
    private Long youngGcCount;

    /**
     * 年轻代 GC 时间（ms）
     */
    private Long youngGcTime;

    /**
     * 老年代 GC 次数
     */
    private Long oldGcCount;

    /**
     * 老年代 GC 时间（ms）
     */
    private Long oldGcTime;

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

    // ==================== HTTP 指标 ====================

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
     * 平均响应时间（ms）
     */
    private Long avgResponseTime;

    // ==================== HTTP 响应时间分布 ====================

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

    // ==================== HTTP 错误分类 ====================

    /**
     * 4xx 错误数（客户端错误）
     */
    private Long error4xxCount;

    /**
     * 5xx 错误数（服务端错误）
     */
    private Long error5xxCount;

    /**
     * 错误率（%）
     */
    private Double errorRate;

    // ==================== 实时 QPS ====================

    /**
     * 当前 QPS（增量/上报间隔）
     */
    private Integer currentQps;

    // ==================== 健康状态 ====================

    /**
     * 健康状态（UP/DOWN）
     */
    private String healthStatus;

    /**
     * 消息类型常量
     */
    public interface MessageType {
        /**
         * 定时指标上报
         */
        String METRICS = "METRICS";

        /**
         * 实例启动注册
         */
        String REGISTER = "REGISTER";

        /**
         * 实例关闭注销
         */
        String UNREGISTER = "UNREGISTER";
    }
}
