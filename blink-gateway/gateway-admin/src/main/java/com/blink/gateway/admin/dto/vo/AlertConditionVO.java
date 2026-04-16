package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 告警条件 VO
 *
 * JSON 结构，用于解析 conditions 字段
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AlertConditionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 指标名称
     * 支持: cpuUsage, memoryUsage, p95ResponseTime, p99ResponseTime, errorRate
     */
    private String metricName;

    /**
     * 操作符: gt(>), lt(<), gte(>=), lte(<=)
     */
    private String operator;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 持续时间 (分钟)
     */
    private Integer durationMinutes;
}