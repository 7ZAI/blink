-- ============================================
-- Blink Base App 初始化数据脚本
-- 执行顺序：在 blink.sql (表结构) 执行后执行
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 字典类型数据
-- ============================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark, locale) VALUES
('性别', 'sys_sex', 0, 'admin', NOW(), '用户性别字典', 'zh_cn'),
('用户状态', 'sys_user_status', 0, 'admin', NOW(), '用户锁定状态', 'zh_cn'),
('菜单类型', 'sys_menu_type', 0, 'admin', NOW(), '菜单类型字典', 'zh_cn'),
('显示状态', 'sys_show_status', 0, 'admin', NOW(), '显示隐藏状态', 'zh_cn'),
('权限类型', 'sys_permission_type', 0, 'admin', NOW(), '权限类型字典', 'zh_cn'),
('角色类型', 'sys_role_type', 0, 'admin', NOW(), '角色类型字典', 'zh_cn'),
('通用状态', 'sys_normal_status', 0, 'admin', NOW(), '通用启用禁用状态', 'zh_cn'),
('数据范围规则类型', 'sys_data_scope_rule', 0, 'admin', NOW(), '数据范围规则类型', 'zh_cn'),
('语言类型', 'sys_locale', 0, 'admin', NOW(), '语言类型字典', 'zh_cn'),
('是否', 'sys_yes_no', 0, 'admin', NOW(), '是否字典', 'zh_cn');

-- 英文版本
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark, locale) VALUES
('Gender', 'sys_sex', 0, 'admin', NOW(), 'User gender dictionary', 'en_us'),
('User Status', 'sys_user_status', 0, 'admin', NOW(), 'User lock status', 'en_us'),
('Menu Type', 'sys_menu_type', 0, 'admin', NOW(), 'Menu type dictionary', 'en_us'),
('Show Status', 'sys_show_status', 0, 'admin', NOW(), 'Show/Hide status', 'en_us'),
('Permission Type', 'sys_permission_type', 0, 'admin', NOW(), 'Permission type dictionary', 'en_us'),
('Role Type', 'sys_role_type', 0, 'admin', NOW(), 'Role type dictionary', 'en_us'),
('Normal Status', 'sys_normal_status', 0, 'admin', NOW(), 'Enable/Disable status', 'en_us'),
('Data Scope Rule Type', 'sys_data_scope_rule', 0, 'admin', NOW(), 'Data scope rule type', 'en_us'),
('Locale', 'sys_locale', 0, 'admin', NOW(), 'Locale dictionary', 'en_us'),
('Yes/No', 'sys_yes_no', 0, 'admin', NOW(), 'Yes/No dictionary', 'en_us');

-- ============================================
-- 2. 字典数据 - 中文
-- ============================================

