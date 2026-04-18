package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;
import com.blink.gateway.admin.service.GatewayInstanceGroupService;
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
 * 实例分组管理控制器
 * 提供实例分组的增删改查功能
 *
 * @author binblink
 * @since 2026-04-18
 */
@RestController
@RequestMapping("/instanceGroup")
public class InstanceGroupController {

    @Resource
    private GatewayInstanceGroupService gatewayInstanceGroupService;

    /**
     * 分页查询实例分组列表
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/queryInstanceGroupList")
    public ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(@RequestBody @Validated RequestDTO<QueryInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.queryInstanceGroupList(reqDto.getBody());
    }

    /**
     * 获取实例分组详情
     *
     * @param reqDto 请求参数
     * @return 分组详情
     */
    @PostMapping("/getInstanceGroupDetail")
    public ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(@RequestBody @Validated RequestDTO<GetInstanceGroupReq> reqDto) {
        return gatewayInstanceGroupService.getInstanceGroupDetail(reqDto.getBody());
    }

    /**
     * 新增实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "新增实例分组")
    @PostMapping("/addInstanceGroup")
    public ResponseDTO<EmptyBody> addInstanceGroup(@RequestBody @Validated RequestDTO<AddInstanceGroupReq> reqDto) {
        gatewayInstanceGroupService.addInstanceGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "更新实例分组")
    @PostMapping("/updateInstanceGroup")
    public ResponseDTO<EmptyBody> updateInstanceGroup(@RequestBody @Validated RequestDTO<UpdateInstanceGroupReq> reqDto) {
        gatewayInstanceGroupService.updateInstanceGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除实例分组
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @RecordLog(type = LogType.OPERATION, description = "删除实例分组")
    @PostMapping("/deleteInstanceGroup")
    public ResponseDTO<EmptyBody> deleteInstanceGroup(@RequestBody @Validated RequestDTO<DeleteInstanceGroupReq> reqDto) {
        gatewayInstanceGroupService.deleteInstanceGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取启用状态的分组列表（用于下拉选择）
     *
     * @param reqDto 请求参数
     * @return 分组列表
     */
    @PostMapping("/getEnabledInstanceGroups")
    public ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return gatewayInstanceGroupService.getEnabledInstanceGroups();
    }
}
