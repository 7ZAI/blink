package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 获取网关指标请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class GetGatewayMetricsReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（可选，不传则返回所有实例指标）
     */
    private String instanceId;
}