package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CloneRouteReq;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.BatchUpdateStatusReq;
import com.blink.gateway.admin.dto.req.ExportRoutesReq;
import com.blink.gateway.admin.dto.req.GetGroupInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.ImportRoutesReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryPushStatusReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteHistoryReq;
import com.blink.gateway.admin.dto.req.RollbackRouteReq;
import com.blink.gateway.admin.dto.req.RouteDiffReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesFromInstanceReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.req.UpdateRouteReq;
import com.blink.gateway.admin.dto.rsp.DiffStats;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushStatusRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteHistoryRsp;
import com.blink.gateway.admin.dto.rsp.ImportRoutesRsp;
import com.blink.gateway.admin.dto.rsp.RouteDiffRsp;
import com.blink.gateway.admin.dto.rsp.SyncRoutesFromInstanceRsp;
import com.blink.gateway.admin.dto.rsp.RoutesGroupStatsRsp;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.entity.GaRouteDO;

import java.util.List;

/**
 * 路由管理服务接口
 * 数据库为主存储 + Redis/Nacos 为运行时缓存
 *
 * @author binblink
 */
public interface RouteService {

    // ========== Redis 路由管理（数据库存储） ==========

    /**
     * 查询路由列表（从数据库）
     */
    ResponseDTO<QueryRouteRsp> getRouteList(QueryRouteReq req);

    /**
     * 获取路由详情
     */
    ResponseDTO<GaRouteDO> getRouteDetail(String routeId);

    /**
     * 保存路由（新增）
     * 写入数据库 + 同步到 Redis/Nacos + 发送 Stream 消息
     */
    ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req);

    /**
     * 更新路由
     * 更新数据库 + 记录历史 + 同步到 Redis/Nacos + 发送 Stream 消息
     */
    ResponseDTO<EmptyBody> updateRoute(UpdateRouteReq req);

    /**
     * 删除路由
     * 删除数据库 + 记录历史 + 删除 Redis/Nacos + 发送 Stream 消息
     */
    ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req);

    /**
     * 查询路由变更历史
     */
    ResponseDTO<QueryRouteHistoryRsp> getRouteHistory(QueryRouteHistoryReq req);

    /**
     * 回滚路由到指定历史版本
     */
    ResponseDTO<EmptyBody> rollbackRoute(RollbackRouteReq req);

    /**
     * 刷新路由缓存
     * 发送 Stream 消息通知网关刷新
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

    /**
     * 查询路由推送状态
     *
     * @param req 查询请求
     * @return 推送状态列表
     */
    ResponseDTO<QueryPushStatusRsp> getPushStatus(QueryPushStatusReq req);

    /**
     * 批量更新路由状态
     *
     * @param req 批量更新请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> batchUpdateStatus(BatchUpdateStatusReq req);

    /**
     * 查询路由分组统计
     *
     * @return 分组统计列表
     */
    ResponseDTO<RoutesGroupStatsRsp> getRoutesGroupStats();

    /**
     * 导出路由配置
     *
     * @param req 导出请求
     * @return JSON格式路由配置
     */
    ResponseDTO<String> exportRoutes(ExportRoutesReq req);

    /**
     * 导入路由配置
     *
     * @param req 导入请求
     * @return 导入结果
     */
    ResponseDTO<ImportRoutesRsp> importRoutes(ImportRoutesReq req);

    /**
     * 克隆路由
     *
     * @param req 克隆请求
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> cloneRoute(CloneRouteReq req);

    /**
     * 从实例同步路由到本地数据库
     * 增量同步模式：新增本地没有的路由，更新本地已有的路由
     *
     * @param req 同步请求（instanceId, routesGroup）
     * @return 同步结果（新增数量、更新数量）
     */
    ResponseDTO<SyncRoutesFromInstanceRsp> syncRoutesFromInstance(SyncRoutesFromInstanceReq req);

    /**
     * 获取路由差异对比
     * 对比仓库路由与实例路由的差异
     *
     * @param req 差异对比请求（routesGroup, instanceId可选）
     * @return 差异对比结果
     */
    ResponseDTO<RouteDiffRsp> getRouteDiff(RouteDiffReq req);

    /**
     * 获取分组下实例的实际路由
     * 从分组下第一个在线实例通过 Actuator 获取路由配置
     *
     * @param req 请求参数（包含路由分组）
     * @return 实例路由响应
     */
    ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(GetGroupInstanceRoutesReq req);
}