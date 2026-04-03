package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.entity.RouteDefinitionDO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询网关路由响应DTO
 *
 * @author binblink
 */
public class QueryGateWayRoutesRsp extends PageDTO<RouteDefinitionDO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}