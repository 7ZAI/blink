package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 查询流量历史请求DTO
 *
 * @author binblink
 * @since 2026-04-14
 */
@Getter
@Setter
public class GetTrafficHistoryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开始时间（Unix 时间戳，毫秒）
     */
    private Long startTime;

    /**
     * 结束时间（Unix 时间戳，毫秒）
     */
    private Long endTime;

    /**
     * 数据粒度：MINUTE/HOUR
     * 默认 MINUTE
     */
    private String granularity;
}