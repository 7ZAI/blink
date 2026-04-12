package com.blink.base.service.flowable;

import org.flowable.engine.runtime.ProcessInstance;

import java.util.List;
import java.util.Map;

/**
 * Flowable查询辅助服务接口
 * <p>
 * 封装共享的查询和转换逻辑，供各子Service使用
 * </p>
 *
 * @author binblink
 */
public interface FlowableQueryHelper {

    /**
     * 获取流程定义名称
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程定义名称
     */
    String getProcessDefinitionName(String processDefinitionId);

    /**
     * 获取流程定义KEY
     *
     * @param processDefinitionId 流程定义ID
     * @return 流程定义KEY
     */
    String getProcessDefinitionKey(String processDefinitionId);

    /**
     * 获取流程变量（已过滤敏感信息）
     *
     * @param processInstanceId 流程实例ID
     * @return 流程变量
     */
    Map<String, Object> getProcessVariables(String processInstanceId);

    /**
     * 获取当前活动节点名称
     *
     * @param pi 流程实例
     * @return 当前活动节点名称
     */
    String getCurrentActivityName(ProcessInstance pi);

    /**
     * 获取用户所属候选组列表
     *
     * @param userId 用户ID
     * @return 候选组ID列表
     */
    List<String> getUserGroups(String userId);

    /**
     * 判断用户是否为流程管理员
     *
     * @param userId 用户ID
     * @return 是否为管理员
     */
    boolean isProcessAdmin(String userId);

    /**
     * 获取任务审批意见
     *
     * @param taskId 任务ID
     * @return 审批意见
     */
    String getTaskComment(String taskId);
}