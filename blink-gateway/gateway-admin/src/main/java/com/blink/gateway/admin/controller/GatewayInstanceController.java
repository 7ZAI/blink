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
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.service.GatewayInstanceService;
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
     * 获取网关实例列表
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
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
    @PostMapping("/offlineInstance")
    public ResponseDTO<EmptyBody> offlineInstance(@RequestBody @Validated RequestDTO<OfflineGatewayInstanceReq> reqDto) {
        gatewayInstanceService.offlineInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 上线网关实例
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
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
     * 保存实例（新增/编辑）
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveInstance")
    public ResponseDTO<EmptyBody> saveInstance(@RequestBody @Validated RequestDTO<SaveInstanceReq> reqDto) {
        gatewayInstanceService.saveInstance(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
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
}