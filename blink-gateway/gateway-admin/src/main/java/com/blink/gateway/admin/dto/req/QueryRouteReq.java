package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 查询路由请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryRouteReq extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 路由组
     */
    private String routesGroup;
}