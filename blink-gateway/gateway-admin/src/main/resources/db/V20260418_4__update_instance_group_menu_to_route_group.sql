-- 将实例分组菜单改为路由分组菜单，并移动到路由管理目录
-- @author binblink
-- @since 2026-04-18

SET NAMES utf8mb4;

-- ============================================
-- 1. 更新菜单名称和路径
-- ============================================
UPDATE sys_menu SET menu_name = '路由分组', menu_en_name = 'RouteGroup' WHERE menu_id = 119 AND delFlag = 0;

-- ============================================
-- 2. 更新按钮权限名称
-- ============================================
UPDATE sys_menu SET menu_name = '新增分组', menu_en_name = 'AddRouteGroup' WHERE menu_id = 120 AND delFlag = 0;
UPDATE sys_menu SET menu_name = '编辑分组', menu_en_name = 'EditRouteGroup' WHERE menu_id = 121 AND delFlag = 0;
UPDATE sys_menu SET menu_name = '删除分组', menu_en_name = 'DeleteRouteGroup' WHERE menu_id = 122 AND delFlag = 0;

-- ============================================
-- 3. 将路由分组菜单从监控中心移动到路由管理目录
-- 前端路由结构: /route/group
-- 路由管理是一个虚拟目录，菜单通过 url 定位
-- ============================================

-- 查找路由仓库菜单的父ID（路由管理目录）
SET @route_parent_id = (SELECT parent_id FROM sys_menu WHERE url = '/route/repository' AND delFlag = 0 LIMIT 1);

-- 更新路由分组菜单
UPDATE sys_menu
SET
  parent_id = @route_parent_id,
  menu_level = 3,
  url = '/route/group',
  component_path = 'views/routeGroup/index.vue',
  order_number = 1
WHERE menu_id = 119 AND delFlag = 0;

-- ============================================
-- 4. 更新按钮权限的 menu_level
-- ============================================
UPDATE sys_menu SET menu_level = 4 WHERE menu_id IN (120, 121, 122) AND delFlag = 0;

-- ============================================
-- 5. 调整路由管理目录下其他菜单的排序
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

SELECT '路由分组菜单移动到路由管理目录完成!' AS message;
