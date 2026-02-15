package com.blink.base.service;

import com.blink.base.dto.req.AddRoutesReq;
import com.blink.base.dto.rsp.QueryGateWayRoutesRsp;
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

    void saveRoute(AddRoutesReq addRoutesReq);

    void deleteRoute(List<String> body);


    QueryGateWayRoutesRsp getRouteslList(EmptyBody body);
}
