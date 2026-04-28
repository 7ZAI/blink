package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新实例配置文件请求
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class UpdateInstanceConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（必填）
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    /**
     * 动态路由模式：redis 或 nacos
     */
    @NotBlank(message = "路由模式不能为空")
    private String routeMode;

    /**
     * 路由分组
     */
    @NotBlank(message = "路由分组不能为空")
    private String routeGroup;

    /**
     * Redis 路由后缀
     */
    private String redisRouteSuffix;

    /**
     * Nacos Data ID
     */
    private String nacosDataId;

    /**
     * Nacos Group
     */
    private String nacosGroup;

    /**
     * 备注
     */
    private String remark;
}
