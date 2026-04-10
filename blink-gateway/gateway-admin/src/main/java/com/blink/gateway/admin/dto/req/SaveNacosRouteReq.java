package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Nacos 路由保存请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class SaveNacosRouteReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Nacos dataId
     */
    private String dataId;

    /**
     * Nacos group
     */
    private String group;

    /**
     * 路由定义列表
     */
    private List<RouteDefinitionReq> routes;
}