-- ============================================
-- Gateway Admin 菜单初始化脚本
-- 添加权限管理、操作日志、字典管理、设置等菜单
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 权限管理菜单 (目录)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(11, '权限管理', 'Permission', 1, 'Lock', NULL, 5, 0, 7, 2, NULL, NULL, 1, 'admin', NOW(), 0);

-- 权限管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(12, '接口权限', 'ApiPermission', 2, 'Key', '/system/permission', 1, 0, 11, 3, 'views/system/permission/index.vue', NULL, 0, 'admin', NOW(), 0),
(13, '数据权限', 'DataPermission', 1, 'DataLine', NULL, 2, 0, 11, 3, NULL, NULL, 1, 'admin', NOW(), 0);

-- 数据权限子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(14, '过滤规则', 'DataFilter', 2, 'Filter', '/system/data-filter', 1, 0, 13, 4, 'views/system/data-filter/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 2. 操作日志菜单
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(15, '操作日志', 'OperationLog', 2, 'Document', '/system/operation-log', 6, 0, 7, 2, 'views/system/operation-log/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 字典管理菜单 (目录)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(16, '字典管理', 'Dict', 1, 'Notebook', NULL, 7, 0, 7, 2, NULL, NULL, 1, 'admin', NOW(), 0);

-- 字典管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(17, '字典类型', 'DictType', 2, 'Collection', '/system/dict-type', 1, 0, 16, 3, 'views/system/dict-type/index.vue', NULL, 0, 'admin', NOW(), 0),
(18, '字典数据', 'DictData', 2, 'List', '/system/dict-data', 2, 0, 16, 3, 'views/system/dict-data/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 4. 设置菜单
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(19, '系统配置', 'SystemConfig', 2, 'Tools', '/system/config', 8, 0, 7, 2, 'views/system/config/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 5. 菜单按钮权限
-- ============================================

-- 用户管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(20, '新增用户', 'AddUser', 3, NULL, NULL, 1, 0, 8, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(21, '编辑用户', 'EditUser', 3, NULL, NULL, 2, 0, 8, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(22, '删除用户', 'DeleteUser', 3, NULL, NULL, 3, 0, 8, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(23, '重置密码', 'ResetPassword', 3, NULL, NULL, 4, 0, 8, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(24, '锁定用户', 'LockUser', 3, NULL, NULL, 5, 0, 8, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- 角色管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(25, '新增角色', 'AddRole', 3, NULL, NULL, 1, 0, 9, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(26, '编辑角色', 'EditRole', 3, NULL, NULL, 2, 0, 9, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(27, '删除角色', 'DeleteRole', 3, NULL, NULL, 3, 0, 9, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(28, '分配权限', 'AssignPermission', 3, NULL, NULL, 4, 0, 9, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(29, '分配菜单', 'AssignMenu', 3, NULL, NULL, 5, 0, 9, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- 菜单管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(30, '新增菜单', 'AddMenu', 3, NULL, NULL, 1, 0, 10, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(31, '编辑菜单', 'EditMenu', 3, NULL, NULL, 2, 0, 10, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(32, '删除菜单', 'DeleteMenu', 3, NULL, NULL, 3, 0, 10, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- 接口权限按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(33, '新增权限', 'AddPermission', 3, NULL, NULL, 1, 0, 12, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(34, '编辑权限', 'EditPermission', 3, NULL, NULL, 2, 0, 12, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(35, '删除权限', 'DeletePermission', 3, NULL, NULL, 3, 0, 12, 4, NULL, NULL, 0, 'admin', NOW(), 0);

-- 过滤规则按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(36, '新增规则', 'AddDataFilter', 3, NULL, NULL, 1, 0, 14, 5, NULL, NULL, 0, 'admin', NOW(), 0),
(37, '编辑规则', 'EditDataFilter', 3, NULL, NULL, 2, 0, 14, 5, NULL, NULL, 0, 'admin', NOW(), 0),
(38, '删除规则', 'DeleteDataFilter', 3, NULL, NULL, 3, 0, 14, 5, NULL, NULL, 0, 'admin', NOW(), 0);

-- 字典类型按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(39, '新增字典类型', 'AddDictType', 3, NULL, NULL, 1, 0, 17, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(40, '编辑字典类型', 'EditDictType', 3, NULL, NULL, 2, 0, 17, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(41, '删除字典类型', 'DeleteDictType', 3, NULL, NULL, 3, 0, 17, 4, NULL, NULL, 0, 'admin', NOW(), 0);

-- 字典数据按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(42, '新增字典数据', 'AddDictData', 3, NULL, NULL, 1, 0, 18, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(43, '编辑字典数据', 'EditDictData', 3, NULL, NULL, 2, 0, 18, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(44, '删除字典数据', 'DeleteDictData', 3, NULL, NULL, 3, 0, 18, 4, NULL, NULL, 0, 'admin', NOW(), 0);

-- 操作日志按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(45, '查询日志', 'SearchLog', 3, NULL, NULL, 1, 0, 15, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(46, '查看详情', 'ViewLogDetail', 3, NULL, NULL, 2, 0, 15, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- 系统配置按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(47, '保存配置', 'SaveConfig', 3, NULL, NULL, 1, 0, 19, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 6. 角色菜单关联 (超级管理员拥有所有菜单)
-- ============================================

INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE delFlag = 0;

-- ============================================
-- 7. 更新系统管理目录的 hasChildren
-- ============================================

UPDATE sys_menu SET hasChildren = 1 WHERE menu_id = 7;

SELECT 'Gateway Admin 菜单初始化完成!' AS message;