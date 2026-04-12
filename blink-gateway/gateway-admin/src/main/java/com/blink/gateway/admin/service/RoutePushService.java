package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.FullPushRoutesReq;
import com.blink.gateway.admin.dto.req.PushRoutesReq;
import com.blink.gateway.admin.dto.req.QueryInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.QueryPushLogReq;
import com.blink.gateway.admin.dto.req.RollbackPushReq;
import com.blink.gateway.admin.dto.rsp.QueryInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushLogRsp;

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
}