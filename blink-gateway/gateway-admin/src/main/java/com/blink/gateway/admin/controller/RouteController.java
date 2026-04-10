package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路由管理控制器
 * 管理网关动态路由配置
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

    /**
     * 查询路由列表
     *
     * @param reqDto 请求参数
     * @return 路由列表
     */
    @PostMapping("/getRouteList")
    public ResponseDTO<QueryGateWayRoutesRsp> getRouteList(@RequestBody @Validated RequestDTO<QueryRouteReq> reqDto) {
        return routeService.getRouteList(reqDto.getBody());
    }

    /**
     * 保存路由
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveRoute")
    public ResponseDTO<EmptyBody> saveRoute(@RequestBody @Validated RequestDTO<SaveRouteReq> reqDto) {
        return routeService.saveRoute(reqDto.getBody());
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
     * 刷新路由缓存
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/refreshRoutes")
    public ResponseDTO<EmptyBody> refreshRoutes(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return routeService.refreshRoutes();
    }

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
}