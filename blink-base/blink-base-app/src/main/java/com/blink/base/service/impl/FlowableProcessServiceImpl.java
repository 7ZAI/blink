package com.blink.base.service.impl;

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
import com.blink.base.service.FlowableProcessService;
import com.blink.base.service.flowable.ProcessDefinitionService;
import com.blink.base.service.flowable.ProcessHistoryService;
import com.blink.base.service.flowable.ProcessInstanceService;
import com.blink.base.service.flowable.TaskManagementService;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程服务门面实现类
 * <p>
 * 作为统一入口，委托各子Service处理具体业务逻辑
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class FlowableProcessServiceImpl implements FlowableProcessService {

    @Resource
    private ProcessDefinitionService processDefinitionService;

    @Resource
    private ProcessInstanceService processInstanceService;

    @Resource
    private TaskManagementService taskManagementService;

    @Resource
    private ProcessHistoryService processHistoryService;

    // ==================== 流程定义管理 ====================

    @Override
    public String deployProcess(DeployProcessReq req) throws BlinkException {
        return processDefinitionService.deployProcess(req);
    }

    @Override
    public String importProcessFromXml(ImportXmlProcessReq req) throws BlinkException {
        return processDefinitionService.importProcessFromXml(req);
    }

    @Override
    public ProcessDefinitionRsp getProcessDefinitionList(QueryProcessDefinitionReq req) throws BlinkException {
        return processDefinitionService.getProcessDefinitionList(req);
    }

    @Override
    public String getProcessDiagramXml(String processDefinitionId) throws BlinkException {
        return processDefinitionService.getProcessDiagramXml(processDefinitionId);
    }

    @Override
    public byte[] getProcessDiagramImage(String processInstanceId) throws BlinkException {
        return processDefinitionService.getProcessDiagramImage(processInstanceId);
    }

    @Override
    public void suspendProcessDefinition(String processDefinitionId) throws BlinkException {
        processDefinitionService.suspendProcessDefinition(processDefinitionId);
    }

    @Override
    public void activateProcessDefinition(String processDefinitionId) throws BlinkException {
        processDefinitionService.activateProcessDefinition(processDefinitionId);
    }

    @Override
    public void deleteProcessDefinition(String deploymentId, boolean cascade) throws BlinkException {
        processDefinitionService.deleteProcessDefinition(deploymentId, cascade);
    }

    // ==================== 流程实例管理 ====================

    @Override
    public ProcessInstanceVO startProcess(StartProcessReq req) throws BlinkException {
        return processInstanceService.startProcess(req);
    }

    @Override
    public ProcessInstanceVO startLeaveProcess(LeaveApprovalReq req) throws BlinkException {
        return processInstanceService.startLeaveProcess(req);
    }

    @Override
    public ProcessInstanceRsp getProcessInstanceList(QueryProcessInstanceReq req) throws BlinkException {
        return processInstanceService.getProcessInstanceList(req);
    }

    @Override
    public List<ProcessInstanceDetailRsp> getMyProcessInstances(String userId, String status) throws BlinkException {
        return processInstanceService.getMyProcessInstances(userId, status);
    }

    @Override
    public ProcessInstanceDetailRsp getProcessInstanceDetail(String processInstanceId) throws BlinkException {
        return processInstanceService.getProcessInstanceDetail(processInstanceId);
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String reason) throws BlinkException {
        processInstanceService.deleteProcessInstance(processInstanceId, reason);
    }

    @Override
    public void rollbackProcess(RollbackProcessReq req) throws BlinkException {
        processInstanceService.rollbackProcess(req.getProcessInstanceId(), req.getTargetActivityId(), req.getReason());
    }

    // ==================== 任务管理 ====================

    @Override
    public TaskRsp getUserTasks(String userId) throws BlinkException {
        return taskManagementService.getUserTasks(userId);
    }

    @Override
    public TaskRsp getPendingTasks(QueryTaskReq req) throws BlinkException {
        return taskManagementService.getPendingTasks(req);
    }

    @Override
    public HistoricTaskRsp getCompletedTasks(QueryTaskReq req) throws BlinkException {
        return taskManagementService.getCompletedTasks(req);
    }

    @Override
    public void completeTask(CompleteTaskReq req) throws BlinkException {
        taskManagementService.completeTask(req);
    }

    @Override
    public void delegateTask(DelegateTaskReq req) throws BlinkException {
        taskManagementService.delegateTask(req);
    }

    @Override
    public void claimTask(String taskId, String userId) throws BlinkException {
        taskManagementService.claimTask(taskId, userId);
    }

    @Override
    public void unclaimTask(String taskId) throws BlinkException {
        taskManagementService.unclaimTask(taskId);
    }

    @Override
    public void withdrawTask(WithdrawTaskReq req) throws BlinkException {
        taskManagementService.withdrawTask(req.getTaskId(), req.getUserId(), req.getReason());
    }

    // ==================== 流程历史 ====================

    @Override
    public List<ProcessHistoryRsp> getProcessHistory(String processInstanceId) throws BlinkException {
        return processHistoryService.getProcessHistory(processInstanceId);
    }
}