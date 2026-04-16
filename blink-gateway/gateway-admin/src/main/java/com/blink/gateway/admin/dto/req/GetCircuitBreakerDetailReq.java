package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取熔断器详情请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 实例ID（可选，不传则返回所有实例）
     */
    private String instanceId;
}
