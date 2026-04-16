package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器配置响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerConfigRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 基础配置名称
     */
    private String baseConfig;

    /**
     * 失败率阈值
     */
    private Double failureRateThreshold;

    /**
     * 滑动窗口大小
     */
    private Integer slidingWindowSize;

    /**
     * 最小调用数
     */
    private Integer minimumNumberOfCalls;

    /**
     * 开启状态等待时间（毫秒）
     */
    private Long waitDurationInOpenState;

    /**
     * 半开状态允许调用数
     */
    private Integer permittedNumberOfCallsInHalfOpenState;

    /**
     * 滑动窗口类型
     */
    private String slidingWindowType;

    /**
     * 慢调用率阈值
     */
    private Double slowCallRateThreshold;

    /**
     * 慢调用持续时间阈值（毫秒）
     */
    private Long slowCallDurationThreshold;

    /**
     * 自动从 OPEN 转换到 HALF_OPEN
     */
    private Boolean automaticTransitionFromOpenToHalfOpenEnabled;

    /**
     * 实例状态列表
     */
    private List<CircuitBreakerStatusRsp> instanceStatuses;
}