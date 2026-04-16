package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.ConfirmPushReq;
import com.blink.gateway.admin.dto.req.FullPushRoutesReq;
import com.blink.gateway.admin.dto.req.GetInstanceRoutesFromActuatorReq;
import com.blink.gateway.admin.dto.req.GetLatestPushReq;
import com.blink.gateway.admin.dto.req.PushRoutesReq;
import com.blink.gateway.admin.dto.req.GetLatestPushReq;
import com.blink.gateway.admin.dto.req.QueryInstancePushHistoryReq;
import com.blink.gateway.admin.dto.req.QueryInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.QueryPushLogReq;
import com.blink.gateway.admin.dto.req.QueryRouteInstancePushStatusReq;
import com.blink.gateway.admin.dto.req.RollbackPushReq;
import com.blink.gateway.admin.dto.req.VerifyPushResultReq;
import com.blink.gateway.admin.dto.rsp.InstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushLogRsp;
import com.blink.gateway.admin.dto.rsp.RouteInstancePushStatusRsp;
import com.blink.gateway.admin.dto.rsp.VerifyPushResultRsp;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;

import java.util.List;

/**
 * 路由推送服务接口
 * 管理路由从仓库推送到实例
 *
 * @author binblink
 * @since 2026-04-11
 */
public interface RoutePushService {

    /**
     * 推送路由到实例
     * 从仓库路由表查询配置，推送到 Redis/Nacos，并记录推送历史
     *
     * @param req 推送请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> pushRoutes(PushRoutesReq req);

    /**
     * 查询推送历史
     *
     * @param req 查询请求
     * @return 推送历史列表
     */
    ResponseDTO<QueryPushLogRsp> getPushHistory(QueryPushLogReq req);

    /**
     * 查询实例当前路由
     * 从 Redis/Nacos 实时查询实例当前运行的路由配置
     *
     * @param req 查询请求
     * @return 实例路由列表
     */
    ResponseDTO<QueryInstanceRoutesRsp> getInstanceRoutes(QueryInstanceRoutesReq req);

    /**
     * 回滚推送
     * 从推送记录中恢复历史路由配置
     *
     * @param req 回滚请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> rollbackPush(RollbackPushReq req);

    /**
     * 全量推送路由
     * 一键推送指定分组下所有启用状态路由
     *
     * @param req 全量推送请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> fullPushRoutes(FullPushRoutesReq req);

    /**
     * 查询实例推送历史
     * 根据实例ID查询相关的推送记录
     *
     * @param req 查询请求
     * @return 推送历史列表
     */
    ResponseDTO<QueryPushLogRsp> getInstancePushHistory(QueryInstancePushHistoryReq req);

    /**
     * 从网关实例获取实际加载的路由
     * 通过 HTTP Actuator 端点获取实例内存中的路由定义
     *
     * @param req 请求参数
     * @return 实例路由响应
     */
    ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(GetInstanceRoutesFromActuatorReq req);

    /**
     * 获取实例最新推送记录
     *
     * @param req 请求参数
     * @return 最新推送记录
     */
    ResponseDTO<GaRoutePushLogDO> getLatestPush(GetLatestPushReq req);

    /**
     * 验证推送结果
     * 比较推送的路由快照与实例实际路由配置
     *
     * @param req 验证请求
     * @return 验证结果
     */
    ResponseDTO<VerifyPushResultRsp> verifyPushResult(VerifyPushResultReq req);

    /**
     * 查询路由实例推送状态
     * 返回每个路由在各实例上的推送状态统计
     *
     * @param req 查询请求
     * @return 推送状态列表
     */
    ResponseDTO<List<RouteInstancePushStatusRsp>> getRouteInstancePushStatus(QueryRouteInstancePushStatusReq req);

    /**
     * 确认推送结果
     * 用户确认推送已生效，更新确认状态
     *
     * @param req 确认请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> confirmPush(ConfirmPushReq req);
}