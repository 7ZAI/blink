package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 熔断器状态响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerStatusRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 状态：CLOSED, OPEN, HALF_OPEN
     */
    private String state;

    /**
     * 失败率
     */
    private Double failureRate;

    /**
     * 慢调用率
     */
    private Double slowCallRate;

    /**
     * 调用总数
     */
    private Integer numberOfCalls;

    /**
     * 成功调用数
     */
    private Integer numberOfSuccessfulCalls;

    /**
     * 失败调用数
     */
    private Integer numberOfFailedCalls;

    /**
     * 慢调用数
     */
    private Integer numberOfSlowCalls;

    /**
     * 时间戳
     */
    private Long timestamp;
}