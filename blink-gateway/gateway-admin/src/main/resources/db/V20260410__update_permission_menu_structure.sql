-- ============================================
-- 更新权限管理菜单结构以匹配前端路由
-- 添加数据权限列表菜单，调整路由地址
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 修改接口权限菜单的 URL
-- ============================================
UPDATE sys_menu
SET url = '/system/permission/api-permission'
WHERE menu_id = 12 AND menu_name = '接口权限';

-- ============================================
-- 2. 新增数据权限列表菜单
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(48, '数据权限列表', 'DataPermissionList', 2, 'List', '/system/permission/data-permission/list', 1, 0, 13, 4, 'views/system/permission/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 修改过滤规则菜单的 URL 和顺序
-- ============================================
UPDATE sys_menu
SET url = '/system/permission/data-permission/rule', order_number = 2
WHERE menu_id = 14 AND menu_name = '过滤规则';

-- ============================================
-- 4. 添加数据权限列表按钮权限 (menu_id 从 54 开始，因为 49-53 已被占用)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(54, '新增数据权限', 'AddDataPermission', 3, NULL, NULL, 1, 0, 48, 5, NULL, NULL, 0, 'admin', NOW(), 0),
(55, '编辑数据权限', 'EditDataPermission', 3, NULL, NULL, 2, 0, 48, 5, NULL, NULL, 0, 'admin', NOW(), 0),
(56, '删除数据权限', 'DeleteDataPermission', 3, NULL, NULL, 3, 0, 48, 5, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 5. 为超级管理员角色添加新菜单关联
-- ============================================
INSERT INTO sys_role_menu_rela (role_id, menu_id) VALUES
(1, 48), (1, 54), (1, 55), (1, 56);

SELECT '权限管理菜单结构更新完成!' AS message;