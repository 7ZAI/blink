package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关指标视图对象
 *
 * @author binblink
 */
@Data
public class GatewayMetricsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * CPU 使用率 (%)
     */
    private Double cpuUsage;

    /**
     * 内存使用率 (%)
     */
    private Double memoryUsage;

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
     * 平均响应时间 (ms)
     */
    private Long avgResponseTime;

    /**
     * 当前连接数
     */
    private Integer activeConnections;

    /**
     * 采样时间
     */
    private Long timestamp;
}