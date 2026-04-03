package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;

/**
 * 路由管理服务接口
 *
 * @author binblink
 */
public interface RouteService {

    /**
     * 查询路由列表
     *
     * @param req 请求参数
     * @return 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getRouteList(QueryRouteReq req);

    /**
     * 保存路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req);

    /**
     * 删除路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req);

    /**
     * 刷新路由缓存
     *
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> refreshRoutes();
}