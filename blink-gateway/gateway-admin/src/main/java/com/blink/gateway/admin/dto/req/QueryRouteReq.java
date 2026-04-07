package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * 查询路由请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryRouteReq extends Page {

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 路由组
     */
    private String routesGroup;
}