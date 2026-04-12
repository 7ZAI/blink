package com.blink.base.service.flowable;

import com.blink.base.dto.req.LeaveApprovalReq;
import com.blink.base.dto.req.QueryProcessInstanceReq;
import com.blink.base.dto.req.StartProcessReq;
import com.blink.base.dto.rsp.ProcessInstanceDetailRsp;
import com.blink.base.dto.rsp.ProcessInstanceRsp;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * 流程实例服务接口
 * <p>
 * 提供流程实例的管理功能：启动、查询、删除、回退
 * </p>
 *
 * @author binblink
 */
public interface ProcessInstanceService {

    /**
     * 启动流程实例
     *
     * @param req 启动请求
     * @return 流程实例信息
     * @throws BlinkException 启动失败时抛出
     */
    ProcessInstanceVO startProcess(StartProcessReq req) throws BlinkException;

    /**
     * 启动请假审批流程
     *
     * @param req 请假申请请求
     * @return 流程实例信息
     * @throws BlinkException 启动失败时抛出
     */
    ProcessInstanceVO startLeaveProcess(LeaveApprovalReq req) throws BlinkException;

    /**
     * 分页查询流程实例列表
     *
     * @param req 查询请求
     * @return 流程实例分页响应
     * @throws BlinkException 查询失败时抛出
     */
    ProcessInstanceRsp getProcessInstanceList(QueryProcessInstanceReq req) throws BlinkException;

    /**
     * 查询用户发起的流程实例
     *
     * @param userId 用户ID
     * @param status 状态（running-运行中, completed-已完成, all-全部）
     * @return 流程实例列表
     * @throws BlinkException 查询失败时抛出
     */
    List<ProcessInstanceDetailRsp> getMyProcessInstances(String userId, String status) throws BlinkException;

    /**
     * 查询流程实例详情
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例详情
     * @throws BlinkException 查询失败时抛出
     */
    ProcessInstanceDetailRsp getProcessInstanceDetail(String processInstanceId) throws BlinkException;

    /**
     * 删除流程实例
     *
     * @param processInstanceId 流程实例ID
     * @param reason            删除原因
     * @throws BlinkException 删除失败时抛出
     */
    void deleteProcessInstance(String processInstanceId, String reason) throws BlinkException;

    /**
     * 回退流程到指定节点
     *
     * @param processInstanceId 流程实例ID
     * @param targetActivityId  目标节点ID
     * @param reason            回退原因
     * @throws BlinkException 回退失败时抛出
     */
    void rollbackProcess(String processInstanceId, String targetActivityId, String reason) throws BlinkException;
}