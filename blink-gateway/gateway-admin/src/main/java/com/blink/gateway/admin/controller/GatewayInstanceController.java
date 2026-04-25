package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SwitchInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.log.annotation.RecordLog;
import com.blink.log.constant.LogType;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关实例管理控制器
 * 提供网关实例的上下线管理、实例详情查询等功能
 *
 * @author binblink
 */
@RestController
@RequestMapping("/gatewayInstance")
public class GatewayInstanceController {

    @Resource
    private GatewayInstanceService gatewayInstanceService;

    /**
     * 获取网关实例列表（已废弃）
     *
     * @deprecated 该接口只查询 Nacos 注册中心，无法看到下线实例。
     *             请使用 {@link #queryInstanceList} 接口。
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @Deprecated
    @PostMapping("/getGatewayInstances")
    public ResponseDTO<GatewayInstanceListRsp> getGatewayInstances(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return gatewayInstanceService.getGatewayInstances();
    }

    /**
     * 获取网关实例详情
     *
     * @param reqDto 请求参数
     * @return 实例详情
     */
    @PostMapping("/getGatewayInstanceDetail")
    public ResponseDTO<GatewayInstanceVO> getGatewayInstanceDetail(@RequestBody @Validated RequestDTO<GetGatewayInstanceDetailReq> reqDto) {
        return gatewayInstanceService.getGatewayInstanceDetail(reqDto.getBody());
    }

    /**
     * 下线网关实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "网关实例下线")
    @PostMapping("/offlineInstance")
    public ResponseDTO<EmptyBody> offlineInstance(@RequestBody @Validated RequestDTO<OfflineGatewayInstanceReq> reqDto) {
        gatewayInstanceService.offlineInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 优雅下线网关实例（流量排空）
     * 先设置 weight=0 停止接收新流量，等待排空后再设置 enabled=false
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "网关实例优雅下线")
    @PostMapping("/gracefulOfflineInstance")
    public ResponseDTO<EmptyBody> gracefulOfflineInstance(@RequestBody @Validated RequestDTO<OfflineGatewayInstanceReq> reqDto) {
        gatewayInstanceService.gracefulOfflineInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 上线网关实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "网关实例上线")
    @PostMapping("/onlineInstance")
    public ResponseDTO<EmptyBody> onlineInstance(@RequestBody @Validated RequestDTO<OnlineGatewayInstanceReq> reqDto) {
        gatewayInstanceService.onlineInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 分页查询实例列表（从数据库）
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @PostMapping("/queryInstanceList")
    public ResponseDTO<QueryInstanceListRsp> queryInstanceList(@RequestBody @Validated RequestDTO<QueryInstanceReq> reqDto) {
        return gatewayInstanceService.queryInstanceList(reqDto.getBody());
    }

    /**
     * 删除实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/deleteInstance")
    public ResponseDTO<EmptyBody> deleteInstance(@RequestBody @Validated RequestDTO<DeleteInstanceReq> reqDto) {
        gatewayInstanceService.deleteInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取实例详情（含监控指标）
     *
     * @param reqDto 请求参数
     * @return 实例详情
     */
    @PostMapping("/getInstanceDetailWithMetrics")
    public ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(@RequestBody @Validated RequestDTO<GetInstanceDetailReq> reqDto) {
        return gatewayInstanceService.getInstanceDetailWithMetrics(reqDto.getBody());
    }

    /**
     * 手动刷新实例状态（从Nacos实时获取）
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/refreshInstanceStatus")
    public ResponseDTO<EmptyBody> refreshInstanceStatus(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return gatewayInstanceService.refreshInstanceStatus();
    }

    /**
     * 切换实例分组
     * 将实例从当前分组切换到目标分组（仅限离线/下线状态实例）
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "切换实例分组")
    @PostMapping("/switchInstanceGroup")
    public ResponseDTO<EmptyBody> switchInstanceGroup(@RequestBody @Validated RequestDTO<SwitchInstanceGroupReq> reqDto) {
        return gatewayInstanceService.switchInstanceGroup(reqDto.getBody());
    }
}