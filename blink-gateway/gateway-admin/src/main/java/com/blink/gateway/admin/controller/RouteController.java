package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.BatchUpdateStatusReq;
import com.blink.gateway.admin.dto.req.CloneRouteReq;
import com.blink.gateway.admin.dto.req.ConfirmPushReq;
import com.blink.gateway.admin.dto.req.ExportRoutesReq;
import com.blink.gateway.admin.dto.req.FullPushRoutesReq;
import com.blink.gateway.admin.dto.req.GetInstanceRoutesFromActuatorReq;
import com.blink.gateway.admin.dto.req.GetGroupInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.GetLatestPushReq;
import com.blink.gateway.admin.dto.req.ImportRoutesReq;
import com.blink.gateway.admin.dto.req.PushRoutesReq;
import com.blink.gateway.admin.dto.req.QueryInstancePushHistoryReq;
import com.blink.gateway.admin.dto.req.QueryStorageRoutesReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryPushLogReq;
import com.blink.gateway.admin.dto.req.QueryPushStatusReq;
import com.blink.gateway.admin.dto.req.QueryRouteInstancePushStatusReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteHistoryReq;
import com.blink.gateway.admin.dto.req.RollbackPushReq;
import com.blink.gateway.admin.dto.req.RollbackRouteReq;
import com.blink.gateway.admin.dto.req.RouteDiffReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.req.SyncRoutesFromInstanceReq;
import com.blink.gateway.admin.dto.req.UpdateRouteReq;
import com.blink.gateway.admin.dto.req.VerifyPushResultReq;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.InstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryStorageRoutesRsp;
import com.blink.gateway.admin.dto.rsp.ImportRoutesRsp;
import com.blink.gateway.admin.dto.rsp.RouteDiffRsp;
import com.blink.gateway.admin.dto.rsp.VerifyPushResultRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushLogRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushStatusRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteHistoryRsp;
import com.blink.gateway.admin.dto.rsp.RouteInstancePushStatusRsp;
import com.blink.gateway.admin.dto.rsp.RoutesGroupStatsRsp;
import com.blink.gateway.admin.dto.rsp.SyncRoutesFromInstanceRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RoutePushService;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路由管理控制器
 * 管理网关动态路由配置
 * 数据库为主存储 + Redis/Nacos 为运行时缓存
 *
 * @author binblink
 */
@RestController
@RequestMapping("/route")
public class RouteController {

    @Resource
    private RouteService routeService;

    @Resource
    private NacosRouteService nacosRouteService;

    @Resource
    private RoutePushService routePushService;

    // ========== Redis/数据库 路由管理 ==========

    /**
     * 查询路由列表（从数据库）
     *
     * @param reqDto 请求参数
     * @return 路由列表
     */
    @PostMapping("/getRouteList")
    public ResponseDTO<QueryRouteRsp> getRouteList(@RequestBody @Validated RequestDTO<QueryRouteReq> reqDto) {
        return routeService.getRouteList(reqDto.getBody());
    }

    /**
     * 获取路由详情
     *
     * @param reqDto 请求参数（body 包含 routeId）
     * @return 路由详情
     */
    @PostMapping("/getRouteDetail")
    public ResponseDTO<GaRouteDO> getRouteDetail(@RequestBody RequestDTO<String> reqDto) {
        return routeService.getRouteDetail(reqDto.getBody());
    }

    /**
     * 保存路由（新增）
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveRoute")
    public ResponseDTO<EmptyBody> saveRoute(@RequestBody @Validated RequestDTO<SaveRouteReq> reqDto) {
        return routeService.saveRoute(reqDto.getBody());
    }

    /**
     * 更新路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/updateRoute")
    public ResponseDTO<EmptyBody> updateRoute(@RequestBody @Validated RequestDTO<UpdateRouteReq> reqDto) {
        return routeService.updateRoute(reqDto.getBody());
    }

    /**
     * 删除路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/deleteRoute")
    public ResponseDTO<EmptyBody> deleteRoute(@RequestBody @Validated RequestDTO<DeleteRouteReq> reqDto) {
        return routeService.deleteRoute(reqDto.getBody());
    }

    /**
     * 查询路由变更历史
     *
     * @param reqDto 请求参数
     * @return 历史记录列表
     */
    @PostMapping("/getRouteHistory")
    public ResponseDTO<QueryRouteHistoryRsp> getRouteHistory(@RequestBody @Validated RequestDTO<QueryRouteHistoryReq> reqDto) {
        return routeService.getRouteHistory(reqDto.getBody());
    }

