package com.blink.gateway.admin.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 流量数据点VO
 *
 * @author binblink
 * @since 2026-04-14
 */
@Getter
@Setter
public class TrafficPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间（格式化字符串，如 "10:30:00"）
     */
    private String time;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 请求数量（增量）
     */
    private Long count;

    /**
     * 成功请求数
     */
    private Long successCount;

    /**
     * 失败请求数
     */
    private Long failedCount;

    /**
     * 峰值 QPS（仅分钟级数据有）
     */
    private Integer peakQps;
}