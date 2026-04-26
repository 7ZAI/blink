package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 路由差异对比请求
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class RouteDiffReq {

    /**
     * 路由分组
     */
    @NotBlank(message = "路由分组不能为空")
    private String routesGroup;

    /**
     * 实例ID（可选，不传时自动选择分组下第一个在线实例）
     */
    private String instanceId;
}