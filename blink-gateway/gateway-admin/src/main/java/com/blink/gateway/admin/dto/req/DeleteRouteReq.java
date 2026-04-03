package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 删除路由请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class DeleteRouteReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由组
     */
    private String routesGroup;

    /**
     * 路由ID列表
     */
    private List<String> routeIds;
}