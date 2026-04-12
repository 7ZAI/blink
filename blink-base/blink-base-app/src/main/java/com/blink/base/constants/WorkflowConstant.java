package com.blink.base.constants;

import java.util.Set;

/**
 * 工作流常量类
 * <p>
 * 定义流程状态、流程KEY、查询通配符、字体配置等常量
 * </p>
 *
 * @author binblink
 */
public interface WorkflowConstant {

    // ==================== 流程状态 ====================

    /**
     * 流程状态-运行中
     */
    String STATUS_RUNNING = "running";

    /**
     * 流程状态-已完成
     */
    String STATUS_COMPLETED = "completed";

    /**
     * 流程状态-已终止
     */
    String STATUS_TERMINATED = "terminated";

    /**
     * 任务状态-待处理
     */
    String STATUS_PENDING = "pending";

    // ==================== 流程KEY ====================

    /**
     * 请假审批流程KEY
     */
    String PROCESS_KEY_LEAVE = "leaveApproval";

    // ==================== 查询通配符 ====================

    /**
     * LIKE查询前缀
     */
    String LIKE_PREFIX = "%";

    /**
     * LIKE查询后缀
     */
    String LIKE_SUFFIX = "%";

    // ==================== 字体配置 ====================

    /**
     * 流程图字体-宋体
     */
    String FONT_NAME_SONGTI = "宋体";

    // ==================== 图片格式 ====================

    /**
     * 图片格式-PNG
     */
    String IMAGE_FORMAT_PNG = "png";

    // ==================== 变量Map初始大小 ====================

    /**
     * 默认流程变量Map大小
     */
    Integer DEFAULT_VARIABLE_MAP_SIZE = 8;

    /**
     * 请假流程变量Map大小
     */
    Integer LEAVE_VARIABLE_MAP_SIZE = 16;

    // ==================== 敏感变量过滤 ====================

    /**
     * 敏感变量名集合（需在返回前过滤）
     */
    Set<String> SENSITIVE_VARIABLES = Set.of(
            "password",
            "token",
            "appSecret",
            "secretKey",
            "accessToken",
            "refreshToken"
    );

    // ==================== 流程节点ID ====================

    /**
     * 请假流程-提交申请节点ID
     */
    String ACTIVITY_ID_SUBMIT_LEAVE = "submitLeaveTask";

    /**
     * 请假流程-部门经理审批节点ID
     */
    String ACTIVITY_ID_DEPT_MANAGER_APPROVAL = "deptManagerApproval";

    /**
     * 请假流程-HR审批节点ID
     */
    String ACTIVITY_ID_HR_APPROVAL = "hrApproval";

    // ==================== 流程变量名 ====================

    /**
     * 流程变量-申请人
     */
    String VAR_APPLICANT = "applicant";

    /**
     * 流程变量-申请人姓名
     */
    String VAR_APPLICANT_NAME = "applicantName";

    /**
     * 流程变量-请假类型
     */
    String VAR_LEAVE_TYPE = "leaveType";

    /**
     * 流程变量-开始日期
     */
    String VAR_START_DATE = "startDate";

    /**
     * 流程变量-结束日期
     */
    String VAR_END_DATE = "endDate";

    /**
     * 流程变量-请假天数
     */
    String VAR_LEAVE_DAYS = "leaveDays";

    /**
     * 流程变量-请假原因
     */
    String VAR_REASON = "reason";

    /**
     * 流程变量-审批结果
     */
    String VAR_APPROVED = "approved";

    /**
     * 流程变量-发起人ID
     */
    String VAR_START_USER_ID = "startUserId";

    /**
     * 流程变量-发起人姓名
     */
    String VAR_START_USER_NAME = "startUserName";

    // ==================== 候选组 ====================

    /**
     * 候选组-部门经理
     */
    String CANDIDATE_GROUP_DEPT_MANAGER = "dept_manager";

    /**
     * 候选组-HR
     */
    String CANDIDATE_GROUP_HR = "hr";

    /**
     * 候选组-财务
     */
    String CANDIDATE_GROUP_FINANCE = "finance";
}