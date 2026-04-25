-- 添加路由分组菜单到路由管理目录
-- 路由管理目录 (menu_id=55) 和路由仓库等子菜单已存在
-- 只需添加路由分组菜单
-- @author binblink
-- @since 2026-04-18

SET NAMES utf8mb4;

-- ============================================
-- 1. 查找路由管理目录ID (menu_id=55)
-- ============================================
SET @route_parent_id = (SELECT menu_id FROM sys_menu WHERE menu_id = 55 AND delFlag = 0 LIMIT 1);

-- ============================================
-- 2. 添加路由分组菜单
-- 放在路由仓库之前，order_number=1
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 66, '路由分组', 'RouteGroup', 2, 'Grid', '/route/group', 1, 0, @route_parent_id, 3, 'views/routeGroup/index.vue', NULL, 0, 'admin', NOW(), 0
WHERE @route_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE url = '/route/group' AND delFlag = 0);

-- ============================================
-- 3. 更新其他路由管理子菜单的排序
-- 路由分组 order_number=1
-- 路由仓库 order_number=2
-- 推送路由 order_number=3
-- 实例路由 order_number=4
-- 推送历史 order_number=5
-- ============================================
UPDATE sys_menu SET order_number = 2 WHERE url = '/route/repository' AND delFlag = 0;
UPDATE sys_menu SET order_number = 3 WHERE url = '/route/push' AND delFlag = 0;
UPDATE sys_menu SET order_number = 4 WHERE url = '/route/instance' AND delFlag = 0;
UPDATE sys_menu SET order_number = 5 WHERE url = '/route/push-history' AND delFlag = 0;

-- ============================================
-- 4. 路由分组按钮权限
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 67, '新增分组', 'AddRouteGroup', 3, NULL, NULL, 1, 0, 66, 4, NULL, NULL, 0, 'admin', NOW(), 0
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 66 AND delFlag = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 67 AND delFlag = 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 68, '编辑分组', 'EditRouteGroup', 3, NULL, NULL, 2, 0, 66, 4, NULL, NULL, 0, 'admin', NOW(), 0
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 66 AND delFlag = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 68 AND delFlag = 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 69, '删除分组', 'DeleteRouteGroup', 3, NULL, NULL, 3, 0, 66, 4, NULL, NULL, 0, 'admin', NOW(), 0
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 66 AND delFlag = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 69 AND delFlag = 0);

SELECT '路由分组菜单添加完成!' AS message;
