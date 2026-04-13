-- ============================================================
-- 请假管理菜单权限配置脚本
-- 创建时间: 2026-04-14
-- 说明: 配置请假管理的菜单和接口权限
-- ============================================================

-- 查询最大菜单ID
SELECT IFNULL(MAX(menu_id), 0) + 1 INTO @max_menu_id FROM sys_menu;
SELECT IFNULL(MAX(ac_id), 0) + 1 INTO @max_perm_id FROM sys_permission;

-- ============================================================
-- 一、创建菜单
-- ============================================================

-- 添加请假管理父菜单
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, icon, sort, status, create_time)
VALUES (@max_menu_id, 0, '请假管理', 0, '/leave', 'Calendar', 50, 0, NOW());

SET @leave_menu_id = @max_menu_id;

-- 添加我的请假子菜单
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, icon, sort, status, create_time)
VALUES (@max_menu_id + 1, @leave_menu_id, '我的请假', 1, '/leave/my', 'leave/my/index', 'Document', 1, 0, NOW());

-- 添加请假审批子菜单
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, icon, sort, status, create_time)
VALUES (@max_menu_id + 2, @leave_menu_id, '请假审批', 1, '/leave/approval', 'leave/approval/index', 'Finished', 2, 0, NOW());

-- 添加请假记录子菜单（管理员）
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, icon, sort, status, create_time)
VALUES (@max_menu_id + 3, @leave_menu_id, '请假记录', 1, '/leave/record', 'leave/record/index', 'List', 3, 0, NOW());

SET @my_leave_menu_id = @max_menu_id + 1;
SET @approval_menu_id = @max_menu_id + 2;
SET @record_menu_id = @max_menu_id + 3;

-- ============================================================
-- 二、创建接口权限
-- ============================================================

-- 提交请假申请
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id, '提交请假申请', '/leave/submit', 1, 0, NOW());

-- 查询我的请假列表
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 1, '查询我的请假列表', '/leave/getMyList', 1, 0, NOW());

-- 查询待审批列表
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 2, '查询待审批列表', '/leave/getPendingList', 1, 0, NOW());

-- 查询请假详情
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 3, '查询请假详情', '/leave/getDetail', 1, 0, NOW());

-- 审批请假
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 4, '审批请假', '/leave/approval', 1, 0, NOW());

-- 取消请假
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 5, '取消请假', '/leave/cancel', 1, 0, NOW());

-- 管理员查询所有请假列表
INSERT INTO sys_permission (ac_id, ac_name, ac_url, ac_type, status, create_time)
VALUES (@max_perm_id + 6, '查询所有请假列表', '/leave/getAllList', 1, 0, NOW());

SET @perm_submit = @max_perm_id;
SET @perm_my_list = @max_perm_id + 1;
SET @perm_pending_list = @max_perm_id + 2;
SET @perm_detail = @max_perm_id + 3;
SET @perm_approval = @max_perm_id + 4;
SET @perm_cancel = @max_perm_id + 5;
SET @perm_all_list = @max_perm_id + 6;

-- ============================================================
-- 三、关联菜单与权限
-- ============================================================

-- 我的请假菜单权限
INSERT INTO sys_menu_perm_rela (menu_id, ac_id) VALUES
(@my_leave_menu_id, @perm_submit),
(@my_leave_menu_id, @perm_my_list),
(@my_leave_menu_id, @perm_detail),
(@my_leave_menu_id, @perm_cancel);

-- 请假审批菜单权限
INSERT INTO sys_menu_perm_rela (menu_id, ac_id) VALUES
(@approval_menu_id, @perm_pending_list),
(@approval_menu_id, @perm_detail),
(@approval_menu_id, @perm_approval);

-- 请假记录菜单权限（管理员）
INSERT INTO sys_menu_perm_rela (menu_id, ac_id) VALUES
(@record_menu_id, @perm_all_list),
(@record_menu_id, @perm_detail);

-- ============================================================
-- 四、关联角色与菜单（普通员工角色）
-- ============================================================

-- 创建普通员工角色（如果不存在）
INSERT INTO sys_role (role_name, role_code, status, create_time)
SELECT '普通员工', 'employee', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'employee');

SET @employee_role_id = (SELECT role_id FROM sys_role WHERE role_code = 'employee');

-- 关联普通员工角色与菜单
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES
(@employee_role_id, @leave_menu_id),
(@employee_role_id, @my_leave_menu_id),
(@employee_role_id, @approval_menu_id);

-- 关联普通员工角色与权限
INSERT INTO sys_role_perm_rela (role_id, ac_id) VALUES
(@employee_role_id, @perm_submit),
(@employee_role_id, @perm_my_list),
(@employee_role_id, @perm_pending_list),
(@employee_role_id, @perm_detail),
(@employee_role_id, @perm_approval),
(@employee_role_id, @perm_cancel);

-- ============================================================
-- 五、关联角色与菜单（部门主管角色）
-- ============================================================

-- 创建部门主管角色（如果不存在）
INSERT INTO sys_role (role_name, role_code, status, create_time)
SELECT '部门主管', 'dept_manager', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'dept_manager');

SET @dept_manager_role_id = (SELECT role_id FROM sys_role WHERE role_code = 'dept_manager');

-- 关联部门主管角色与菜单
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES
(@dept_manager_role_id, @leave_menu_id),
(@dept_manager_role_id, @my_leave_menu_id),
(@dept_manager_role_id, @approval_menu_id);

-- 关联部门主管角色与权限
INSERT INTO sys_role_perm_rela (role_id, ac_id) VALUES
(@dept_manager_role_id, @perm_submit),
(@dept_manager_role_id, @perm_my_list),
(@dept_manager_role_id, @perm_pending_list),
(@dept_manager_role_id, @perm_detail),
(@dept_manager_role_id, @perm_approval),
(@dept_manager_role_id, @perm_cancel);

-- ============================================================
-- 六、关联角色与菜单（HR角色）
-- ============================================================

-- 创建HR角色（如果不存在）
INSERT INTO sys_role (role_name, role_code, status, create_time)
SELECT '人事专员', 'hr_manager', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'hr_manager');

SET @hr_role_id = (SELECT role_id FROM sys_role WHERE role_code = 'hr_manager');

-- 关联HR角色与菜单
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES
(@hr_role_id, @leave_menu_id),
(@hr_role_id, @my_leave_menu_id),
(@hr_role_id, @approval_menu_id),
(@hr_role_id, @record_menu_id);

-- 关联HR角色与权限
INSERT INTO sys_role_perm_rela (role_id, ac_id) VALUES
(@hr_role_id, @perm_submit),
(@hr_role_id, @perm_my_list),
(@hr_role_id, @perm_pending_list),
(@hr_role_id, @perm_detail),
(@hr_role_id, @perm_approval),
(@hr_role_id, @perm_cancel),
(@hr_role_id, @perm_all_list);
