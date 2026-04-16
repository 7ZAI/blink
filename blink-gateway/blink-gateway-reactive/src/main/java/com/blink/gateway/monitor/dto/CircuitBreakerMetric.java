package com.blink.gateway.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单个熔断器指标 DTO
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerMetric implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 状态：CLOSED/OPEN/HALF_OPEN
     */
    private String state;

    /**
     * 失败率（%）
     */
    private Double failureRate;

    /**
     * 慢调用率（%）
     */
    private Double slowCallRate;

    /**
     * 总调用次数
     */
    private Integer numberOfCalls;

    /**
     * 失败调用次数
     */
    private Integer numberOfFailedCalls;

    /**
     * 慢调用次数
     */
    private Integer numberOfSlowCalls;

    /**
     * 成功调用次数
     */
    private Integer numberOfSuccessfulCalls;

    /**
     * 状态转换时间戳
     */
    private Long stateTransitionTime;

    /**
     * 指标采集时间戳
     */
    private Long timestamp;
}