    /**
     * 回滚路由到指定历史版本
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/rollbackRoute")
    public ResponseDTO<EmptyBody> rollbackRoute(@RequestBody @Validated RequestDTO<RollbackRouteReq> reqDto) {
        return routeService.rollbackRoute(reqDto.getBody());
    }

    /**
     * 刷新路由缓存
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/refreshRoutes")
    public ResponseDTO<EmptyBody> refreshRoutes(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return routeService.refreshRoutes();
    }

    // ========== 存储方式和实例同步 ==========

    /**
     * 获取支持的存储方式列表
     *
     * @param reqDto 请求参数
     * @return 存储方式列表
     */
    @PostMapping("/getStorageModes")
    public ResponseDTO<List<StorageModeVO>> getStorageModes(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return routeService.getStorageModes();
    }

    /**
     * 获取在线网关实例列表
     *
     * @param reqDto 请求参数
     * @return 在线网关实例列表
     */
    @PostMapping("/getOnlineGatewayInstances")
    public ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return routeService.getOnlineGatewayInstances();
    }

    /**
     * 同步路由到指定实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/syncRoutesToInstances")
    public ResponseDTO<EmptyBody> syncRoutesToInstances(@RequestBody @Validated RequestDTO<SyncRoutesReq> reqDto) {
        return routeService.syncRoutesToInstances(reqDto.getBody());
    }

    /**
     * 查询路由推送状态
     *
     * @param reqDto 请求参数
     * @return 推送状态列表
     */
    @PostMapping("/getPushStatus")
    public ResponseDTO<QueryPushStatusRsp> getPushStatus(@RequestBody RequestDTO<QueryPushStatusReq> reqDto) {
        return routeService.getPushStatus(reqDto.getBody());
    }

    /**
     * 批量更新路由状态
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/batchUpdateStatus")
    public ResponseDTO<EmptyBody> batchUpdateStatus(@RequestBody @Validated RequestDTO<BatchUpdateStatusReq> reqDto) {
        return routeService.batchUpdateStatus(reqDto.getBody());
    }

    /**
     * 查询路由分组统计
     *
     * @param reqDto 请求参数
     * @return 分组统计列表
     */
    @PostMapping("/getRoutesGroupStats")
    public ResponseDTO<RoutesGroupStatsRsp> getRoutesGroupStats(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return routeService.getRoutesGroupStats();
    }

    /**
     * 导出路由配置
     *
     * @param reqDto 请求参数
     * @return JSON格式路由配置
     */
    @PostMapping("/exportRoutes")
    public ResponseDTO<String> exportRoutes(@RequestBody RequestDTO<ExportRoutesReq> reqDto) {
        return routeService.exportRoutes(reqDto.getBody());
    }

    /**
     * 导入路由配置
     *
     * @param reqDto 请求参数
     * @return 导入结果
     */
    @PostMapping("/importRoutes")
    public ResponseDTO<ImportRoutesRsp> importRoutes(@RequestBody @Validated RequestDTO<ImportRoutesReq> reqDto) {
        return routeService.importRoutes(reqDto.getBody());
    }

    /**
     * 克隆路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/cloneRoute")
    public ResponseDTO<EmptyBody> cloneRoute(@RequestBody @Validated RequestDTO<CloneRouteReq> reqDto) {
        return routeService.cloneRoute(reqDto.getBody());
    }

    /**
     * 从实例同步路由到本地数据库
     * 增量同步模式：新增本地没有的路由，更新本地已有的路由，不删除本地已有路由
     *
     * @param reqDto 请求参数
     * @return 同步结果
     */
    @PostMapping("/syncRoutesFromInstance")
    public ResponseDTO<SyncRoutesFromInstanceRsp> syncRoutesFromInstance(
        @RequestBody @Validated RequestDTO<SyncRoutesFromInstanceReq> reqDto) {
        return routeService.syncRoutesFromInstance(reqDto.getBody());
    }

    /**
     * 获取路由差异对比
     * 对比仓库路由与实例路由的差异
     *
     * @param reqDto 请求参数
     * @return 差异对比结果
     */
    @PostMapping("/getRouteDiff")
    public ResponseDTO<RouteDiffRsp> getRouteDiff(@RequestBody @Validated RequestDTO<RouteDiffReq> reqDto) {
        return routeService.getRouteDiff(reqDto.getBody());
    }

    /**
     * 获取分组下实例的实际路由
     * 从分组下第一个在线实例通过 Actuator 获取路由配置
     *
     * @param reqDto 请求参数
     * @return 实例路由响应
     */
    @PostMapping("/getGroupInstanceRoutes")
    public ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(
        @RequestBody @Validated RequestDTO<GetGroupInstanceRoutesReq> reqDto) {
        return routeService.getGroupInstanceRoutes(reqDto.getBody());
    }

    // ========== Nacos 路由管理 ==========

    /**
     * 查询 Nacos 路由列表
     *
     * @param reqDto 请求参数
     * @return Nacos 路由列表
     */
    @PostMapping("/getNacosRouteList")
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(@RequestBody @Validated RequestDTO<QueryNacosRouteReq> reqDto) {
        return nacosRouteService.getNacosRouteList(reqDto.getBody());
    }

    /**
     * 保存 Nacos 路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveNacosRoute")
    public ResponseDTO<EmptyBody> saveNacosRoute(@RequestBody @Validated RequestDTO<SaveNacosRouteReq> reqDto) {
        return nacosRouteService.saveNacosRoute(reqDto.getBody());
    }

    /**
     * 删除 Nacos 路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/deleteNacosRoute")
    public ResponseDTO<EmptyBody> deleteNacosRoute(@RequestBody @Validated RequestDTO<DeleteNacosRouteReq> reqDto) {
        return nacosRouteService.deleteNacosRoute(reqDto.getBody());
    }

    // ========== 路由推送管理 ==========

    /**
     * 推送路由到实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/pushRoutes")
    public ResponseDTO<EmptyBody> pushRoutes(@RequestBody @Validated RequestDTO<PushRoutesReq> reqDto) {
        return routePushService.pushRoutes(reqDto.getBody());
    }

    /**
     * 全量推送路由
     * 一键推送指定分组下所有启用状态路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/fullPushRoutes")
    public ResponseDTO<EmptyBody> fullPushRoutes(@RequestBody @Validated RequestDTO<FullPushRoutesReq> reqDto) {
        return routePushService.fullPushRoutes(reqDto.getBody());
    }

    /**
     * 查询推送历史
     *
     * @param reqDto 请求参数
     * @return 推送历史列表
     */
    @PostMapping("/getPushHistory")
    public ResponseDTO<QueryPushLogRsp> getPushHistory(@RequestBody @Validated RequestDTO<QueryPushLogReq> reqDto) {
        return routePushService.getPushHistory(reqDto.getBody());
    }

    /**
     * 查询配置中心路由
     * 从 Redis/Nacos 配置中心查询已推送的路由配置
     *
     * @param reqDto 请求参数
     * @return 配置中心路由列表
     */
    @PostMapping("/getStorageRoutes")
    public ResponseDTO<QueryStorageRoutesRsp> getStorageRoutes(@RequestBody RequestDTO<QueryStorageRoutesReq> reqDto) {
        return routePushService.getStorageRoutes(reqDto.getBody());
    }

    /**
     * 查询实例推送历史
     *
     * @param reqDto 请求参数
     * @return 推送历史列表
     */
    @PostMapping("/getInstancePushHistory")
    public ResponseDTO<QueryPushLogRsp> getInstancePushHistory(@RequestBody @Validated RequestDTO<QueryInstancePushHistoryReq> reqDto) {
        return routePushService.getInstancePushHistory(reqDto.getBody());
    }

    /**
     * 获取实例最新推送记录
     *
     * @param reqDto 请求参数（包含实例ID）
     * @return 最新推送记录（单条，无记录时返回 null）
     */
    @PostMapping("/getLatestPush")
    public ResponseDTO<GaRoutePushLogDO> getLatestPush(@RequestBody @Validated RequestDTO<GetLatestPushReq> reqDto) {
        return routePushService.getLatestPush(reqDto.getBody());
    }

    /**
     * 回滚推送
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/rollbackPush")
    public ResponseDTO<EmptyBody> rollbackPush(@RequestBody @Validated RequestDTO<RollbackPushReq> reqDto) {
        return routePushService.rollbackPush(reqDto.getBody());
    }

    /**
     * 从网关实例获取实际加载的路由
     * 通过 HTTP Actuator 端点获取实例内存中的路由定义
     *
     * @param reqDto 请求参数
     * @return 实例路由响应
     */
    @PostMapping("/getInstanceRoutesFromActuator")
    public ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(
        @RequestBody @Validated RequestDTO<GetInstanceRoutesFromActuatorReq> reqDto) {
        return routePushService.getInstanceRoutesFromActuator(reqDto.getBody());
    }

    /**
     * 验证推送结果
     * 比较推送的路由快照与实例实际路由配置
     *
     * @param reqDto 请求参数
     * @return 验证结果
     */
    @PostMapping("/verifyPushResult")
    public ResponseDTO<VerifyPushResultRsp> verifyPushResult(@RequestBody @Validated RequestDTO<VerifyPushResultReq> reqDto) {
        return routePushService.verifyPushResult(reqDto.getBody());
    }

    /**
     * 查询路由实例推送状态
     * 返回每个路由在各实例上的推送状态统计
     *
     * @param reqDto 请求参数
     * @return 推送状态列表
     */
    @PostMapping("/getRouteInstancePushStatus")
    public ResponseDTO<List<RouteInstancePushStatusRsp>> getRouteInstancePushStatus(
        @RequestBody @Validated RequestDTO<QueryRouteInstancePushStatusReq> reqDto) {
        return routePushService.getRouteInstancePushStatus(reqDto.getBody());
    }

    /**
     * 确认推送结果
     * 用户确认推送已生效
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/confirmPush")
    public ResponseDTO<EmptyBody> confirmPush(@RequestBody @Validated RequestDTO<ConfirmPushReq> reqDto) {
        return routePushService.confirmPush(reqDto.getBody());
    }
}