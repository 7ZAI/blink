package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取分组实例路由请求
 * 从分组下在线实例获取实际加载的路由配置
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class GetGroupInstanceRoutesReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由分组（必填）
     */
    @NotBlank(message = "路由分组不能为空")
    private String routesGroup;
}