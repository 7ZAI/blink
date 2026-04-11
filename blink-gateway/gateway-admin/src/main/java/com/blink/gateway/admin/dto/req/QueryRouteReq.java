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
     * 路由名称（模糊查询）
     */
    private String routeName;

    /**
     * 路由组
     */
    private String routesGroup;

    /**
     * 存储方式：redis/nacos
     */
    private String storageMode;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

    /**
     * 目标URI（模糊查询）
     */
    private String uri;
}