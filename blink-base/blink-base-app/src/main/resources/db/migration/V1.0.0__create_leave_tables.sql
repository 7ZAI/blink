-- ============================================================
-- 请假流程业务表创建脚本
-- 创建时间: 2026-04-14
-- 说明: 包含请假申请表和审批记录表
-- ============================================================

-- 请假申请表
CREATE TABLE IF NOT EXISTS `biz_leave_request` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
    `applicant_id` int NOT NULL COMMENT '申请人ID',
    `applicant_name` varchar(50) NOT NULL COMMENT '申请人姓名',
    `dept_id` int DEFAULT NULL COMMENT '部门ID',
    `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
    `leave_type` varchar(20) NOT NULL COMMENT '请假类型：annual-年假/sick-病假/personal-事假/compensatory-调休/marriage-婚假/maternity-产假',
    `start_date` datetime NOT NULL COMMENT '开始时间',
    `end_date` datetime NOT NULL COMMENT '结束时间',
    `days` decimal(5,1) NOT NULL COMMENT '请假天数',
    `reason` varchar(500) DEFAULT NULL COMMENT '请假原因',
    `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿/pending-待审批/approved-已通过/rejected-已拒绝/cancelled-已取消',
    `current_task` varchar(100) DEFAULT NULL COMMENT '当前任务节点',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- 请假审批记录表
CREATE TABLE IF NOT EXISTS `biz_leave_approval` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `leave_request_id` int NOT NULL COMMENT '请假申请ID',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
    `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
    `task_name` varchar(100) DEFAULT NULL COMMENT '任务名称',
    `approver_id` int NOT NULL COMMENT '审批人ID',
    `approver_name` varchar(50) NOT NULL COMMENT '审批人姓名',
    `approval_result` varchar(20) NOT NULL COMMENT '审批结果：approved-通过/rejected-拒绝',
    `approval_comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
    `approval_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (`id`),
    KEY `idx_leave_request_id` (`leave_request_id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假审批记录表';

-- 初始化请假类型字典
INSERT INTO `sys_dict_type` (`dict_type_id`, `dict_type`, `dict_name`, `status`, `create_time`)
VALUES (100, 'leave_type', '请假类型', 0, NOW())
ON DUPLICATE KEY UPDATE `dict_name` = '请假类型';

INSERT INTO `sys_dict_data` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `list_class`, `status`, `create_time`) VALUES
('leave_type', 'annual', '年假', 1, 'success', 0, NOW()),
('leave_type', 'sick', '病假', 2, 'warning', 0, NOW()),
('leave_type', 'personal', '事假', 3, 'info', 0, NOW()),
('leave_type', 'compensatory', '调休', 4, 'primary', 0, NOW()),
('leave_type', 'marriage', '婚假', 5, 'danger', 0, NOW()),
('leave_type', 'maternity', '产假', 6, 'success', 0, NOW())
ON DUPLICATE KEY UPDATE `dict_label` = VALUES(`dict_label`);

-- 初始化请假状态字典
INSERT INTO `sys_dict_type` (`dict_type_id`, `dict_type`, `dict_name`, `status`, `create_time`)
VALUES (101, 'leave_status', '请假状态', 0, NOW())
ON DUPLICATE KEY UPDATE `dict_name` = '请假状态';

INSERT INTO `sys_dict_data` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `list_class`, `status`, `create_time`) VALUES
('leave_status', 'draft', '草稿', 1, 'info', 0, NOW()),
('leave_status', 'pending', '待审批', 2, 'warning', 0, NOW()),
('leave_status', 'approved', '已通过', 3, 'success', 0, NOW()),
('leave_status', 'rejected', '已拒绝', 4, 'danger', 0, NOW()),
('leave_status', 'cancelled', '已取消', 5, 'info', 0, NOW())
ON DUPLICATE KEY UPDATE `dict_label` = VALUES(`dict_label`);
