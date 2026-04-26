package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 从实例同步路由请求
 * 增量同步模式：新增本地没有的路由，更新本地已有的路由
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class SyncRoutesFromInstanceReq {

    /**
     * 实例ID（可选，不传时自动根据分组查找在线实例）
     * 格式：gateway-app:host:port
     */
    private String instanceId;

    /**
     * 目标路由分组
     */
    @NotBlank(message = "路由分组不能为空")
    private String routesGroup;
}