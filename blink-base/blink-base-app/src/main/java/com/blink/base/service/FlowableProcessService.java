package com.blink.base.service;

import com.blink.base.dto.req.CompleteTaskReq;
import com.blink.base.dto.req.DelegateTaskReq;
import com.blink.base.dto.req.DeployProcessReq;
import com.blink.base.dto.req.ImportXmlProcessReq;
import com.blink.base.dto.req.LeaveApprovalReq;
import com.blink.base.dto.req.QueryProcessDefinitionReq;
import com.blink.base.dto.req.QueryProcessInstanceReq;
import com.blink.base.dto.req.QueryTaskReq;
import com.blink.base.dto.req.RollbackProcessReq;
import com.blink.base.dto.req.StartProcessReq;
import com.blink.base.dto.req.WithdrawTaskReq;
import com.blink.base.dto.rsp.HistoricTaskRsp;
import com.blink.base.dto.rsp.ProcessDefinitionRsp;
import com.blink.base.dto.rsp.ProcessHistoryRsp;
import com.blink.base.dto.rsp.ProcessInstanceDetailRsp;
import com.blink.base.dto.rsp.ProcessInstanceRsp;
import com.blink.base.dto.rsp.TaskRsp;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * 流程服务接口
 * <p>
 * 提供流程的定义、部署、启动、查询、任务处理等功能
 * </p>
 *
 * @author binblink
 */
public interface FlowableProcessService {

    // ==================== 流程定义管理 ====================

    /**
     * 部署流程定义
     *
     * @param req 部署请求
     * @return 部署ID
     * @throws BlinkException 部署失败时抛出
     */
    String deployProcess(DeployProcessReq req) throws BlinkException;

    /**
     * 从BPMN XML导入流程定义
     *
     * @param req 导入请求（包含XML内容）
     * @return 部署ID
     * @throws BlinkException 导入失败时抛出
     */
    String importProcessFromXml(ImportXmlProcessReq req) throws BlinkException;

    /**
     * 分页查询流程定义列表
     *
     * @param req 查询请求
     * @return 流程定义分页响应
     * @throws BlinkException 查询失败时抛出
     */
    ProcessDefinitionRsp getProcessDefinitionList(QueryProcessDefinitionReq req) throws BlinkException;

    /**
     * 根据流程定义ID获取流程图XML
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程图XML
     * @throws BlinkException 获取失败时抛出
     */
    String getProcessDiagramXml(String processDefinitionId) throws BlinkException;

    /**
     * 根据流程实例ID获取流程图（高亮当前节点）
     *
     * @param processInstanceId 流程实例ID
     * @return 流程图图片字节数组
     * @throws BlinkException 获取失败时抛出
     */
    byte[] getProcessDiagramImage(String processInstanceId) throws BlinkException;

    /**
     * 挂起流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @throws BlinkException 挂起失败时抛出
     */
    void suspendProcessDefinition(String processDefinitionId) throws BlinkException;

    /**
     * 激活流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @throws BlinkException 激活失败时抛出
     */
    void activateProcessDefinition(String processDefinitionId) throws BlinkException;

    /**
     * 删除流程定义（删除部署）
     *
     * @param deploymentId 部署ID
     * @param cascade      是否级联删除流程实例
     * @throws BlinkException 删除失败时抛出
     */
    void deleteProcessDefinition(String deploymentId, boolean cascade) throws BlinkException;

    // ==================== 流程实例管理 ====================

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
     * @param req 回退请求
     * @throws BlinkException 回退失败时抛出
     */
    void rollbackProcess(RollbackProcessReq req) throws BlinkException;

    // ==================== 任务管理 ====================

    /**
     * 查询用户待办任务
     *
     * @param userId 用户ID
     * @return 待办任务分页响应
     * @throws BlinkException 查询失败时抛出
     */
    TaskRsp getUserTasks(String userId) throws BlinkException;

    /**
     * 分页查询待办任务
     *
     * @param req 查询请求
     * @return 待办任务分页响应
     * @throws BlinkException 查询失败时抛出
     */
    TaskRsp getPendingTasks(QueryTaskReq req) throws BlinkException;

    /**
     * 分页查询已办任务
     *
     * @param req 查询请求
     * @return 已办任务分页响应
     * @throws BlinkException 查询失败时抛出
     */
    HistoricTaskRsp getCompletedTasks(QueryTaskReq req) throws BlinkException;

    /**
     * 完成任务
     *
     * @param req 完成任务请求
     * @throws BlinkException 完成失败时抛出
     */
    void completeTask(CompleteTaskReq req) throws BlinkException;

    /**
     * 委托任务
     *
     * @param req 委托任务请求
     * @throws BlinkException 委托失败时抛出
     */
    void delegateTask(DelegateTaskReq req) throws BlinkException;

    /**
     * 认领任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @throws BlinkException 认领失败时抛出
     */
    void claimTask(String taskId, String userId) throws BlinkException;

    /**
     * 取消认领任务
     *
     * @param taskId 任务ID
     * @throws BlinkException 取消认领失败时抛出
     */
    void unclaimTask(String taskId) throws BlinkException;

    /**
     * 撤回任务（发起人撤回未处理的任务）
     *
     * @param req 撤回请求
     * @throws BlinkException 撤回失败时抛出
     */
    void withdrawTask(WithdrawTaskReq req) throws BlinkException;

    // ==================== 流程历史 ====================

    /**
     * 查询流程实例历史
     *
     * @param processInstanceId 流程实例ID
     * @return 历史记录
     * @throws BlinkException 查询失败时抛出
     */
    List<ProcessHistoryRsp> getProcessHistory(String processInstanceId) throws BlinkException;
}