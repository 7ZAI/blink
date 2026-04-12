package com.blink.base.service.flowable.impl;

import cn.hutool.core.collection.CollUtil;
import com.blink.base.constants.WorkflowConstant;
import com.blink.base.service.flowable.FlowableQueryHelper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Flowable查询辅助服务实现类
 * <p>
 * 封装共享的查询和转换逻辑，供各子Service使用
 * </p>
 *
 * @author binblink
 */
@Service
@Slf4j
public class FlowableQueryHelperImpl implements FlowableQueryHelper {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public FlowableQueryHelperImpl(RepositoryService repositoryService,
                                    RuntimeService runtimeService,
                                    TaskService taskService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public String getProcessDefinitionName(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                return processDefinition.getName();
            }
        } catch (Exception e) {
            log.warn("[Workflow] 获取流程定义名称失败 | processDefinitionId: {}", processDefinitionId);
        }
        return null;
    }

    @Override
    public String getProcessDefinitionKey(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition != null) {
                return processDefinition.getKey();
            }
        } catch (Exception e) {
            log.warn("[Workflow] 获取流程定义KEY失败 | processDefinitionId: {}", processDefinitionId);
        }
        return null;
    }

    @Override
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        try {
            Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
            // 过滤敏感变量
            if (CollUtil.isNotEmpty(variables)) {
                WorkflowConstant.SENSITIVE_VARIABLES.forEach(variables::remove);
            }
            return variables;
        } catch (Exception e) {
            log.warn("[Workflow] 获取流程变量失败 | processInstanceId: {}", processInstanceId);
            return Collections.emptyMap();
        }
    }

    @Override
    public String getCurrentActivityName(ProcessInstance pi) {
        try {
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
            if (CollUtil.isNotEmpty(activeActivityIds)) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
                if (bpmnModel != null) {
                    var flowElement = bpmnModel.getFlowElement(activeActivityIds.get(0));
                    if (flowElement != null) {
                        return flowElement.getName();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Workflow] 获取当前活动节点失败 | processInstanceId: {}", pi.getId());
        }
        return null;
    }

    @Override
    public List<String> getUserGroups(String userId) {
        // TODO: 从系统用户-组关联表查询用户所属组
        // 转换为Flowable组ID格式
        // 示例实现，实际应从sysUserGroupRelaMapper查询
        return Collections.emptyList();
    }

    @Override
    public boolean isProcessAdmin(String userId) {
        // TODO: 检查用户是否拥有流程管理权限
        // 实际应从sysUserService.getSysUserDetail查询superFlag
        return false;
    }

    @Override
    public String getTaskComment(String taskId) {
        try {
            if (taskId == null) {
                return null;
            }
            var comments = taskService.getTaskComments(taskId);
            if (CollUtil.isNotEmpty(comments)) {
                // 返回最新的审批意见
                return comments.get(comments.size() - 1).getFullMessage();
            }
        } catch (Exception e) {
            log.warn("[Workflow] 获取任务审批意见失败 | taskId: {}", taskId);
        }
        return null;
    }
}