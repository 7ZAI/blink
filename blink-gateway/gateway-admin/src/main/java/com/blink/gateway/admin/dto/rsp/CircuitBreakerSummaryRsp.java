package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器汇总响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerSummaryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
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
     * 最小调用次数
     */
    private Integer minimumNumberOfCalls;

    /**
     * 开启状态等待时间（秒）
     */
    private Long waitDurationInOpenState;

    /**
     * CLOSED 状态实例数
     */
    private Integer closedCount;

    /**
     * OPEN 状态实例数
     */
    private Integer openCount;

    /**
     * HALF_OPEN 状态实例数
     */
    private Integer halfOpenCount;

    /**
     * 实例详情列表（聚合视图时返回）
     */
    private List<CircuitBreakerInstanceRsp> instances;
}
