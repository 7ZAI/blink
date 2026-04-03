package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关统计数据响应DTO
 *
 * @author binblink
 */
@Data
public class GatewayStatisticsRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例总数
     */
    private Integer totalInstances;

    /**
     * 健康实例数
     */
    private Integer healthyInstances;

    /**
     * 总请求数
     */
    private Long totalRequests;

    /**
     * 成功请求数
     */
    private Long successRequests;

    /**
     * 失败请求数
     */
    private Long failedRequests;

    /**
     * 平均响应时间(ms)
     */
    private Long avgResponseTime;
}