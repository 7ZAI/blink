package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;

import java.util.List;

/**
 * 实例分组服务接口
 *
 * @author binblink
 * @since 2026-04-18
 */
public interface GatewayInstanceGroupService {

    /**
     * 分页查询实例分组列表
     *
     * @param req 查询请求参数
     * @return 分组列表响应
     */
    ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(QueryInstanceGroupReq req);

    /**
     * 获取实例分组详情
     *
     * @param req 请求参数
     * @return 分组详情
     */
    ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(GetInstanceGroupReq req);

    /**
     * 新增实例分组
     *
     * @param req 新增请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> addInstanceGroup(AddInstanceGroupReq req);

    /**
     * 更新实例分组
     *
     * @param req 更新请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> updateInstanceGroup(UpdateInstanceGroupReq req);

    /**
     * 删除实例分组
     *
     * @param req 删除请求参数
     * @return 操作结果
     */
    ResponseDTO<Void> deleteInstanceGroup(DeleteInstanceGroupReq req);

    /**
     * 获取所有启用的分组列表（用于下拉选择）
     *
     * @return 分组列表
     */
    ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups();
}
