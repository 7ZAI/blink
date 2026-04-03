package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
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
}