package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * HTTP 请求统计 VO
 *
 * @author binblink
 */
@Data
public class HttpMetricsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 成功率 (%)
     */
    private Double successRate;

    /**
     * 平均响应时间 (ms)
     */
    private Long avgResponseTime;

    /**
     * 采样时间戳
     */
    private Long timestamp;
}