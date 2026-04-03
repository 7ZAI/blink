package com.blink.gateway.admin.dto.req;

import com.blink.gateway.admin.dto.req.RouteDefinitionReq;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 保存路由请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class SaveRouteReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由组
     */
    private String routesGroup;

    /**
     * 路由列表
     */
    private List<RouteDefinitionReq> routes;
}