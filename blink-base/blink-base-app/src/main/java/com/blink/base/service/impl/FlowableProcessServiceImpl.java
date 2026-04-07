package com.blink.base.service.impl;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.dto.req.CompleteTaskReq;
import com.blink.base.dto.req.DelegateTaskReq;
import com.blink.base.dto.req.DeployProcessReq;
import com.blink.base.dto.req.LeaveApprovalReq;
import com.blink.base.dto.req.QueryProcessDefinitionReq;
import com.blink.base.dto.req.QueryProcessInstanceReq;
import com.blink.base.dto.req.QueryTaskReq;
import com.blink.base.dto.req.StartProcessReq;
import com.blink.base.dto.rsp.HistoricTaskRsp;
import com.blink.base.dto.rsp.ProcessDefinitionRsp;
import com.blink.base.dto.rsp.ProcessHistoryRsp;
import com.blink.base.dto.rsp.ProcessInstanceDetailRsp;
import com.blink.base.dto.rsp.ProcessInstanceRsp;
import com.blink.base.dto.rsp.TaskRsp;
import com.blink.base.dto.vo.HistoricTaskVO;
import com.blink.base.dto.vo.ProcessDefinitionVO;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.base.dto.vo.TaskVO;
import com.blink.base.service.FlowableProcessService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程服务实现类
 * <p>
 * 提供Flowable工作流的核心操作功能
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class FlowableProcessServiceImpl implements FlowableProcessService {

    private static final String PROCESS_KEY_LEAVE = "leaveApproval";

    private final RepositoryService repositoryService;

    private final RuntimeService runtimeService;

    private final TaskService taskService;

    private final HistoryService historyService;

    private final ProcessEngineConfiguration processEngineConfiguration;

    public FlowableProcessServiceImpl(RepositoryService repositoryService,
                                       RuntimeService runtimeService,
                                       TaskService taskService,
                                       HistoryService historyService,
                                       ProcessEngineConfiguration processEngineConfiguration) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.processEngineConfiguration = processEngineConfiguration;
    }

    // ==================== 流程定义管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployProcess(DeployProcessReq req) throws BlinkException {
        try {
            log.info("开始部署流程定义: {}, KEY: {}", req.getProcessName(), req.getProcessKey());

            Deployment deployment = repositoryService.createDeployment()
                    .name(req.getProcessName())
                    .key(req.getProcessKey())
                    .addString(req.getProcessKey() + ".bpmn20.xml", req.getBpmnXmlContent())
                    .deploy();

            log.info("流程定义部署成功, 部署ID: {}", deployment.getId());
            return deployment.getId();

        } catch (Exception e) {
            log.error("部署流程定义失败: {}", e.getMessage(), e);
            throw new BlinkException("部署流程定义失败: " + e.getMessage(), e, "DEPLOY_PROCESS_ERROR");
        }
    }

    @Override
    public ProcessDefinitionRsp getProcessDefinitionList(QueryProcessDefinitionReq req) throws BlinkException {
        try {
            log.debug("查询流程定义列表, 名称: {}, KEY: {}", req.getName(), req.getKey());

            ProcessDefinitionQuery query = buildProcessDefinitionQuery(req);

            // 获取最新版本映射
            Map<String, String> latestVersionKeys = getLatestVersionKeys();

            ProcessDefinitionRsp rsp = new ProcessDefinitionRsp();

            // 使用 PageUtils.queryPage 执行分页
            return PageUtils.queryPage(req, () -> executeProcessDefinitionQuery(query, latestVersionKeys), rsp);

        } catch (Exception e) {
            log.error("查询流程定义列表失败: {}", e.getMessage(), e);
            throw new BlinkException("查询流程定义列表失败: " + e.getMessage(), e, "QUERY_PROCESS_DEF_ERROR");
        }
    }

    @Override
    public String getProcessDiagramXml(String processDefinitionId) throws BlinkException {
        try {
            log.debug("获取流程图XML, 流程定义ID: {}", processDefinitionId);

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                // 业务异常：流程定义不存在
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_DEF_NOT_FOUND);
            }

            byte[] xmlBytes = repositoryService.getModelEditorSource(processDefinitionId);
            if (xmlBytes != null) {
                return new String(xmlBytes);
            }

            return null;

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取流程图XML失败: {}", e.getMessage(), e);
            throw new BlinkException("获取流程图XML失败: " + e.getMessage(), e, "GET_DIAGRAM_XML_ERROR");
        }
    }

    @Override
    public byte[] getProcessDiagramImage(String processInstanceId) throws BlinkException {
        try {
            log.debug("获取流程图图片, 流程实例ID: {}", processInstanceId);

            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            String processDefinitionId;
            List<String> activeActivityIds = new ArrayList<>();

            if (processInstance != null) {
                processDefinitionId = processInstance.getProcessDefinitionId();
                activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            } else {
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicProcessInstance == null) {
                    // 业务异常：流程实例不存在
                    BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
                }
                processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                // 业务异常：流程定义不存在
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_DEF_NOT_FOUND);
            }

            return generateProcessDiagram(bpmnModel, activeActivityIds);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取流程图图片失败: {}", e.getMessage(), e);
            throw new BlinkException("获取流程图图片失败: " + e.getMessage(), e, "GET_DIAGRAM_IMAGE_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendProcessDefinition(String processDefinitionId) throws BlinkException {
        try {
            log.info("挂起流程定义: {}", processDefinitionId);
            repositoryService.suspendProcessDefinitionById(processDefinitionId);
            log.info("流程定义挂起成功: {}", processDefinitionId);
        } catch (Exception e) {
            log.error("挂起流程定义失败: {}", e.getMessage(), e);
            throw new BlinkException("挂起流程定义失败: " + e.getMessage(), e, "SUSPEND_PROCESS_DEF_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateProcessDefinition(String processDefinitionId) throws BlinkException {
        try {
            log.info("激活流程定义: {}", processDefinitionId);
            repositoryService.activateProcessDefinitionById(processDefinitionId);
            log.info("流程定义激活成功: {}", processDefinitionId);
        } catch (Exception e) {
            log.error("激活流程定义失败: {}", e.getMessage(), e);
            throw new BlinkException("激活流程定义失败: " + e.getMessage(), e, "ACTIVATE_PROCESS_DEF_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessDefinition(String deploymentId, boolean cascade) throws BlinkException {
        try {
            log.info("删除流程定义, 部署ID: {}, 级联删除: {}", deploymentId, cascade);
            repositoryService.deleteDeployment(deploymentId, cascade);
            log.info("流程定义删除成功: {}", deploymentId);
        } catch (Exception e) {
            log.error("删除流程定义失败: {}", e.getMessage(), e);
            throw new BlinkException("删除流程定义失败: " + e.getMessage(), e, "DELETE_PROCESS_DEF_ERROR");
        }
    }

    // ==================== 流程实例管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startProcess(StartProcessReq req) throws BlinkException {
        try {
            log.info("启动流程实例, 流程KEY: {}, 业务KEY: {}", req.getProcessDefinitionKey(), req.getBusinessKey());

            Map<String, Object> variables = req.getVariables();
            if (variables == null) {
                variables = new HashMap<>(8);
            }

            ProcessInstance processInstance = doStartProcessInstance(req, variables);

            log.info("流程实例启动成功, 流程实例ID: {}", processInstance.getId());
            return convertToProcessInstanceVO(processInstance, "running");

        } catch (Exception e) {
            log.error("启动流程实例失败: {}", e.getMessage(), e);
            throw new BlinkException("启动流程实例失败: " + e.getMessage(), e, "START_PROCESS_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startLeaveProcess(LeaveApprovalReq req) throws BlinkException {
        try {
            log.info("用户[{}]启动请假审批流程, 请假天数: {}", req.getApplicantId(), req.getLeaveDays());

            Map<String, Object> variables = buildLeaveProcessVariables(req);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    PROCESS_KEY_LEAVE,
                    req.getApplicantId(),
                    variables
            );

            log.info("请假审批流程启动成功, 流程实例ID: {}", processInstance.getId());
            return convertToProcessInstanceVO(processInstance, "running");

        } catch (Exception e) {
            log.error("启动请假审批流程失败: {}", e.getMessage(), e);
            throw new BlinkException("启动请假审批流程失败: " + e.getMessage(), e, "START_PROCESS_ERROR");
        }
    }

    @Override
    public ProcessInstanceRsp getProcessInstanceList(QueryProcessInstanceReq req) throws BlinkException {
        try {
            log.debug("查询流程实例列表, KEY: {}, 发起人: {}", req.getProcessDefinitionKey(), req.getStartUserId());

            ProcessInstanceRsp rsp = new ProcessInstanceRsp();

            return PageUtils.<QueryProcessInstanceReq, ProcessInstanceVO, ProcessInstanceRsp>queryPage(req, () -> executeProcessInstanceQuery(req), rsp);

        } catch (Exception e) {
            log.error("查询流程实例列表失败: {}", e.getMessage(), e);
            throw new BlinkException("查询流程实例列表失败: " + e.getMessage(), e, "QUERY_PROCESS_INSTANCE_ERROR");
        }
    }

    @Override
    public List<ProcessInstanceDetailRsp> getMyProcessInstances(String userId, String status) throws BlinkException {
        try {
            log.debug("查询用户[{}]发起的流程实例, 状态: {}", userId, status);

            List<ProcessInstanceDetailRsp> result = new ArrayList<>();

            if (!"completed".equals(status)) {
                // 查询运行中的流程
                List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                        .startedBy(userId)
                        .orderByProcessInstanceId()
                        .desc()
                        .list();
                for (ProcessInstance pi : processInstances) {
                    result.add(convertToProcessInstanceDetailRsp(pi, "running"));
                }
            }

            if (!"running".equals(status)) {
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
            log.error("查询用户发起的流程实例失败: {}", e.getMessage(), e);
            throw new BlinkException("查询用户发起的流程实例失败: " + e.getMessage(), e, "QUERY_MY_PROCESS_ERROR");
        }
    }

    @Override
    public ProcessInstanceDetailRsp getProcessInstanceDetail(String processInstanceId) throws BlinkException {
        var rsp = new ProcessInstanceDetailRsp();
        try {
            log.debug("查询流程实例详情: {}", processInstanceId);

            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                return convertToProcessInstanceDetailRsp(processInstance, "running");
            }

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (historicProcessInstance != null) {
                rsp = convertToHistoricProcessInstanceDetailRsp(historicProcessInstance);
                return rsp;
            }
            // 业务异常：流程实例不存在
            BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询流程实例详情失败: {}", e.getMessage(), e);
            throw new BlinkException("查询流程实例详情失败: " + e.getMessage(), e, "GET_PROCESS_INSTANCE_ERROR");
        }

        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessInstance(String processInstanceId, String reason) throws BlinkException {
        try {
            log.info("删除流程实例[{}], 原因: {}", processInstanceId, reason);
            runtimeService.deleteProcessInstance(processInstanceId, reason);
            log.info("流程实例[{}]删除成功", processInstanceId);
        } catch (Exception e) {
            log.error("删除流程实例失败: {}", e.getMessage(), e);
            throw new BlinkException("删除流程实例失败: " + e.getMessage(), e, "DELETE_PROCESS_ERROR");
        }
    }

    // ==================== 任务管理 ====================

    @Override
    public TaskRsp getUserTasks(String userId) throws BlinkException {
        try {
            log.debug("查询用户[{}]的待办任务", userId);

            TaskRsp rsp = new TaskRsp();
            QueryTaskReq req = new QueryTaskReq();

            return PageUtils.<QueryTaskReq,TaskVO,TaskRsp>queryPage(req, () -> executeUserTasksQuery(userId), rsp);

        } catch (Exception e) {
            log.error("查询用户待办任务失败: {}", e.getMessage(), e);
            throw new BlinkException("查询用户待办任务失败: " + e.getMessage(), e, "QUERY_TASK_ERROR");
        }
    }

    @Override
    public TaskRsp getPendingTasks(QueryTaskReq req) throws BlinkException {
        try {
            log.debug("分页查询待办任务, 用户ID: {}", req.getUserId());

            TaskRsp rsp = new TaskRsp();

            return PageUtils.<QueryTaskReq,TaskVO,TaskRsp>queryPage(req, () -> executePendingTasksQuery(req), rsp);

        } catch (Exception e) {
            log.error("分页查询待办任务失败: {}", e.getMessage(), e);
            throw new BlinkException("分页查询待办任务失败: " + e.getMessage(), e, "QUERY_PENDING_TASK_ERROR");
        }
    }

    @Override
    public HistoricTaskRsp getCompletedTasks(QueryTaskReq req) throws BlinkException {
        try {
            log.debug("分页查询已办任务, 用户ID: {}", req.getUserId());

            HistoricTaskRsp rsp = new HistoricTaskRsp();

            return PageUtils.<QueryTaskReq, HistoricTaskVO, HistoricTaskRsp>queryPage(req, () -> executeCompletedTasksQuery(req), rsp);

        } catch (Exception e) {
            log.error("分页查询已办任务失败: {}", e.getMessage(), e);
            throw new BlinkException("分页查询已办任务失败: " + e.getMessage(), e, "QUERY_COMPLETED_TASK_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(CompleteTaskReq req) throws BlinkException {
        try {
            log.info("用户[{}]开始完成任务[{}]", req.getUserId(), req.getTaskId());

            Task task = taskService.createTaskQuery()
                    .taskId(req.getTaskId())
                    .singleResult();

            if (task == null) {
                // 业务异常：任务不存在
                BlinkException.throwBusinessException(BaseErrCodeConstant.TASK_NOT_FOUND);
            }

            boolean hasPermission = checkTaskPermission(task, req.getUserId());
            if (!hasPermission) {
                // 业务异常：无权限处理该任务
                BlinkException.throwBusinessException(BaseErrCodeConstant.NO_TASK_PERMISSION);
            }

            // 认领任务
            claimTaskIfNotAssigned(task, req);

            // 添加审批意见
            addTaskComment(task, req);

            // 构建变量并完成任务
            Map<String, Object> variables = buildTaskVariables(req);
            taskService.complete(req.getTaskId(), variables);
            log.info("任务[{}]完成成功", req.getTaskId());

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成任务失败: {}", e.getMessage(), e);
            throw new BlinkException("完成任务失败: " + e.getMessage(), e, "COMPLETE_TASK_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(DelegateTaskReq req) throws BlinkException {
        try {
            log.info("用户[{}]委托任务[{}]给用户[{}]", req.getCurrentUserId(), req.getTaskId(), req.getTargetUserId());

            Task task = taskService.createTaskQuery()
                    .taskId(req.getTaskId())
                    .singleResult();

            if (task == null) {
                // 业务异常：任务不存在
                BlinkException.throwBusinessException(BaseErrCodeConstant.TASK_NOT_FOUND);
            }

            boolean hasPermission = checkTaskPermission(task, req.getCurrentUserId());
            if (!hasPermission) {
                // 业务异常：无权限处理该任务
                BlinkException.throwBusinessException(BaseErrCodeConstant.NO_TASK_PERMISSION);
            }

            taskService.delegateTask(req.getTaskId(), req.getTargetUserId());
            log.info("任务[{}]委托成功", req.getTaskId());

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("委托任务失败: {}", e.getMessage(), e);
            throw new BlinkException("委托任务失败: " + e.getMessage(), e, "DELEGATE_TASK_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) throws BlinkException {
        try {
            log.info("用户[{}]认领任务[{}]", userId, taskId);
            taskService.claim(taskId, userId);
            log.info("任务[{}]认领成功", taskId);
        } catch (Exception e) {
            log.error("认领任务失败: {}", e.getMessage(), e);
            throw new BlinkException("认领任务失败: " + e.getMessage(), e, "CLAIM_TASK_ERROR");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unclaimTask(String taskId) throws BlinkException {
        try {
            log.info("取消认领任务[{}]", taskId);
            taskService.unclaim(taskId);
            log.info("任务[{}]取消认领成功", taskId);
        } catch (Exception e) {
            log.error("取消认领任务失败: {}", e.getMessage(), e);
            throw new BlinkException("取消认领任务失败: " + e.getMessage(), e, "UNCLAIM_TASK_ERROR");
        }
    }

    // ==================== 流程历史 ====================

    @Override
    public List<ProcessHistoryRsp> getProcessHistory(String processInstanceId) throws BlinkException {
        try {
            log.debug("查询流程实例[{}]的历史记录", processInstanceId);

            List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();

            Map<String, String> commentMap = buildCommentMap(processInstanceId);

            List<ProcessHistoryRsp> result = new ArrayList<>(activities.size());
            for (HistoricActivityInstance activity : activities) {
                result.add(convertToProcessHistoryRsp(activity, commentMap));
            }

            log.debug("流程实例[{}]共有{}条历史记录", processInstanceId, result.size());
            return result;

        } catch (Exception e) {
            log.error("查询流程历史失败: {}", e.getMessage(), e);
            throw new BlinkException("查询流程历史失败: " + e.getMessage(), e, "QUERY_HISTORY_ERROR");
        }
    }

    // ==================== 私有方法 - 查询执行 ====================

    /**
     * 构建流程定义查询条件
     *
     * @param req 查询请求
     * @return 流程定义查询对象
     */
    private ProcessDefinitionQuery buildProcessDefinitionQuery(QueryProcessDefinitionReq req) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

        if (req.getName() != null && !req.getName().isEmpty()) {
            query.processDefinitionNameLike("%" + req.getName() + "%");
        }
        if (req.getKey() != null && !req.getKey().isEmpty()) {
            query.processDefinitionKey(req.getKey());
        }
        if (Boolean.TRUE.equals(req.getLatestVersion())) {
            query.latestVersion();
        }

        return query;
    }

    /**
     * 获取最新版本映射
     *
     * @return KEY -> 最新版本ID的映射
     */
    private Map<String, String> getLatestVersionKeys() {
        List<ProcessDefinition> latestVersions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();
        return latestVersions.stream()
                .collect(Collectors.toMap(
                        ProcessDefinition::getKey,
                        ProcessDefinition::getId,
                        (a, b) -> a
                ));
    }

    /**
     * 执行流程定义查询并转换为VO列表
     *
     * @param query             查询对象
     * @param latestVersionKeys 最新版本映射
     * @return 流程定义VO列表
     */
    private List<ProcessDefinitionVO> executeProcessDefinitionQuery(
            ProcessDefinitionQuery query, Map<String, String> latestVersionKeys) {
        List<ProcessDefinition> processDefinitions = query
                .orderByProcessDefinitionVersion()
                .desc()
                .list();

        return processDefinitions.stream()
                .map(pd -> convertToProcessDefinitionVO(pd, latestVersionKeys))
                .collect(Collectors.toList());
    }

    /**
     * 执行流程实例查询
     *
     * @param req 查询请求
     * @return 流程实例VO列表
     */
    private List<ProcessInstanceVO> executeProcessInstanceQuery(QueryProcessInstanceReq req) {
        org.flowable.engine.runtime.ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

        if (req.getProcessDefinitionKey() != null && !req.getProcessDefinitionKey().isEmpty()) {
            runtimeQuery.processDefinitionKey(req.getProcessDefinitionKey());
        }
        if (req.getStartUserId() != null && !req.getStartUserId().isEmpty()) {
            runtimeQuery.startedBy(req.getStartUserId());
        }

        List<ProcessInstanceVO> result = new ArrayList<>();

        // 查询运行中的流程
        if (!"completed".equals(req.getStatus())) {
            List<ProcessInstance> processInstances = runtimeQuery
                    .orderByProcessInstanceId()
                    .desc()
                    .list();
            for (ProcessInstance pi : processInstances) {
                result.add(convertToProcessInstanceVO(pi, "running"));
            }
        }

        // 查询已完成的流程
        if (!"running".equals(req.getStatus())) {
            result.addAll(queryHistoricProcessInstances(req));
        }

        return result;
    }

    /**
     * 查询历史流程实例
     *
     * @param req 查询请求
     * @return 流程实例VO列表
     */
    private List<ProcessInstanceVO> queryHistoricProcessInstances(QueryProcessInstanceReq req) {
        org.flowable.engine.history.HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery()
                .finished();

        if (req.getProcessDefinitionKey() != null && !req.getProcessDefinitionKey().isEmpty()) {
            historyQuery.processDefinitionKey(req.getProcessDefinitionKey());
        }
        if (req.getStartUserId() != null && !req.getStartUserId().isEmpty()) {
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
     * 执行用户任务查询
     *
     * @param userId 用户ID
     * @return 任务VO列表
     */
    private List<TaskVO> executeUserTasksQuery(String userId) {
        // 查询用户作为受理人的任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        // 查询用户作为候选人的任务
        List<Task> candidateTasks = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        tasks.addAll(candidateTasks);

        return tasks.stream()
                .map(this::convertToTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * 执行待办任务查询
     *
     * @param req 查询请求
     * @return 任务VO列表
     */
    private List<TaskVO> executePendingTasksQuery(QueryTaskReq req) {
        org.flowable.task.api.TaskQuery query = taskService.createTaskQuery();

        if (req.getUserId() != null && !req.getUserId().isEmpty()) {
            query.taskCandidateOrAssigned(req.getUserId());
        }
        if (req.getTaskName() != null && !req.getTaskName().isEmpty()) {
            query.taskNameLike("%" + req.getTaskName() + "%");
        }
        if (req.getProcessDefinitionKey() != null && !req.getProcessDefinitionKey().isEmpty()) {
            query.processDefinitionKey(req.getProcessDefinitionKey());
        }

        List<Task> tasks = query
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream()
                .map(this::convertToTaskVO)
                .collect(Collectors.toList());
    }

    /**
     * 执行已办任务查询
     *
     * @param req 查询请求
     * @return 历史任务VO列表
     */
    private List<HistoricTaskVO> executeCompletedTasksQuery(QueryTaskReq req) {
        org.flowable.task.api.history.HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

        if (req.getUserId() != null && !req.getUserId().isEmpty()) {
            query.taskAssignee(req.getUserId());
        }
        if (req.getTaskName() != null && !req.getTaskName().isEmpty()) {
            query.taskNameLike("%" + req.getTaskName() + "%");
        }
        if (req.getProcessDefinitionKey() != null && !req.getProcessDefinitionKey().isEmpty()) {
            query.processDefinitionKey(req.getProcessDefinitionKey());
        }

        query.finished();

        List<HistoricTaskInstance> historicTasks = query
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        return historicTasks.stream()
                .map(this::convertToHistoricTaskVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 - 业务处理 ====================

    /**
     * 启动流程实例
     *
     * @param req       启动请求
     * @param variables 流程变量
     * @return 流程实例
     */
    private ProcessInstance doStartProcessInstance(StartProcessReq req, Map<String, Object> variables) {
        if (req.getBusinessKey() != null && !req.getBusinessKey().isEmpty()) {
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
     *
     * @param req 请假请求
     * @return 流程变量
     */
    private Map<String, Object> buildLeaveProcessVariables(LeaveApprovalReq req) {
        Map<String, Object> variables = new HashMap<>(16);
        variables.put("applicant", req.getApplicantId());
        variables.put("applicantName", req.getApplicantName());
        variables.put("leaveType", req.getLeaveType());
        variables.put("startDate", req.getStartDate());
        variables.put("endDate", req.getEndDate());
        variables.put("leaveDays", req.getLeaveDays());
        variables.put("reason", req.getReason());
        return variables;
    }

    /**
     * 生成流程图
     *
     * @param bpmnModel          BPMN模型
     * @param activeActivityIds 活动节点ID列表
     * @return 图片字节数组
     */
    private byte[] generateProcessDiagram(BpmnModel bpmnModel, List<String> activeActivityIds) throws Exception {
        ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        InputStream inputStream = diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                activeActivityIds,
                Collections.emptyList(),
                "宋体",
                "宋体",
                "宋体",
                null,
                1.0,
                true
        );

        if (inputStream == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GENERATE_DIAGRAM_ERROR);
        }

        return inputStream.readAllBytes();
    }

    /**
     * 认领任务（如果未被认领）
     *
     * @param task 任务对象
     * @param req  完成任务请求
     */
    private void claimTaskIfNotAssigned(Task task, CompleteTaskReq req) {
        if (task.getAssignee() == null) {
            taskService.claim(req.getTaskId(), req.getUserId());
        }
    }

    /**
     * 添加任务评论
     *
     * @param task 任务对象
     * @param req  完成任务请求
     */
    private void addTaskComment(Task task, CompleteTaskReq req) {
        if (req.getComment() != null && !req.getComment().isEmpty()) {
            taskService.addComment(req.getTaskId(), task.getProcessInstanceId(), req.getComment());
        }
    }

    /**
     * 构建任务变量
     *
     * @param req 完成任务请求
     * @return 流程变量
     */
    private Map<String, Object> buildTaskVariables(CompleteTaskReq req) {
        Map<String, Object> variables = req.getVariables();
        if (variables == null) {
            variables = new HashMap<>(8);
        }
        if (req.getApproved() != null) {
            variables.put("approved", req.getApproved());
        }
        return variables;
    }

    /**
     * 构建评论映射
     *
     * @param processInstanceId 流程实例ID
     * @return 任务ID -> 评论内容的映射
     */
    private Map<String, String> buildCommentMap(String processInstanceId) {
        Map<String, String> commentMap = new HashMap<>();
        taskService.getProcessInstanceComments(processInstanceId).forEach(comment -> {
            if (comment.getTaskId() != null) {
                commentMap.put(comment.getTaskId(), comment.getFullMessage());
            }
        });
        return commentMap;
    }

    // ==================== 私有方法 - 对象转换 ====================

    /**
     * 转换ProcessDefinition为ProcessDefinitionVO
     *
     * @param pd                流程定义
     * @param latestVersionKeys 最新版本映射
     * @return 流程定义VO
     */
    private ProcessDefinitionVO convertToProcessDefinitionVO(ProcessDefinition pd, Map<String, String> latestVersionKeys) {
        ProcessDefinitionVO vo = new ProcessDefinitionVO();
        vo.setProcessDefinitionId(pd.getId());
        vo.setProcessDefinitionKey(pd.getKey());
        vo.setProcessDefinitionName(pd.getName());
        vo.setDescription(pd.getDescription());
        vo.setVersion(pd.getVersion());
        vo.setDeploymentId(pd.getDeploymentId());
        vo.setSuspended(pd.isSuspended());
        vo.setLatestVersion(latestVersionKeys.get(pd.getKey()).equals(pd.getId()));
        return vo;
    }

    /**
     * 转换ProcessInstance为ProcessInstanceVO
     *
     * @param pi     流程实例
     * @param status 状态
     * @return 流程实例VO
     */
    private ProcessInstanceVO convertToProcessInstanceVO(ProcessInstance pi, String status) {
        String processName = getProcessDefinitionName(pi.getProcessDefinitionId());
        Map<String, Object> processVariables = getProcessVariables(pi.getId());
        String currentActivityName = getCurrentActivityName(pi);

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
     *
     * @param hpi 历史流程实例
     * @return 流程实例VO
     */
    private ProcessInstanceVO convertToHistoricProcessInstanceVO(HistoricProcessInstance hpi) {
        String processName = getProcessDefinitionName(hpi.getProcessDefinitionId());

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
                .status("completed")
                .build();
    }

    /**
     * 转换ProcessInstance为ProcessInstanceDetailRsp
     *
     * @param pi     流程实例
     * @param status 状态
     * @return 流程实例详情响应
     */
    private ProcessInstanceDetailRsp convertToProcessInstanceDetailRsp(ProcessInstance pi, String status) {
        String processName = getProcessDefinitionName(pi.getProcessDefinitionId());
        Map<String, Object> processVariables = getProcessVariables(pi.getId());
        String currentActivityName = getCurrentActivityName(pi);

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
     *
     * @param hpi 历史流程实例
     * @return 流程实例详情响应
     */
    private ProcessInstanceDetailRsp convertToHistoricProcessInstanceDetailRsp(HistoricProcessInstance hpi) {
        String processName = getProcessDefinitionName(hpi.getProcessDefinitionId());

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
                .status("completed")
                .build();
    }

    /**
     * 转换Task为TaskVO
     *
     * @param task 任务
     * @return 任务VO
     */
    private TaskVO convertToTaskVO(Task task) {
        Map<String, Object> processVariables = getProcessVariables(task.getProcessInstanceId());
        String processName = getProcessDefinitionName(task.getProcessDefinitionId());

        return TaskVO.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .taskDescription(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processName(processName)
                .assignee(task.getAssignee())
                .createTime(task.getCreateTime() != null ?
                        LocalDateTime.ofInstant(task.getCreateTime().toInstant(), ZoneId.systemDefault()) : null)
                .dueDate(task.getDueDate() != null ?
                        LocalDateTime.ofInstant(task.getDueDate().toInstant(), ZoneId.systemDefault()) : null)
                .priority(task.getPriority())
                .processVariables(processVariables)
                .build();
    }

    /**
     * 转换HistoricTaskInstance为HistoricTaskVO
     *
     * @param hti 历史任务实例
     * @return 历史任务VO
     */
    private HistoricTaskVO convertToHistoricTaskVO(HistoricTaskInstance hti) {
        String processName = getProcessDefinitionName(hti.getProcessDefinitionId());
        String processDefinitionKey = getProcessDefinitionKey(hti.getProcessDefinitionId());

        return HistoricTaskVO.builder()
                .taskId(hti.getId())
                .taskName(hti.getName())
                .taskDescription(hti.getDescription())
                .processInstanceId(hti.getProcessInstanceId())
                .processDefinitionId(hti.getProcessDefinitionId())
                .processDefinitionKey(processDefinitionKey)
                .processName(processName)
                .assignee(hti.getAssignee())
                .createTime(hti.getCreateTime() != null ?
                        LocalDateTime.ofInstant(hti.getCreateTime().toInstant(), ZoneId.systemDefault()) : null)
                .endTime(hti.getEndTime() != null ?
                        LocalDateTime.ofInstant(hti.getEndTime().toInstant(), ZoneId.systemDefault()) : null)
                .durationInMillis(hti.getDurationInMillis())
                .deleteReason(hti.getDeleteReason())
                .priority(hti.getPriority())
                .build();
    }

    /**
     * 转换HistoricActivityInstance为ProcessHistoryRsp
     *
     * @param activity   历史活动实例
     * @param commentMap 评论映射
     * @return 流程历史响应
     */
    private ProcessHistoryRsp convertToProcessHistoryRsp(HistoricActivityInstance activity, Map<String, String> commentMap) {
        return ProcessHistoryRsp.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType())
                .taskId(activity.getTaskId())
                .assignee(activity.getAssignee())
                .startTime(activity.getStartTime() != null ?
                        LocalDateTime.ofInstant(activity.getStartTime().toInstant(), ZoneId.systemDefault()) : null)
                .endTime(activity.getEndTime() != null ?
                        LocalDateTime.ofInstant(activity.getEndTime().toInstant(), ZoneId.systemDefault()) : null)
                .durationInMillis(activity.getDurationInMillis())
                .comment(commentMap.get(activity.getTaskId()))
                .status(activity.getEndTime() != null ? "completed" : "pending")
                .build();
    }

    // ==================== 私有方法 - 辅助查询 ====================

    /**
     * 获取流程定义名称
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程定义名称
     */
    private String getProcessDefinitionName(String processDefinitionId) {
        try {
            var processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                return processDefinition.getName();
            }
        } catch (Exception e) {
            log.warn("获取流程定义名称失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取流程定义KEY
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程定义KEY
     */
    private String getProcessDefinitionKey(String processDefinitionId) {
        try {
            var processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                return processDefinition.getKey();
            }
        } catch (Exception e) {
            log.warn("获取流程定义KEY失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取流程变量
     *
     * @param processInstanceId 流程实例ID
     * @return 流程变量
     */
    private Map<String, Object> getProcessVariables(String processInstanceId) {
        try {
            return runtimeService.getVariables(processInstanceId);
        } catch (Exception e) {
            log.warn("获取流程变量失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前活动节点名称
     *
     * @param pi 流程实例
     * @return 当前活动节点名称
     */
    private String getCurrentActivityName(ProcessInstance pi) {
        try {
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
            if (!activeActivityIds.isEmpty()) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
                if (bpmnModel != null) {
                    var flowElement = bpmnModel.getFlowElement(activeActivityIds.get(0));
                    if (flowElement != null) {
                        return flowElement.getName();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取当前活动节点失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查用户是否有权限处理任务
     *
     * @param task   任务
     * @param userId 用户ID
     * @return 是否有权限
     */
    private boolean checkTaskPermission(Task task, String userId) {
        // 如果用户是任务受理人
        if (userId.equals(task.getAssignee())) {
            return true;
        }

        // 如果用户是候选人
        if (task.getAssignee() == null) {
            long count = taskService.createTaskQuery()
                    .taskId(task.getId())
                    .taskCandidateUser(userId)
                    .count();
            return count > 0;
        }

        return false;
    }
}