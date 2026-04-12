package com.blink.base.service.flowable.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.WorkflowConstant;
import com.blink.base.dto.req.LeaveApprovalReq;
import com.blink.base.dto.req.QueryProcessInstanceReq;
import com.blink.base.dto.req.StartProcessReq;
import com.blink.base.dto.rsp.ProcessInstanceDetailRsp;
import com.blink.base.dto.rsp.ProcessInstanceRsp;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.base.service.flowable.FlowableQueryHelper;
import com.blink.base.service.flowable.ProcessInstanceService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务实现类
 * <p>
 * 提供流程实例的管理功能：启动、查询、删除、回退
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final TaskService taskService;
    private final FlowableQueryHelper flowableQueryHelper;

    public ProcessInstanceServiceImpl(RuntimeService runtimeService,
                                       HistoryService historyService,
                                       RepositoryService repositoryService,
                                       TaskService taskService,
                                       FlowableQueryHelper flowableQueryHelper) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.taskService = taskService;
        this.flowableQueryHelper = flowableQueryHelper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startProcess(StartProcessReq req) throws BlinkException {
        try {
            log.info("[Workflow] 启动流程实例 | processKey: {}, businessKey: {}", req.getProcessDefinitionKey(), req.getBusinessKey());

            Map<String, Object> variables = req.getVariables();
            if (variables == null) {
                variables = new HashMap<>(WorkflowConstant.DEFAULT_VARIABLE_MAP_SIZE);
            }

            ProcessInstance processInstance = doStartProcessInstance(req, variables);

            log.info("[Workflow] 流程实例启动成功 | processInstanceId: {}", processInstance.getId());
            return convertToProcessInstanceVO(processInstance, WorkflowConstant.STATUS_RUNNING);

        } catch (Exception e) {
            log.error("[Workflow] 启动流程实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("启动流程实例失败: " + e.getMessage(), e, BaseErrCodeConstant.START_PROCESS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startLeaveProcess(LeaveApprovalReq req) throws BlinkException {
        try {
            log.info("[Workflow] 用户启动请假审批流程 | applicantId: {}, leaveDays: {}", req.getApplicantId(), req.getLeaveDays());

            Map<String, Object> variables = buildLeaveProcessVariables(req);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    WorkflowConstant.PROCESS_KEY_LEAVE,
                    req.getApplicantId(),
                    variables
            );

            log.info("[Workflow] 请假审批流程启动成功 | processInstanceId: {}", processInstance.getId());
            return convertToProcessInstanceVO(processInstance, WorkflowConstant.STATUS_RUNNING);

        } catch (Exception e) {
            log.error("[Workflow] 启动请假审批流程失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("启动请假审批流程失败: " + e.getMessage(), e, BaseErrCodeConstant.START_PROCESS_ERROR);
        }
    }

    @Override
    public ProcessInstanceRsp getProcessInstanceList(QueryProcessInstanceReq req) throws BlinkException {
        try {
            log.debug("[Workflow] 查询流程实例列表 | processKey: {}, startUserId: {}", req.getProcessDefinitionKey(), req.getStartUserId());

            ProcessInstanceRsp rsp = new ProcessInstanceRsp();

            return PageUtils.<QueryProcessInstanceReq, ProcessInstanceVO, ProcessInstanceRsp>queryPage(req, () -> executeProcessInstanceQuery(req), rsp);

        } catch (Exception e) {
            log.error("[Workflow] 查询流程实例列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询流程实例列表失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_PROCESS_INSTANCE_ERROR);
        }
    }

    @Override
    public List<ProcessInstanceDetailRsp> getMyProcessInstances(String userId, String status) throws BlinkException {
        try {
            log.debug("[Workflow] 查询用户发起的流程实例 | userId: {}, status: {}", userId, status);

            List<ProcessInstanceDetailRsp> result = new ArrayList<>();

            if (!WorkflowConstant.STATUS_COMPLETED.equals(status)) {
                // 查询运行中的流程
                List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                        .startedBy(userId)
                        .orderByProcessInstanceId()
                        .desc()
                        .list();
                for (ProcessInstance pi : processInstances) {
                    result.add(convertToProcessInstanceDetailRsp(pi, WorkflowConstant.STATUS_RUNNING));
                }
            }

            if (!WorkflowConstant.STATUS_RUNNING.equals(status)) {
                // 查询已完成的流程
                List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                        .startedBy(userId)
                        .finished()
                        .orderByProcessInstanceStartTime()
                        .desc()
                        .list();
                for (HistoricProcessInstance hpi : historicProcessInstances) {
                    result.add(convertToHistoricProcessInstanceDetailRsp(hpi));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[Workflow] 查询用户发起的流程实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询用户发起的流程实例失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_MY_PROCESS_ERROR);
        }
    }

    @Override
    public ProcessInstanceDetailRsp getProcessInstanceDetail(String processInstanceId) throws BlinkException {
        ProcessInstanceDetailRsp rsp = new ProcessInstanceDetailRsp();
        try {
            log.debug("[Workflow] 查询流程实例详情 | processInstanceId: {}", processInstanceId);

            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                return convertToProcessInstanceDetailRsp(processInstance, WorkflowConstant.STATUS_RUNNING);
            }

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicProcessInstance != null) {
                rsp = convertToHistoricProcessInstanceDetailRsp(historicProcessInstance);
                return rsp;
            }

            BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 查询流程实例详情失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询流程实例详情失败: " + e.getMessage(), e, BaseErrCodeConstant.GET_PROCESS_INSTANCE_ERROR);
        }

        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessInstance(String processInstanceId, String reason) throws BlinkException {
        try {
            log.info("[Workflow] 删除流程实例 | processInstanceId: {}, reason: {}", processInstanceId, reason);
            runtimeService.deleteProcessInstance(processInstanceId, reason);
            log.info("[Workflow] 流程实例删除成功 | processInstanceId: {}", processInstanceId);
        } catch (Exception e) {
            log.error("[Workflow] 删除流程实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除流程实例失败: " + e.getMessage(), e, BaseErrCodeConstant.DELETE_PROCESS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackProcess(String processInstanceId, String targetActivityId, String reason) throws BlinkException {
        try {
            log.info("[Workflow] 回退流程到指定节点 | processInstanceId: {}, targetActivityId: {}, reason: {}", processInstanceId, targetActivityId, reason);

            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (pi == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
            }

            // 获取当前活动节点
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            if (CollUtil.isEmpty(activeActivityIds)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
            }

            // 添加回退评论
            taskService.addComment(null, processInstanceId, "流程回退: " + reason);

            // 执行回退
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdsToSingleActivityId(activeActivityIds, targetActivityId)
                    .changeState();

            log.info("[Workflow] 流程回退成功 | processInstanceId: {}", processInstanceId);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 回退流程失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("回退流程失败: " + e.getMessage(), e, BaseErrCodeConstant.ROLLBACK_PROCESS_ERROR);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 启动流程实例
     */
    private ProcessInstance doStartProcessInstance(StartProcessReq req, Map<String, Object> variables) {
        if (StrUtil.isNotBlank(req.getBusinessKey())) {
            return runtimeService.startProcessInstanceByKey(
                    req.getProcessDefinitionKey(),
                    req.getBusinessKey(),
                    variables
            );
        } else {
            return runtimeService.startProcessInstanceByKey(
                    req.getProcessDefinitionKey(),
                    variables
            );
        }
    }

    /**
     * 构建请假流程变量
     */
    private Map<String, Object> buildLeaveProcessVariables(LeaveApprovalReq req) {
        Map<String, Object> variables = new HashMap<>(WorkflowConstant.LEAVE_VARIABLE_MAP_SIZE);
        variables.put(WorkflowConstant.VAR_APPLICANT, req.getApplicantId());
        variables.put(WorkflowConstant.VAR_APPLICANT_NAME, req.getApplicantName());
        variables.put(WorkflowConstant.VAR_LEAVE_TYPE, req.getLeaveType());
        variables.put(WorkflowConstant.VAR_START_DATE, req.getStartDate());
        variables.put(WorkflowConstant.VAR_END_DATE, req.getEndDate());
        variables.put(WorkflowConstant.VAR_LEAVE_DAYS, req.getLeaveDays());
        variables.put(WorkflowConstant.VAR_REASON, req.getReason());
        return variables;
    }

    /**
     * 执行流程实例查询
     */
    private List<ProcessInstanceVO> executeProcessInstanceQuery(QueryProcessInstanceReq req) {
        org.flowable.engine.runtime.ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

        if (StrUtil.isNotBlank(req.getProcessDefinitionKey())) {
            runtimeQuery.processDefinitionKey(req.getProcessDefinitionKey());
        }
        if (StrUtil.isNotBlank(req.getStartUserId())) {
            runtimeQuery.startedBy(req.getStartUserId());
        }

        List<ProcessInstanceVO> result = new ArrayList<>();

        // 查询运行中的流程
        if (!WorkflowConstant.STATUS_COMPLETED.equals(req.getStatus())) {
            List<ProcessInstance> processInstances = runtimeQuery
                    .orderByProcessInstanceId()
                    .desc()
                    .list();
            for (ProcessInstance pi : processInstances) {
                result.add(convertToProcessInstanceVO(pi, WorkflowConstant.STATUS_RUNNING));
            }
        }

        // 查询已完成的流程
        if (!WorkflowConstant.STATUS_RUNNING.equals(req.getStatus())) {
            result.addAll(queryHistoricProcessInstances(req));
        }

        return result;
    }

    /**
     * 查询历史流程实例
     */
    private List<ProcessInstanceVO> queryHistoricProcessInstances(QueryProcessInstanceReq req) {
        org.flowable.engine.history.HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery()
                .finished();

        if (StrUtil.isNotBlank(req.getProcessDefinitionKey())) {
            historyQuery.processDefinitionKey(req.getProcessDefinitionKey());
        }
        if (StrUtil.isNotBlank(req.getStartUserId())) {
            historyQuery.startedBy(req.getStartUserId());
        }

        List<HistoricProcessInstance> historicProcessInstances = historyQuery
                .orderByProcessInstanceStartTime()
                .desc()
                .list();

        List<ProcessInstanceVO> result = new ArrayList<>();
        for (HistoricProcessInstance hpi : historicProcessInstances) {
            result.add(convertToHistoricProcessInstanceVO(hpi));
        }

        return result;
    }

    /**
     * 转换ProcessInstance为ProcessInstanceVO
     */
    private ProcessInstanceVO convertToProcessInstanceVO(ProcessInstance pi, String status) {
        String processName = flowableQueryHelper.getProcessDefinitionName(pi.getProcessDefinitionId());
        Map<String, Object> processVariables = flowableQueryHelper.getProcessVariables(pi.getId());
        String currentActivityName = flowableQueryHelper.getCurrentActivityName(pi);

        return ProcessInstanceVO.builder()
                .processInstanceId(pi.getId())
                .processDefinitionId(pi.getProcessDefinitionId())
                .processDefinitionKey(pi.getProcessDefinitionKey())
                .processDefinitionName(processName)
                .businessKey(pi.getBusinessKey())
                .currentActivityName(currentActivityName)
                .startTime(pi.getStartTime() != null ?
                        LocalDateTime.ofInstant(pi.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .startUserId(pi.getStartUserId())
                .status(status)
                .processVariables(processVariables)
                .build();
    }

    /**
     * 转换HistoricProcessInstance为ProcessInstanceVO
     */
    private ProcessInstanceVO convertToHistoricProcessInstanceVO(HistoricProcessInstance hpi) {
        String processName = flowableQueryHelper.getProcessDefinitionName(hpi.getProcessDefinitionId());

        return ProcessInstanceVO.builder()
                .processInstanceId(hpi.getId())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(processName)
                .businessKey(hpi.getBusinessKey())
                .startTime(hpi.getStartTime() != null ?
                        LocalDateTime.ofInstant(hpi.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .endTime(hpi.getEndTime() != null ?
                        LocalDateTime.ofInstant(hpi.getEndTime().toInstant(), ZoneId.systemDefault()) : null)
                .startUserId(hpi.getStartUserId())
                .status(WorkflowConstant.STATUS_COMPLETED)
                .build();
    }

    /**
     * 转换ProcessInstance为ProcessInstanceDetailRsp
     */
    private ProcessInstanceDetailRsp convertToProcessInstanceDetailRsp(ProcessInstance pi, String status) {
        String processName = flowableQueryHelper.getProcessDefinitionName(pi.getProcessDefinitionId());
        Map<String, Object> processVariables = flowableQueryHelper.getProcessVariables(pi.getId());
        String currentActivityName = flowableQueryHelper.getCurrentActivityName(pi);

        return ProcessInstanceDetailRsp.builder()
                .processInstanceId(pi.getId())
                .processDefinitionId(pi.getProcessDefinitionId())
                .processDefinitionKey(pi.getProcessDefinitionKey())
                .processDefinitionName(processName)
                .businessKey(pi.getBusinessKey())
                .currentActivityName(currentActivityName)
                .startTime(pi.getStartTime() != null ?
                        LocalDateTime.ofInstant(pi.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .startUserId(pi.getStartUserId())
                .status(status)
                .processVariables(processVariables)
                .build();
    }

    /**
     * 转换HistoricProcessInstance为ProcessInstanceDetailRsp
     */
    private ProcessInstanceDetailRsp convertToHistoricProcessInstanceDetailRsp(HistoricProcessInstance hpi) {
        String processName = flowableQueryHelper.getProcessDefinitionName(hpi.getProcessDefinitionId());

        return ProcessInstanceDetailRsp.builder()
                .processInstanceId(hpi.getId())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(processName)
                .businessKey(hpi.getBusinessKey())
                .startTime(hpi.getStartTime() != null ?
                        LocalDateTime.ofInstant(hpi.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .endTime(hpi.getEndTime() != null ?
                        LocalDateTime.ofInstant(hpi.getEndTime().toInstant(), ZoneId.systemDefault()) : null)
                .startUserId(hpi.getStartUserId())
                .status(WorkflowConstant.STATUS_COMPLETED)
                .build();
    }
}