package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddRouteGroupReq;
import com.blink.gateway.admin.dto.req.DeleteRouteGroupReq;
import com.blink.gateway.admin.dto.req.GetRouteGroupReq;
import com.blink.gateway.admin.dto.req.QueryRouteGroupReq;
import com.blink.gateway.admin.dto.req.UpdateRouteGroupReq;
import com.blink.gateway.admin.dto.rsp.RouteGroupListRsp;
import com.blink.gateway.admin.dto.vo.RouteGroupVO;

import java.util.List;

/**
 * 路由分组服务接口
 *
 * @author binblink
 * @since 2026-04-18
 */
public interface GatewayRouteGroupService {

    /**
     * 分页查询路由分组列表
     *
     * @param req 查询请求参数
     * @return 分组列表响应
     */
    ResponseDTO<RouteGroupListRsp> queryRouteGroupList(QueryRouteGroupReq req);

    /**
     * 获取路由分组详情
     *
     * @param req 请求参数
     * @return 分组详情
     */
    ResponseDTO<RouteGroupVO> getRouteGroupDetail(GetRouteGroupReq req);

    /**
     * 新增路由分组
     *
     * @param req 新增请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> addRouteGroup(AddRouteGroupReq req);

    /**
     * 更新路由分组
     *
     * @param req 更新请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> updateRouteGroup(UpdateRouteGroupReq req);

    /**
     * 删除路由分组
     *
     * @param req 删除请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> deleteRouteGroup(DeleteRouteGroupReq req);

    /**
     * 获取所有启用的分组列表（用于下拉选择）
     *
     * @return 分组列表
     */
    ResponseDTO<List<RouteGroupVO>> getEnabledRouteGroups();
}
