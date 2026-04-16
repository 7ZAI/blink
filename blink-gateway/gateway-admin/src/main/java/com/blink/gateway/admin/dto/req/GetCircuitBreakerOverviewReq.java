package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取熔断器总览请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerOverviewReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（可选，不传则返回聚合视图）
     */
    private String instanceId;
}
