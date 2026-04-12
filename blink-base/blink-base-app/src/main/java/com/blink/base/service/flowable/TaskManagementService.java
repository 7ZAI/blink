package com.blink.base.service.flowable;

import com.blink.base.dto.req.CompleteTaskReq;
import com.blink.base.dto.req.DelegateTaskReq;
import com.blink.base.dto.req.QueryTaskReq;
import com.blink.base.dto.rsp.HistoricTaskRsp;
import com.blink.base.dto.rsp.TaskRsp;
import com.blink.framework.common.exception.BlinkException;
import org.flowable.task.api.Task;

/**
 * 任务管理服务接口
 * <p>
 * 提供任务的管理功能：查询、完成、委托、认领、撤回
 * </p>
 *
 * @author binblink
 */
public interface TaskManagementService {

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
     * @param taskId 任务ID
     * @param userId 撤回人ID
     * @param reason 撤回原因
     * @throws BlinkException 撤回失败时抛出
     */
    void withdrawTask(String taskId, String userId, String reason) throws BlinkException;

    /**
     * 检查用户是否有权限处理任务
     *
     * @param task   任务
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean checkTaskPermission(Task task, String userId);
}