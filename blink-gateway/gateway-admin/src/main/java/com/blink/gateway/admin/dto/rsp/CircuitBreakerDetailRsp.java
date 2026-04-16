package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 熔断器详情响应
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class CircuitBreakerDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置信息
     */
    private CircuitBreakerConfigRsp config;

    /**
     * 实例状态列表
     */
    private List<CircuitBreakerInstanceRsp> instances;

    /**
     * 状态转换历史
     */
    private List<StateTransitionHistoryRsp> history;

    /**
     * 趋势数据（最近 30 分钟）
     */
    private List<TrendDataRsp> trend;
}
