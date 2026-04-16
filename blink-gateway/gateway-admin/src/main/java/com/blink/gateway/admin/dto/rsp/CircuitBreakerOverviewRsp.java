package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器总览响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class CircuitBreakerOverviewRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器列表
     */
    private List<CircuitBreakerSummaryRsp> circuitBreakers;

    /**
     * 熔断器总数
     */
    private Integer totalCircuitBreakers;

    /**
     * OPEN 状态数量
     */
    private Integer openCount;

    /**
     * CLOSED 状态数量
     */
    private Integer closedCount;

    /**
     * HALF_OPEN 状态数量
     */
    private Integer halfOpenCount;

    /**
     * 实例总数
     */
    private Integer totalInstances;

    /**
     * 健康度评分（0-100）
     */
    private Double healthScore;
}
