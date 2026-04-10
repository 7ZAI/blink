package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;

/**
 * Nacos 路由管理服务接口
 *
 * @author binblink
 */
public interface NacosRouteService {

    /**
     * 查询 Nacos 路由列表
     *
     * @param req 请求参数
     * @return 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req);

    /**
     * 保存 Nacos 路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req);

    /**
     * 删除 Nacos 路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req);
}