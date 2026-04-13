package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.dto.req.ApprovalLeaveReq;
import com.blink.base.dto.req.QueryLeaveReq;
import com.blink.base.dto.req.SubmitLeaveReq;
import com.blink.base.dto.rsp.LeaveRequestRsp;
import com.blink.base.dto.vo.LeaveApprovalVO;
import com.blink.base.dto.vo.LeaveRequestVO;
import com.blink.base.entity.BizLeaveApprovalDO;
import com.blink.base.entity.BizLeaveRequestDO;
import com.blink.base.entity.SysGroupDO;
import com.blink.base.entity.SysUserDO;
import com.blink.base.mapper.BizLeaveApprovalMapper;
import com.blink.base.mapper.BizLeaveRequestMapper;
import com.blink.base.mapper.SysGroupMapper;
import com.blink.base.mapper.SysUserGroupRelaMapper;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.service.LeaveRequestService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假申请服务实现类
 *
 * @author binblink
 */
@Slf4j
@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private static final String PROCESS_DEF_KEY = "leaveRequest";
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_CANCELLED = "cancelled";

    @Resource
    private BizLeaveRequestMapper leaveRequestMapper;

    @Resource
    private BizLeaveApprovalMapper leaveApprovalMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysGroupMapper sysGroupMapper;

    @Resource
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLeave(SubmitLeaveReq req) {
        String loginName = BlinkRequestContextHolder.getLoginName();
        String userIdStr = BlinkRequestContextHolder.getUserId();
        Integer userId = Integer.valueOf(userIdStr);

        // 查询用户信息
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        // 1. 保存业务数据
        BizLeaveRequestDO leaveRequest = new BizLeaveRequestDO();
        BeanUtil.copyProperties(req, leaveRequest);
        leaveRequest.setApplicantId(userId);
        leaveRequest.setApplicantName(user.getUsername());

        // 查询用户部门
        Integer deptId = sysUserGroupRelaMapper.selectDeptIdByUserId(userId);
        leaveRequest.setDeptId(deptId);

        leaveRequest.setStatus(STATUS_PENDING);
        leaveRequest.setCreateBy(loginName);
        leaveRequest.setCreateTime(LocalDateTime.now());

        // 查询部门名称
        if (deptId != null) {
            SysGroupDO group = sysGroupMapper.selectById(deptId);
            if (group != null) {
                leaveRequest.setDeptName(group.getGroupName());
            }
        }

        leaveRequestMapper.insert(leaveRequest);

        // 2. 启动流程实例
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", loginName);
        variables.put("applicantName", user.getUsername());
        variables.put("days", req.getDays());
        variables.put("leaveType", req.getLeaveType());
        variables.put("startDate", req.getStartDate().toString());
        variables.put("endDate", req.getEndDate().toString());
        variables.put("reason", req.getReason());
        variables.put("businessKey", leaveRequest.getId().toString());
        // 设置审批人（暂时设置为admin，后续可根据部门动态查询）
        variables.put("approver", "admin");
        variables.put("hrApprover", "admin");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEF_KEY,
                leaveRequest.getId().toString(),
                variables
        );

        // 3. 更新流程实例ID和当前任务
        leaveRequest.setProcessInstanceId(processInstance.getId());

        // 获取当前任务
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        if (currentTask != null) {
            leaveRequest.setCurrentTask(currentTask.getName());
        }

        leaveRequestMapper.updateById(leaveRequest);

        log.info("[LeaveRequest] 提交请假申请成功 | id: {}, processInstanceId: {}, days: {}",
                leaveRequest.getId(), processInstance.getId(), req.getDays());
    }

    @Override
    public LeaveRequestRsp getMyLeaveList(QueryLeaveReq req) {
        String userIdStr = BlinkRequestContextHolder.getUserId();
        Integer userId = Integer.valueOf(userIdStr);
        LeaveRequestRsp rsp = new LeaveRequestRsp();
        PageUtils.queryPage(req,
                () -> leaveRequestMapper.selectByApplicantId(userId, req), rsp);

        // 设置字典名称
        setDictNames(rsp.getRows());

        // 设置是否可取消
        rsp.getRows().forEach(vo -> {
            vo.setCanCancel(STATUS_PENDING.equals(vo.getStatus()));
        });

        return rsp;
    }

    @Override
    public LeaveRequestRsp getPendingApprovalList(QueryLeaveReq req) {
        String loginName = BlinkRequestContextHolder.getLoginName();
        LeaveRequestRsp rsp = new LeaveRequestRsp();
        PageUtils.queryPage(req,
                () -> leaveRequestMapper.selectPendingApproval(loginName, req), rsp);

        // 设置字典名称
        setDictNames(rsp.getRows());

        // 设置是否可审批
        rsp.getRows().forEach(vo -> {
            vo.setCanApprove(STATUS_PENDING.equals(vo.getStatus()));
        });

        return rsp;
    }

    @Override
    public LeaveRequestVO getLeaveDetail(Integer id) {
        LeaveRequestVO vo = leaveRequestMapper.selectDetailById(id);
        if (vo == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LEAVE_REQUEST_NOT_EXIST);
        }

        // 查询审批记录
        List<LeaveApprovalVO> approvalList = leaveApprovalMapper.selectByLeaveRequestId(id);
        vo.setApprovalList(approvalList);

        // 设置字典名称
        setDictNames(List.of(vo));

        // 设置操作权限
        String currentUserIdStr = BlinkRequestContextHolder.getUserId();
        Integer currentUserId = Integer.valueOf(currentUserIdStr);
        vo.setCanCancel(STATUS_PENDING.equals(vo.getStatus()) && currentUserId.equals(vo.getApplicantId()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvalLeave(ApprovalLeaveReq req) {
        String loginName = BlinkRequestContextHolder.getLoginName();
        String userIdStr = BlinkRequestContextHolder.getUserId();
        Integer userId = Integer.valueOf(userIdStr);

        // 1. 查询请假申请
        BizLeaveRequestDO leaveRequest = leaveRequestMapper.selectById(req.getLeaveRequestId());
        if (leaveRequest == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LEAVE_REQUEST_NOT_EXIST);
        }

        if (!STATUS_PENDING.equals(leaveRequest.getStatus())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LEAVE_ALREADY_PROCESSED);
        }

        // 2. 查询当前任务
        Task task = taskService.createTaskQuery()
                .processInstanceId(leaveRequest.getProcessInstanceId())
                .taskCandidateUser(loginName)
                .singleResult();

        if (task == null) {
            // 尝试按代理人查询
            task = taskService.createTaskQuery()
                    .processInstanceId(leaveRequest.getProcessInstanceId())
                    .taskAssignee(loginName)
                    .singleResult();
        }

        if (task == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.NO_LEAVE_APPROVAL_PERMISSION);
        }

        // 3. 认领任务（如果是候选人组任务）
        if (StrUtil.isBlank(task.getAssignee())) {
            taskService.claim(task.getId(), loginName);
        }

        // 4. 完成任务
        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", req.getApprovalResult());
        variables.put("approver", loginName);
        taskService.complete(task.getId(), variables);

        // 5. 保存审批记录
        BizLeaveApprovalDO approval = new BizLeaveApprovalDO();
        approval.setLeaveRequestId(leaveRequest.getId());
        approval.setProcessInstanceId(leaveRequest.getProcessInstanceId());
        approval.setTaskId(task.getId());
        approval.setTaskName(task.getName());
        approval.setApproverId(userId);
        approval.setApproverName(loginName);
        approval.setApprovalResult(req.getApprovalResult());
        approval.setApprovalComment(req.getApprovalComment());
        approval.setApprovalTime(LocalDateTime.now());
        leaveApprovalMapper.insert(approval);

        // 6. 更新业务状态
        SysUserDO user = sysUserMapper.selectById(userId);
        if (STATUS_REJECTED.equals(req.getApprovalResult())) {
            // 拒绝：终止流程
            leaveRequest.setStatus(STATUS_REJECTED);
            runtimeService.deleteProcessInstance(leaveRequest.getProcessInstanceId(), "审批拒绝");
            leaveRequest.setCurrentTask(null);
        } else {
            // 通过：检查流程是否结束
            long count = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(leaveRequest.getProcessInstanceId())
                    .count();
            if (count == 0) {
                // 流程已结束
                leaveRequest.setStatus(STATUS_APPROVED);
                leaveRequest.setCurrentTask(null);
            } else {
                // 更新当前任务
                Task nextTask = taskService.createTaskQuery()
                        .processInstanceId(leaveRequest.getProcessInstanceId())
                        .singleResult();
                if (nextTask != null) {
                    leaveRequest.setCurrentTask(nextTask.getName());
                }
            }
        }

        leaveRequest.setUpdateBy(loginName);
        leaveRequest.setUpdateTime(LocalDateTime.now());
        leaveRequestMapper.updateById(leaveRequest);

        log.info("[LeaveRequest] 审批完成 | id: {}, result: {}, approver: {}",
                req.getLeaveRequestId(), req.getApprovalResult(), loginName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(Integer id) {
        String loginName = BlinkRequestContextHolder.getLoginName();
        String userIdStr = BlinkRequestContextHolder.getUserId();
        Integer userId = Integer.valueOf(userIdStr);

        BizLeaveRequestDO leaveRequest = leaveRequestMapper.selectById(id);
        if (leaveRequest == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LEAVE_REQUEST_NOT_EXIST);
        }

        // 校验权限：只有申请人可以取消
        if (!userId.equals(leaveRequest.getApplicantId())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ONLY_APPLICANT_CAN_CANCEL);
        }

        // 只有待审批状态可以取消
        if (!STATUS_PENDING.equals(leaveRequest.getStatus())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LEAVE_CANNOT_CANCEL);
        }

        // 删除流程实例
        if (StrUtil.isNotBlank(leaveRequest.getProcessInstanceId())) {
            runtimeService.deleteProcessInstance(leaveRequest.getProcessInstanceId(), "用户取消");
        }

        // 更新状态
        leaveRequest.setStatus(STATUS_CANCELLED);
        leaveRequest.setCurrentTask(null);
        leaveRequest.setUpdateBy(loginName);
        leaveRequest.setUpdateTime(LocalDateTime.now());
        leaveRequestMapper.updateById(leaveRequest);

        log.info("[LeaveRequest] 取消请假申请 | id: {}, operator: {}", id, loginName);
    }

    @Override
    public LeaveRequestRsp getAllLeaveList(QueryLeaveReq req) {
        LeaveRequestRsp rsp = new LeaveRequestRsp();
        PageUtils.queryPage(req,
                () -> leaveRequestMapper.selectAllList(req), rsp);

        // 设置字典名称
        setDictNames(rsp.getRows());

        return rsp;
    }

    /**
     * 设置字典名称
     */
    private void setDictNames(List<LeaveRequestVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        Map<String, String> leaveTypeMap = Map.of(
                "annual", "年假",
                "sick", "病假",
                "personal", "事假",
                "compensatory", "调休",
                "marriage", "婚假",
                "maternity", "产假"
        );

        Map<String, String> statusMap = Map.of(
                "draft", "草稿",
                "pending", "待审批",
                "approved", "已通过",
                "rejected", "已拒绝",
                "cancelled", "已取消"
        );

        list.forEach(vo -> {
            vo.setLeaveTypeName(leaveTypeMap.getOrDefault(vo.getLeaveType(), vo.getLeaveType()));
            vo.setStatusName(statusMap.getOrDefault(vo.getStatus(), vo.getStatus()));
        });
    }
}
