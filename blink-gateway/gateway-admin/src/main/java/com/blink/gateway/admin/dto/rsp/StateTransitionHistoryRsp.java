package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 状态转换历史响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class StateTransitionHistoryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 转换前状态
     */
    private String fromState;

    /**
     * 转换后状态
     */
    private String toState;

    /**
     * 转换时间戳
     */
    private Long timestamp;

    /**
     * 转换原因
     */
    private String reason;

    /**
     * 失败率
     */
    private Double failureRate;

    /**
     * 调用次数
     */
    private Integer numberOfCalls;
}
