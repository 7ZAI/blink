package com.blink.base.service;

import com.blink.base.dto.rsp.QueryGateWayRoutesRspDTO;
import com.blink.base.entity.RouteDefinitionDO;
import com.blink.framework.common.data.EmptyBody;

import java.util.List;

/**
 * <p>
 *  网关路由管理 动态路由实现
 * </p>
 *
 * @author binblink
 * @module blink
 */
public interface GateWayRoutesService {

    void saveRoute(List<RouteDefinitionDO> body);

    void deleteRoute(List<String> body);


    QueryGateWayRoutesRspDTO getRouteslList(EmptyBody body);
}
