-- ============================================
-- 字典数据迁移初始化脚本
-- @author binblink
-- @since 2026-03-21
-- ============================================

-- ============================================
-- 1. 插入字典类型
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
-- 2. 插入字典数据 - 中文
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
-- 3. 插入字典数据 - 英文
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