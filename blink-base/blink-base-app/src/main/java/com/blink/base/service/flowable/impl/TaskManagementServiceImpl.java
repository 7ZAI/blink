package com.blink.base.service.flowable.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.WorkflowConstant;
import com.blink.base.dto.req.CompleteTaskReq;
import com.blink.base.dto.req.DelegateTaskReq;
import com.blink.base.dto.req.QueryTaskReq;
import com.blink.base.dto.rsp.HistoricTaskRsp;
import com.blink.base.dto.rsp.TaskRsp;
import com.blink.base.dto.vo.HistoricTaskVO;
import com.blink.base.dto.vo.TaskVO;
import com.blink.base.service.flowable.FlowableQueryHelper;
import com.blink.base.service.flowable.TaskManagementService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务管理服务实现类
 * <p>
 * 提供任务的管理功能：查询、完成、委托、认领、撤回
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowableQueryHelper flowableQueryHelper;

    public TaskManagementServiceImpl(TaskService taskService,
                                      RuntimeService runtimeService,
                                      HistoryService historyService,
                                      FlowableQueryHelper flowableQueryHelper) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.flowableQueryHelper = flowableQueryHelper;
    }

    @Override
    public TaskRsp getUserTasks(String userId) throws BlinkException {
        try {
            log.debug("[Workflow] 查询用户待办任务 | userId: {}", userId);

            TaskRsp rsp = new TaskRsp();
            QueryTaskReq req = new QueryTaskReq();

            return PageUtils.<QueryTaskReq, TaskVO, TaskRsp>queryPage(req, () -> executeUserTasksQuery(userId), rsp);

        } catch (Exception e) {
            log.error("[Workflow] 查询用户待办任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询用户待办任务失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_TASK_ERROR);
        }
    }

    @Override
    public TaskRsp getPendingTasks(QueryTaskReq req) throws BlinkException {
        try {
            log.debug("[Workflow] 分页查询待办任务 | userId: {}", req.getUserId());

            TaskRsp rsp = new TaskRsp();

            return PageUtils.<QueryTaskReq, TaskVO, TaskRsp>queryPage(req, () -> executePendingTasksQuery(req), rsp);

        } catch (Exception e) {
            log.error("[Workflow] 分页查询待办任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("分页查询待办任务失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_PENDING_TASK_ERROR);
        }
    }

    @Override
    public HistoricTaskRsp getCompletedTasks(QueryTaskReq req) throws BlinkException {
        try {
            log.debug("[Workflow] 分页查询已办任务 | userId: {}", req.getUserId());

            HistoricTaskRsp rsp = new HistoricTaskRsp();

            return PageUtils.<QueryTaskReq, HistoricTaskVO, HistoricTaskRsp>queryPage(req, () -> executeCompletedTasksQuery(req), rsp);

        } catch (Exception e) {
            log.error("[Workflow] 分页查询已办任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("分页查询已办任务失败: " + e.getMessage(), e, BaseErrCodeConstant.QUERY_COMPLETED_TASK_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(CompleteTaskReq req) throws BlinkException {
        try {
            log.info("[Workflow] 用户完成任务 | userId: {}, taskId: {}", req.getUserId(), req.getTaskId());

            Task task = taskService.createTaskQuery()
                    .taskId(req.getTaskId())
                    .singleResult();

            if (task == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.TASK_NOT_FOUND);
            }

            boolean hasPermission = checkTaskPermission(task, req.getUserId());
            if (!hasPermission) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NO_TASK_PERMISSION);
            }

            // 认领任务
            claimTaskIfNotAssigned(task, req);

            // 添加审批意见
            addTaskComment(task, req);

            // 构建变量并完成任务
            Map<String, Object> variables = buildTaskVariables(req);
            taskService.complete(req.getTaskId(), variables);
            log.info("[Workflow] 任务完成成功 | taskId: {}", req.getTaskId());

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 完成任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("完成任务失败: " + e.getMessage(), e, BaseErrCodeConstant.COMPLETE_TASK_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(DelegateTaskReq req) throws BlinkException {
        try {
            log.info("[Workflow] 用户委托任务 | currentUserId: {}, taskId: {}, targetUserId: {}", req.getCurrentUserId(), req.getTaskId(), req.getTargetUserId());

            Task task = taskService.createTaskQuery()
                    .taskId(req.getTaskId())
                    .singleResult();

            if (task == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.TASK_NOT_FOUND);
            }

            boolean hasPermission = checkTaskPermission(task, req.getCurrentUserId());
            if (!hasPermission) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NO_TASK_PERMISSION);
            }

            taskService.delegateTask(req.getTaskId(), req.getTargetUserId());
            log.info("[Workflow] 任务委托成功 | taskId: {}", req.getTaskId());

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 委托任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("委托任务失败: " + e.getMessage(), e, BaseErrCodeConstant.DELEGATE_TASK_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) throws BlinkException {
        try {
            log.info("[Workflow] 用户认领任务 | userId: {}, taskId: {}", userId, taskId);
            taskService.claim(taskId, userId);
            log.info("[Workflow] 任务认领成功 | taskId: {}", taskId);
        } catch (Exception e) {
            log.error("[Workflow] 认领任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("认领任务失败: " + e.getMessage(), e, BaseErrCodeConstant.CLAIM_TASK_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unclaimTask(String taskId) throws BlinkException {
        try {
            log.info("[Workflow] 取消认领任务 | taskId: {}", taskId);
            taskService.unclaim(taskId);
            log.info("[Workflow] 任务取消认领成功 | taskId: {}", taskId);
        } catch (Exception e) {
            log.error("[Workflow] 取消认领任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("取消认领任务失败: " + e.getMessage(), e, BaseErrCodeConstant.UNCLAIM_TASK_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTask(String taskId, String userId, String reason) throws BlinkException {
        try {
            log.info("[Workflow] 用户撤回任务 | userId: {}, taskId: {}", userId, taskId);

            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.TASK_NOT_FOUND);
            }

            // 校验权限：只有发起人可以撤回未处理的任务
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();

            if (pi == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PROCESS_INSTANCE_NOT_FOUND);
            }

            if (!StrUtil.equals(userId, pi.getStartUserId())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NO_TASK_PERMISSION);
            }

            // 检查任务是否已被处理
            if (StrUtil.isNotBlank(task.getAssignee())) {
                // 任务已被认领，需要先取消认领
                taskService.unclaim(taskId);
            }

            // 回退到提交申请节点
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdTo(task.getTaskDefinitionKey(), WorkflowConstant.ACTIVITY_ID_SUBMIT_LEAVE)
                    .changeState();

            log.info("[Workflow] 任务撤回成功 | taskId: {}", taskId);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Workflow] 撤回任务失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("撤回任务失败: " + e.getMessage(), e, BaseErrCodeConstant.WITHDRAW_TASK_ERROR);
        }
    }

    @Override
    public boolean checkTaskPermission(Task task, String userId) {
        // 1. 受理人直接权限
        if (StrUtil.equals(userId, task.getAssignee())) {
            return true;
        }

        // 2. 候选人权限
        if (task.getAssignee() == null) {
            // 检查候选人
            if (taskService.createTaskQuery()
                    .taskId(task.getId())
                    .taskCandidateUser(userId)
                    .count() > 0) {
                return true;
            }

            // 3. 候选组权限
            List<String> userGroups = flowableQueryHelper.getUserGroups(userId);
            if (CollUtil.isNotEmpty(userGroups)) {
                if (taskService.createTaskQuery()
                        .taskId(task.getId())
                        .taskCandidateGroupIn(userGroups)
                        .count() > 0) {
                    return true;
                }
            }
        }

        // 4. 管理员超权
        return flowableQueryHelper.isProcessAdmin(userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 执行用户任务查询
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
     */
    private List<TaskVO> executePendingTasksQuery(QueryTaskReq req) {
        org.flowable.task.api.TaskQuery query = taskService.createTaskQuery();

        if (StrUtil.isNotBlank(req.getUserId())) {
            query.taskCandidateOrAssigned(req.getUserId());
        }
        if (StrUtil.isNotBlank(req.getTaskName())) {
            query.taskNameLike(WorkflowConstant.LIKE_PREFIX + req.getTaskName() + WorkflowConstant.LIKE_SUFFIX);
        }
        if (StrUtil.isNotBlank(req.getProcessDefinitionKey())) {
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
     */
    private List<HistoricTaskVO> executeCompletedTasksQuery(QueryTaskReq req) {
        org.flowable.task.api.history.HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

        if (StrUtil.isNotBlank(req.getUserId())) {
            query.taskAssignee(req.getUserId());
        }
        if (StrUtil.isNotBlank(req.getTaskName())) {
            query.taskNameLike(WorkflowConstant.LIKE_PREFIX + req.getTaskName() + WorkflowConstant.LIKE_SUFFIX);
        }
        if (StrUtil.isNotBlank(req.getProcessDefinitionKey())) {
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

    /**
     * 认领任务（如果未被认领）
     */
    private void claimTaskIfNotAssigned(Task task, CompleteTaskReq req) {
        if (task.getAssignee() == null) {
            taskService.claim(req.getTaskId(), req.getUserId());
        }
    }

    /**
     * 添加任务评论
     */
    private void addTaskComment(Task task, CompleteTaskReq req) {
        if (StrUtil.isNotBlank(req.getComment())) {
            taskService.addComment(req.getTaskId(), task.getProcessInstanceId(), req.getComment());
        }
    }

    /**
     * 构建任务变量
     */
    private Map<String, Object> buildTaskVariables(CompleteTaskReq req) {
        Map<String, Object> variables = req.getVariables();
        if (variables == null) {
            variables = new HashMap<>(WorkflowConstant.DEFAULT_VARIABLE_MAP_SIZE);
        }
        if (req.getApproved() != null) {
            variables.put(WorkflowConstant.VAR_APPROVED, req.getApproved());
        }
        return variables;
    }

    /**
     * 转换Task为TaskVO
     */
    private TaskVO convertToTaskVO(Task task) {
        Map<String, Object> processVariables = flowableQueryHelper.getProcessVariables(task.getProcessInstanceId());
        String processName = flowableQueryHelper.getProcessDefinitionName(task.getProcessDefinitionId());

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
     */
    private HistoricTaskVO convertToHistoricTaskVO(HistoricTaskInstance hti) {
        String processName = flowableQueryHelper.getProcessDefinitionName(hti.getProcessDefinitionId());
        String processDefinitionKey = flowableQueryHelper.getProcessDefinitionKey(hti.getProcessDefinitionId());

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
}