-- 性别 (sys_sex)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_sex', '男', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_sex', '女', '2', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_sex', '不确定', '3', NULL, 'info', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 用户状态 (sys_user_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_user_status', '正常', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '管理员锁定', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '密码锁定', '2', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 菜单类型 (sys_menu_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_menu_type', '目录', '1', NULL, 'primary', 0, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '菜单', '2', NULL, 'success', 1, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '按钮', '3', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 显示状态 (sys_show_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_show_status', '显示', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_show_status', '隐藏', '1', NULL, 'info', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 权限类型 (sys_permission_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_permission_type', 'API权限', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_permission_type', '数据权限', '2', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 角色类型 (sys_role_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_role_type', '自定义角色', '0', NULL, 'info', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_role_type', '系统角色', '1', NULL, 'primary', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 通用状态 (sys_normal_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_normal_status', '启用', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_normal_status', '禁用', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 数据范围规则类型 (sys_data_scope_rule)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_data_scope_rule', '字段过滤', 'FIELD_FILTER', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '创建者过滤', 'CREATOR_FILTER', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '日期范围过滤', 'DATE_RANGE_FILTER', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn'),
('sys_data_scope_rule', '自定义SQL', 'CUSTOM_SQL', NULL, 'info', 0, 0, 4, 'admin', NOW(), 'zh_cn');

-- 语言类型 (sys_locale)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_locale', '简体中文', 'zh_cn', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_locale', 'English', 'en_us', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 是否 (sys_yes_no)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_yes_no', '是', '1', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_yes_no', '否', '0', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- ============================================
-- 3. 字典数据 - 英文
-- ============================================

-- Gender (sys_sex)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_sex', 'Male', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_sex', 'Female', '2', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_sex', 'Unknown', '3', NULL, 'info', 0, 0, 3, 'admin', NOW(), 'en_us');

-- User Status (sys_user_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_user_status', 'Normal', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_user_status', 'Admin Locked', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_user_status', 'Password Locked', '2', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us');

-- Menu Type (sys_menu_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_menu_type', 'Directory', '1', NULL, 'primary', 0, 0, 1, 'admin', NOW(), 'en_us'),
('sys_menu_type', 'Menu', '2', NULL, 'success', 1, 0, 2, 'admin', NOW(), 'en_us'),
('sys_menu_type', 'Button', '3', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us');

-- Show Status (sys_show_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_show_status', 'Show', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_show_status', 'Hide', '1', NULL, 'info', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Permission Type (sys_permission_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_permission_type', 'API Permission', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_permission_type', 'Data Permission', '2', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Role Type (sys_role_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_role_type', 'Custom Role', '0', NULL, 'info', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_role_type', 'System Role', '1', NULL, 'primary', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Normal Status (sys_normal_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_normal_status', 'Enabled', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_normal_status', 'Disabled', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Data Scope Rule Type (sys_data_scope_rule)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_data_scope_rule', 'Field Filter', 'FIELD_FILTER', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Creator Filter', 'CREATOR_FILTER', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Date Range Filter', 'DATE_RANGE_FILTER', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'en_us'),
('sys_data_scope_rule', 'Custom SQL', 'CUSTOM_SQL', NULL, 'info', 0, 0, 4, 'admin', NOW(), 'en_us');

-- Locale (sys_locale)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_locale', 'Simplified Chinese', 'zh_cn', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_locale', 'English', 'en_us', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'en_us');

-- Yes/No (sys_yes_no)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_yes_no', 'Yes', '1', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'en_us'),
('sys_yes_no', 'No', '0', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'en_us');

-- ============================================
-- 4. 角色数据
-- ============================================

INSERT INTO sys_role (role_id, role_name, role_en_name, status, role_code, role_type, create_by, create_time, update_by, update_time, delFlag) VALUES
(1, '超级管理员', 'Super Admin', 0, 'admin:super', 1, 'admin', NOW(), NULL, NULL, 0);

-- ============================================
-- 5. 超级管理员用户
-- 密码: 123456 (BCrypt加密)
-- ============================================

INSERT INTO sys_user (user_id, login_name, password, username, avatar, avatar_style, sex, phone, email, locked, salt, psw_retry, superFlag, remark, create_by, create_time, delFlag) VALUES
(1, 'admin', '$2a$10$5InOZPGXsu4I9mEIfpYkdesbyLTrfn3YmQwISWqicSZ01OZzgLDkG', '超级管理员', NULL, 'fun-emoji', 3, NULL, NULL, 0, NULL, 0, 1, '系统超级管理员', 'admin', NOW(), 0);

-- ============================================
-- 6. 用户角色关联
-- ============================================

INSERT INTO sys_user_role_rela (user_id, role_id) VALUES (1, 1);

-- ============================================
-- 7. 菜单数据 (核心系统菜单)
-- ============================================

-- 一级菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(1, '首页', 'Dashboard', 2, 'HomeFilled', '/dashboard', 1, 0, 0, 1, 'views/dashboard/index.vue', 0, 'admin', NOW(), 0),
(2, '系统管理', 'System', 1, 'Setting', '/system', 2, 0, 0, 1, NULL, 1, 'admin', NOW(), 0);

-- 二级菜单 (系统管理子菜单)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(3, '用户管理', 'User', 1, 'User', NULL, 1, 0, 2, 2, NULL, 1, 'admin', NOW(), 0),
(4, '菜单管理', 'Menu', 2, 'Menu', '/system/menu', 2, 0, 2, 2, 'views/system/menu/index.vue', 0, 'admin', NOW(), 0),
(5, '角色管理', 'Role', 2, 'UserFilled', '/system/role', 3, 0, 2, 2, 'views/system/role/index.vue', 0, 'admin', NOW(), 0),
(6, '组织管理', 'Group', 2, 'OfficeBuilding', '/system/group', 4, 0, 2, 2, 'views/system/group/index.vue', 0, 'admin', NOW(), 0),
(7, '权限管理', 'Permission', 1, 'Lock', NULL, 5, 0, 2, 2, NULL, 1, 'admin', NOW(), 0),
(18, '设置', 'System Config', 2, 'Tools', '/system/config', 6, 0, 2, 2, 'views/system/config/index.vue', 0, 'admin', NOW(), 0),
(23, '字典管理', 'Dict', 1, 'Notebook', NULL, 7, 0, 2, 2, NULL, 1, 'admin', NOW(), 0),
(26, '操作日志', 'OperationLog', 2, 'Document', '/system/operation-log', 8, 0, 2, 2, 'views/system/operation-log/index.vue', 0, 'admin', NOW(), 0);

-- 三级菜单 (用户管理子菜单)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(8, '用户列表', 'UserList', 2, NULL, '/system/user', 1, 0, 3, 3, 'views/system/user/index.vue', 0, 'admin', NOW(), 0),
(9, '在线用户', 'OnlineUser', 2, NULL, '/system/online-user', 2, 0, 3, 3, 'views/system/online-user/index.vue', 0, 'admin', NOW(), 0);

-- 用户管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(11, '编辑用户', 'EditUser', 3, NULL, 1, 0, 8, 3, 'admin', NOW(), 0),
(12, '删除用户', 'DeleteUser', 3, NULL, 2, 0, 8, 3, 'admin', NOW(), 0),
(13, '批量删除', 'BatchDelete', 3, NULL, 3, 0, 8, 3, 'admin', NOW(), 0),
(14, '用户详情', 'UserDetail', 3, NULL, 4, 0, 8, 3, 'admin', NOW(), 0),
(15, '锁定用户', 'LockUser', 3, NULL, 5, 0, 8, 3, 'admin', NOW(), 0),
(16, '解锁用户', 'UnlockUser', 3, NULL, 6, 0, 8, 3, 'admin', NOW(), 0),
(32, '新增用户', 'AddUser', 3, NULL, 0, 0, 8, 3, 'admin', NOW(), 0),
(33, '分配角色', 'AssignRole', 3, NULL, 7, 0, 8, 3, 'admin', NOW(), 0),
(34, '重置密码', 'ResetPassword', 3, NULL, 8, 0, 8, 3, 'admin', NOW(), 0);

-- 在线用户按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(62, '刷新列表', 'RefreshOnlineUser', 3, NULL, 1, 0, 9, 3, 'admin', NOW(), 0),
(63, '强制下线', 'KickoutUser', 3, NULL, 2, 0, 9, 3, 'admin', NOW(), 0);

-- 权限管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(29, '接口权限', 'ApiPermission', 2, '/system/permission', 1, 0, 7, 3, 'views/system/permission/index.vue', 0, 'admin', NOW(), 0),
(30, '数据权限管理', 'DataFilterPermission', 1, NULL, 2, 0, 7, 3, NULL, 1, 'admin', NOW(), 0);

-- 数据权限管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(27, '过滤规则', 'DataScope', 2, '/system/data-filter', 1, 0, 30, 4, 'admin', NOW(), 0);

-- 过滤规则按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(52, '新增过滤规则', 'AddDataFilter', 3, NULL, 1, 0, 27, 5, 'admin', NOW(), 0),
(53, '编辑过滤规则', 'EditDataFilter', 3, NULL, 2, 0, 27, 5, 'admin', NOW(), 0),
(54, '删除过滤规则', 'DeleteDataFilter', 3, NULL, 3, 0, 27, 5, 'admin', NOW(), 0),
(55, '过滤规则详情', 'DataFilterDetail', 3, NULL, 4, 0, 27, 5, 'admin', NOW(), 0);

-- 接口权限按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(49, '新增权限', 'AddPermission', 3, NULL, 1, 0, 29, 4, 'admin', NOW(), 0),
(50, '编辑权限', 'EditPermission', 3, NULL, 2, 0, 29, 4, 'admin', NOW(), 0),
(51, '删除权限', 'DeletePermission', 3, NULL, 3, 0, 29, 4, 'admin', NOW(), 0);

-- 菜单管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(43, '新增菜单', 'AddMenu', 3, NULL, 1, 0, 4, 3, 'admin', NOW(), 0),
(44, '编辑菜单', 'EditMenu', 3, NULL, 2, 0, 4, 3, 'admin', NOW(), 0),
(45, '删除菜单', 'DeleteMenu', 3, NULL, 3, 0, 4, 3, 'admin', NOW(), 0);

-- 角色管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(35, '新增角色', 'AddRole', 3, NULL, 1, 0, 5, 3, 'admin', NOW(), 0),
(36, '编辑角色', 'EditRole', 3, NULL, 2, 0, 5, 3, 'admin', NOW(), 0),
(37, '删除角色', 'DeleteRole', 3, NULL, 3, 0, 5, 3, 'admin', NOW(), 0),
(38, '批量删除角色', 'BatchDeleteRole', 3, NULL, 4, 0, 5, 3, 'admin', NOW(), 0),
(39, '分配权限', 'AssignPermission', 3, NULL, 5, 0, 5, 3, 'admin', NOW(), 0),
(40, '分配菜单', 'AssignMenu', 3, NULL, 6, 0, 5, 3, 'admin', NOW(), 0),
(41, '分配用户', 'AssignUser', 3, NULL, 7, 0, 5, 3, 'admin', NOW(), 0),
(42, '角色详情', 'RoleDetail', 3, NULL, 8, 0, 5, 3, 'admin', NOW(), 0);

-- 组织管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(46, '新增组织', 'AddGroup', 3, NULL, 1, 0, 6, 3, 'admin', NOW(), 0),
(47, '编辑组织', 'EditGroup', 3, NULL, 2, 0, 6, 3, 'admin', NOW(), 0),
(48, '删除组织', 'DeleteGroup', 3, NULL, 3, 0, 6, 3, 'admin', NOW(), 0);

-- 设置按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(67, '保存配置', 'SaveConfig', 3, NULL, 1, 0, 18, 3, 'admin', NOW(), 0),
(68, '取消修改', 'CancelConfig', 3, NULL, 2, 0, 18, 3, 'admin', NOW(), 0);

-- 字典管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(24, '字典类型', 'DictType', 2, '/system/dict-type', 1, 0, 23, 3, 'views/system/dict-type/index.vue', 0, 'admin', NOW(), 0),
(25, '字典数据', 'DictData', 2, '/system/dict-data', 2, 0, 23, 3, 'views/system/dict-data/index.vue', 0, 'admin', NOW(), 0);

-- 字典类型按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(56, '新增字典类型', 'AddDictType', 3, NULL, 1, 0, 24, 4, 'admin', NOW(), 0),
(57, '编辑字典类型', 'EditDictType', 3, NULL, 2, 0, 24, 4, 'admin', NOW(), 0),
(58, '删除字典类型', 'DeleteDictType', 3, NULL, 3, 0, 24, 4, 'admin', NOW(), 0);

-- 字典数据按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(59, '新增字典数据', 'AddDictData', 3, NULL, 1, 0, 25, 4, 'admin', NOW(), 0),
(60, '编辑字典数据', 'EditDictData', 3, NULL, 2, 0, 25, 4, 'admin', NOW(), 0),
(61, '删除字典数据', 'DeleteDictData', 3, NULL, 3, 0, 25, 4, 'admin', NOW(), 0);

-- 操作日志按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, url, order_number, status, parent_id, menu_level, create_by, create_time, delFlag) VALUES
(64, '查询日志', 'SearchLog', 3, NULL, 1, 0, 26, 3, 'admin', NOW(), 0),
(65, '重置搜索', 'ResetLogSearch', 3, NULL, 2, 0, 26, 3, 'admin', NOW(), 0),
(66, '查看详情', 'ViewLogDetail', 3, NULL, 3, 0, 26, 3, 'admin', NOW(), 0);

-- ============================================
-- 8. 角色菜单关联 (超级管理员拥有所有菜单)
-- ============================================

INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE delFlag = 0;

-- ============================================
-- 9. 系统配置
-- ============================================

INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark) VALUES
('base:site:name', '站点名称', 'Blink管理系统', 0, 1, '站点名称', 0, 0, 'admin', NOW(), '系统站点名称'),
('base:site:copyright', '版权信息', 'Copyright © 2024 Blink. All rights reserved.', 0, 1, '版权信息', 0, 0, 'admin', NOW(), '页脚版权信息'),
('base:system:title', '系统标题', 'Blink Management', 0, 1, '系统标题', 0, 0, 'admin', NOW(), '浏览器标题'),
('base:system:logo', '系统Logo', '/logo.png', 0, 1, '系统Logo', 0, 0, 'admin', NOW(), '系统Logo路径'),
('base:system:footer', '页脚信息', 'Powered by Blink', 0, 1, '页脚信息', 0, 0, 'admin', NOW(), '页脚显示内容'),
('base:page:size', '分页大小', '10', 1, 2, '默认分页大小', 0, 0, 'admin', NOW(), '列表默认每页显示条数'),
('base:allowed:file:types', '允许上传的文件类型', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', 0, 3, '允许上传的文件类型', 0, 0, 'admin', NOW(), '文件上传类型限制'),
('base:upload:maxSize', '上传文件最大大小(MB)', '10', 1, 3, '上传文件最大大小', 0, 0, 'admin', NOW(), '单文件最大上传大小'),
('base:upload:allowTypes', '允许上传的文件类型', 'image/jpeg,image/png,image/gp,application/pdf,application/msword,application/vnd.ms-excel', 0, 3, '允许上传的MIME类型', 0, 0, 'admin', NOW(), 'MIME类型限制'),
('base:user:defaultAvatar', '用户默认头像', '/avatar/default.png', 0, 4, '用户默认头像', 0, 0, 'admin', NOW(), '新用户默认头像'),
('base:user:defaultAvatarStyle', '用户默认头像样式', 'fun-emoji', 0, 4, '用户默认头像样式', 0, 0, 'admin', NOW(), 'DiceBear头像样式'),
('base:session:timeout', '会话超时时间', '7200', 1, 4, '会话超时时间(秒)', 0, 0, 'admin', NOW(), '用户会话超时时间，默认2小时'),
('base:password:complexity', '密码复杂度要求', '^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$', 0, 4, '密码复杂度正则', 0, 0, 'admin', NOW(), '密码复杂度正则表达式'),
('base:user:passwordMinLength', '密码最小长度', '6', 1, 4, '密码最小长度', 0, 0, 'admin', NOW(), '密码最小长度要求'),
('base:session:kickoutAfter', '踢出后登录', 'true', 2, 4, '踢出后是否允许重新登录', 0, 0, 'admin', NOW(), '新登录是否踢出旧会话'),
('base:user:initPassword', '用户初始密码', '123456', 0, 4, '用户初始密码', 0, 0, 'admin', NOW(), '新增用户默认密码'),
('base:log:enableOperationLog', '启用操作日志', 'true', 2, 5, '是否记录操作日志', 0, 0, 'admin', NOW(), '启用操作日志记录'),
('base:log:enableLoginLog', '启用登录日志', 'true', 2, 5, '是否记录登录日志', 0, 0, 'admin', NOW(), '启用登录日志记录'),
('base:log:retentionDays', '日志保留天数', '90', 1, 5, '日志保留天数', 0, 0, 'admin', NOW(), '日志保留天数'),
('base:log:enabled', '日志总开关', 'true', 2, 5, '日志总开关', 0, 0, 'admin', NOW(), '日志功能总开关'),
('base:session:maxConcurrent', '最大并发会话数', '3', 1, 4, '最大并发会话数', 0, 0, 'admin', NOW(), '同一用户最大同时在线设备数'),
('base:login:captcha:enabled', '登录验证码开关', 'true', 2, 6, '登录验证码开关', 0, 0, 'admin', NOW(), '是否启用验证码'),
('base:login:password:maxRetry', '密码最大重试次数', '5', 1, 6, '密码最大重试次数', 0, 0, 'admin', NOW(), '密码错误最大重试次数'),
('base:login:password:lockTime', '账户锁定时间(分钟)', '30', 1, 6, '账户锁定时间', 0, 0, 'admin', NOW(), '密码错误次数过多后锁定时间');

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Blink Base App 初始化数据完成!' AS message;