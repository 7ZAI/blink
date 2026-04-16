package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;

/**
 * 从实例获取路由请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetInstanceRoutesFromActuatorReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID，格式：gateway-app:host:port
     */
    private String instanceId;
}
