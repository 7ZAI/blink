package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddRouteGroupReq;
import com.blink.gateway.admin.dto.req.DeleteRouteGroupReq;
import com.blink.gateway.admin.dto.req.GetRouteGroupReq;
import com.blink.gateway.admin.dto.req.QueryRouteGroupReq;
import com.blink.gateway.admin.dto.req.UpdateRouteGroupReq;
import com.blink.gateway.admin.dto.rsp.RouteGroupListRsp;
import com.blink.gateway.admin.dto.vo.RouteGroupVO;
import com.blink.gateway.admin.service.GatewayRouteGroupService;
import com.blink.log.annotation.RecordLog;
import com.blink.log.constant.LogType;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路由分组管理控制器
 * 提供路由分组的增删改查功能
 *
 * @author binblink
 * @since 2026-04-18
 */
@RestController
@RequestMapping("/routeGroup")
public class RouteGroupController {

    @Resource
    private GatewayRouteGroupService gatewayRouteGroupService;

    /**
     * 分页查询路由分组列表
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/queryRouteGroupList")
    public ResponseDTO<RouteGroupListRsp> queryRouteGroupList(@RequestBody @Validated RequestDTO<QueryRouteGroupReq> reqDto) {
        return gatewayRouteGroupService.queryRouteGroupList(reqDto.getBody());
    }

    /**
     * 获取路由分组详情
     *
     * @param reqDto 请求参数
     * @return 分组详情
     */
    @PostMapping("/getRouteGroupDetail")
    public ResponseDTO<RouteGroupVO> getRouteGroupDetail(@RequestBody @Validated RequestDTO<GetRouteGroupReq> reqDto) {
        return gatewayRouteGroupService.getRouteGroupDetail(reqDto.getBody());
    }

    /**
     * 新增路由分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "新增路由分组")
    @PostMapping("/addRouteGroup")
    public ResponseDTO<EmptyBody> addRouteGroup(@RequestBody @Validated RequestDTO<AddRouteGroupReq> reqDto) {
        gatewayRouteGroupService.addRouteGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新路由分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "更新路由分组")
    @PostMapping("/updateRouteGroup")
    public ResponseDTO<EmptyBody> updateRouteGroup(@RequestBody @Validated RequestDTO<UpdateRouteGroupReq> reqDto) {
        gatewayRouteGroupService.updateRouteGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除路由分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "删除路由分组")
    @PostMapping("/deleteRouteGroup")
    public ResponseDTO<EmptyBody> deleteRouteGroup(@RequestBody @Validated RequestDTO<DeleteRouteGroupReq> reqDto) {
        gatewayRouteGroupService.deleteRouteGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取启用状态的分组列表（用于下拉选择）
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/getEnabledRouteGroups")
    public ResponseDTO<List<RouteGroupVO>> getEnabledRouteGroups(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return gatewayRouteGroupService.getEnabledRouteGroups();
    }
}
