package com.blink.base.controller;

import com.blink.base.dto.req.ClaimTaskReq;
import com.blink.base.dto.req.CompleteTaskReq;
import com.blink.base.dto.req.DelegateTaskReq;
import com.blink.base.dto.req.DeleteProcessInstanceReq;
import com.blink.base.dto.req.DeployProcessReq;
import com.blink.base.dto.req.DeploymentIdReq;
import com.blink.base.dto.req.ImportXmlProcessReq;
import com.blink.base.dto.req.LeaveApprovalReq;
import com.blink.base.dto.req.ProcessDefinitionIdReq;
import com.blink.base.dto.req.ProcessInstanceIdReq;
import com.blink.base.dto.req.QueryMyProcessReq;
import com.blink.base.dto.req.QueryProcessDefinitionReq;
import com.blink.base.dto.req.QueryProcessInstanceReq;
import com.blink.base.dto.req.QueryTaskReq;
import com.blink.base.dto.req.RollbackProcessReq;
import com.blink.base.dto.req.StartProcessReq;
import com.blink.base.dto.req.TaskIdReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.req.WithdrawTaskReq;
import com.blink.base.dto.rsp.HistoricTaskRsp;
import com.blink.base.dto.rsp.ProcessDefinitionRsp;
import com.blink.base.dto.rsp.ProcessHistoryRsp;
import com.blink.base.dto.rsp.ProcessInstanceDetailRsp;
import com.blink.base.dto.rsp.ProcessInstanceRsp;
import com.blink.base.dto.rsp.TaskRsp;
import com.blink.base.dto.vo.ProcessInstanceVO;
import com.blink.base.service.FlowableProcessService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程管理前端控制器
 * <p>
 * 提供工作流相关的REST API接口，包括流程定义管理、流程实例管理、任务管理等功能
 * </p>
 *
 * @author binblink
 */
@RestController
@RequestMapping("/workflow")
@Validated
public class FlowableProcessController {

    @Resource
    private FlowableProcessService flowableProcessService;

    // ==================== 流程定义管理 ====================

