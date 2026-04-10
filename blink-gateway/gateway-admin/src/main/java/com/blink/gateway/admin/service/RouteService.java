package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;

import java.util.List;

/**
 * 路由管理服务接口
 *
 * @author binblink
 */
public interface RouteService {

    // ========== Redis 路由管理 ==========

    /**
     * 查询 Redis 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getRouteList(QueryRouteReq req);

    /**
     * 保存 Redis 路由
     */
    ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req);

    /**
     * 删除 Redis 路由
     */
    ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req);

    /**
     * 刷新路由缓存
     */
    ResponseDTO<EmptyBody> refreshRoutes();

    // ========== Nacos 路由管理 ==========

    /**
     * 查询 Nacos 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req);

    /**
     * 保存 Nacos 路由
     */
    ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req);

    /**
     * 删除 Nacos 路由
     */
    ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req);

    // ========== 存储方式和实例同步 ==========

    /**
     * 获取支持的存储方式列表
     */
    ResponseDTO<List<StorageModeVO>> getStorageModes();

    /**
     * 获取在线网关实例列表
     */
    ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances();

    /**
     * 同步路由到指定实例
     */
    ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req);
}