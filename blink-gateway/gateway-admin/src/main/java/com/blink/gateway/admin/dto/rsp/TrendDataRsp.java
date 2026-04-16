package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 趋势数据响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class TrendDataRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 失败率
     */
    private Double failureRate;

    /**
     * 慢调用率
     */
    private Double slowCallRate;

    /**
     * 调用次数
     */
    private Integer numberOfCalls;
}
