package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取状态转换历史请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetCircuitBreakerHistoryReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 熔断器名称
     */
    private String name;

    /**
     * 返回数量限制（默认 20）
     */
    private Integer limit;
}
