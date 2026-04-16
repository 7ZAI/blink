package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 熔断器实例状态响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerInstanceRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

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