    /**
     * 部署流程定义
     *
     * @param reqDto 部署请求
     * @return {@link ResponseDTO<String>}
     */
    @PostMapping("/deployProcess")
    public ResponseDTO<String> deployProcess(@RequestBody @Valid RequestDTO<DeployProcessReq> reqDto) {
        String deploymentId = flowableProcessService.deployProcess(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(deploymentId);
    }

    /**
     * 分页查询流程定义列表
     *
     * @param reqDto 查询请求
     * @return {@link ResponseDTO<ProcessDefinitionRsp>}
     */
    @PostMapping("/getProcessDefinitionList")
    public ResponseDTO<ProcessDefinitionRsp> getProcessDefinitionList(
            @RequestBody @Valid RequestDTO<QueryProcessDefinitionReq> reqDto) {
        ProcessDefinitionRsp result = flowableProcessService.getProcessDefinitionList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 获取流程图XML
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<String>}
     */
    @PostMapping("/getProcessDiagramXml")
    public ResponseDTO<String> getProcessDiagramXml(@RequestBody @Valid RequestDTO<ProcessDefinitionIdReq> reqDto) {
        String xml = flowableProcessService.getProcessDiagramXml(reqDto.getBody().getProcessDefinitionId());
        return ResponseDTO.newSuccessInstance(xml);
    }

    /**
     * 获取流程实例的流程图图片（高亮当前节点）
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<byte[]>}
     */
    @PostMapping("/getProcessDiagramImage")
    public ResponseDTO<byte[]> getProcessDiagramImage(@RequestBody @Valid RequestDTO<ProcessInstanceIdReq> reqDto) {
        byte[] imageBytes = flowableProcessService.getProcessDiagramImage(reqDto.getBody().getProcessInstanceId());
        return ResponseDTO.newSuccessInstance(imageBytes);
    }

    /**
     * 挂起流程定义
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/suspendProcessDefinition")
    public ResponseDTO<EmptyBody> suspendProcessDefinition(@RequestBody @Valid RequestDTO<ProcessDefinitionIdReq> reqDto) {
        flowableProcessService.suspendProcessDefinition(reqDto.getBody().getProcessDefinitionId());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 激活流程定义
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/activateProcessDefinition")
    public ResponseDTO<EmptyBody> activateProcessDefinition(@RequestBody @Valid RequestDTO<ProcessDefinitionIdReq> reqDto) {
        flowableProcessService.activateProcessDefinition(reqDto.getBody().getProcessDefinitionId());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除流程定义
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/deleteProcessDefinition")
    public ResponseDTO<EmptyBody> deleteProcessDefinition(@RequestBody @Valid RequestDTO<DeploymentIdReq> reqDto) {
        flowableProcessService.deleteProcessDefinition(
                reqDto.getBody().getDeploymentId(),
                Boolean.TRUE.equals(reqDto.getBody().getCascade()));
        return ResponseDTO.newSuccessInstance();
    }

    // ==================== 流程实例管理 ====================

    /**
     * 启动流程实例
     *
     * @param reqDto 启动请求
     * @return {@link ResponseDTO<ProcessInstanceVO>}
     */
    @PostMapping("/startProcess")
    public ResponseDTO<ProcessInstanceVO> startProcess(@RequestBody @Valid RequestDTO<StartProcessReq> reqDto) {
        ProcessInstanceVO result = flowableProcessService.startProcess(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 启动请假审批流程
     *
     * @param reqDto 请假申请请求
     * @return {@link ResponseDTO<ProcessInstanceVO>}
     */
    @PostMapping("/startLeaveProcess")
    public ResponseDTO<ProcessInstanceVO> startLeaveProcess(@RequestBody @Valid RequestDTO<LeaveApprovalReq> reqDto) {
        ProcessInstanceVO result = flowableProcessService.startLeaveProcess(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 分页查询流程实例列表
     *
     * @param reqDto 查询请求
     * @return {@link ResponseDTO<ProcessInstanceRsp>}
     */
    @PostMapping("/getProcessInstanceList")
    public ResponseDTO<ProcessInstanceRsp> getProcessInstanceList(
            @RequestBody @Valid RequestDTO<QueryProcessInstanceReq> reqDto) {
        ProcessInstanceRsp result = flowableProcessService.getProcessInstanceList(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 查询用户发起的流程实例
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<List<ProcessInstanceDetailRsp>>}
     */
    @PostMapping("/getMyProcessInstances")
    public ResponseDTO<List<ProcessInstanceDetailRsp>> getMyProcessInstances(
            @RequestBody @Valid RequestDTO<QueryMyProcessReq> reqDto) {
        List<ProcessInstanceDetailRsp> result = flowableProcessService.getMyProcessInstances(
                reqDto.getBody().getUserId(),
                reqDto.getBody().getStatus());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 查询流程实例详情
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<ProcessInstanceDetailRsp>}
     */
    @PostMapping("/getProcessInstanceDetail")
    public ResponseDTO<ProcessInstanceDetailRsp> getProcessInstanceDetail(
            @RequestBody @Valid RequestDTO<ProcessInstanceIdReq> reqDto) {
        ProcessInstanceDetailRsp result = flowableProcessService.getProcessInstanceDetail(
                reqDto.getBody().getProcessInstanceId());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 删除流程实例
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/deleteProcessInstance")
    public ResponseDTO<EmptyBody> deleteProcessInstance(
            @RequestBody @Valid RequestDTO<DeleteProcessInstanceReq> reqDto) {
        flowableProcessService.deleteProcessInstance(
                reqDto.getBody().getProcessInstanceId(),
                reqDto.getBody().getReason());
        return ResponseDTO.newSuccessInstance();
    }

    // ==================== 任务管理 ====================

    /**
     * 查询用户待办任务
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<TaskRsp>}
     */
    @PostMapping("/getUserTasks")
    public ResponseDTO<TaskRsp> getUserTasks(@RequestBody @Valid RequestDTO<UserIdReq> reqDto) {
        TaskRsp result = flowableProcessService.getUserTasks(String.valueOf(reqDto.getBody().getUserId()));
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 分页查询待办任务
     *
     * @param reqDto 查询请求
     * @return {@link ResponseDTO<TaskRsp>}
     */
    @PostMapping("/getPendingTasks")
    public ResponseDTO<TaskRsp> getPendingTasks(@RequestBody @Valid RequestDTO<QueryTaskReq> reqDto) {
        TaskRsp result = flowableProcessService.getPendingTasks(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 分页查询已办任务
     *
     * @param reqDto 查询请求
     * @return {@link ResponseDTO<HistoricTaskRsp>}
     */
    @PostMapping("/getCompletedTasks")
    public ResponseDTO<HistoricTaskRsp> getCompletedTasks(@RequestBody @Valid RequestDTO<QueryTaskReq> reqDto) {
        HistoricTaskRsp result = flowableProcessService.getCompletedTasks(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 完成任务
     *
     * @param reqDto 完成任务请求
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/completeTask")
    public ResponseDTO<EmptyBody> completeTask(@RequestBody @Valid RequestDTO<CompleteTaskReq> reqDto) {
        flowableProcessService.completeTask(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 委托任务
     *
     * @param reqDto 委托任务请求
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/delegateTask")
    public ResponseDTO<EmptyBody> delegateTask(@RequestBody @Valid RequestDTO<DelegateTaskReq> reqDto) {
        flowableProcessService.delegateTask(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 认领任务
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/claimTask")
    public ResponseDTO<EmptyBody> claimTask(@RequestBody @Valid RequestDTO<ClaimTaskReq> reqDto) {
        flowableProcessService.claimTask(reqDto.getBody().getTaskId(), reqDto.getBody().getUserId());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 取消认领任务
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/unclaimTask")
    public ResponseDTO<EmptyBody> unclaimTask(@RequestBody @Valid RequestDTO<TaskIdReq> reqDto) {
        flowableProcessService.unclaimTask(reqDto.getBody().getTaskId());
        return ResponseDTO.newSuccessInstance();
    }

    // ==================== 流程历史 ====================

    /**
     * 查询流程历史
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<List<ProcessHistoryRsp>>}
     */
    @PostMapping("/getProcessHistory")
    public ResponseDTO<List<ProcessHistoryRsp>> getProcessHistory(
            @RequestBody @Valid RequestDTO<ProcessInstanceIdReq> reqDto) {
        List<ProcessHistoryRsp> history = flowableProcessService.getProcessHistory(
                reqDto.getBody().getProcessInstanceId());
        return ResponseDTO.newSuccessInstance(history);
    }

    // ==================== 新增功能 ====================

    /**
     * 导入BPMN XML流程定义
     *
     * @param reqDto 导入请求（包含XML内容）
     * @return {@link ResponseDTO<String>} 部署ID
     */
    @PostMapping("/importProcessFromXml")
    public ResponseDTO<String> importProcessFromXml(@RequestBody @Valid RequestDTO<ImportXmlProcessReq> reqDto) {
        String deploymentId = flowableProcessService.importProcessFromXml(reqDto.getBody());
        return ResponseDTO.newSuccessInstance(deploymentId);
    }

    /**
     * 回退流程到指定节点
     *
     * @param reqDto 回退请求
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/rollbackProcess")
    public ResponseDTO<EmptyBody> rollbackProcess(@RequestBody @Valid RequestDTO<RollbackProcessReq> reqDto) {
        flowableProcessService.rollbackProcess(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 撤回任务（发起人撤回未处理的任务）
     *
     * @param reqDto 撤回请求
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/withdrawTask")
    public ResponseDTO<EmptyBody> withdrawTask(@RequestBody @Valid RequestDTO<WithdrawTaskReq> reqDto) {
        flowableProcessService.withdrawTask(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }
}