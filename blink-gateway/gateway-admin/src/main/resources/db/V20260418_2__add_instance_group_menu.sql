-- 实例分组菜单
-- 在监控中心目录下添加实例分组管理菜单
-- @author binblink
-- @since 2026-04-18

SET NAMES utf8mb4;

-- ============================================
-- 1. 查询监控中心菜单ID
-- ============================================
SET @monitor_parent_id = (SELECT menu_id FROM sys_menu WHERE url IS NULL AND menu_name = '监控中心' AND delFlag = 0 LIMIT 1);

-- ============================================
-- 2. 插入实例分组菜单
-- 放置在仪表盘之后，告警管理之前
-- 使用 menu_id 119 开始，避免与现有菜单冲突
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(119, '实例分组', 'InstanceGroup', 2, 'Grid', '/monitor/instance-group', 2, 0, @monitor_parent_id, 2, 'views/monitor/instance-group/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 更新其他菜单的排序，让实例分组排在正确位置
-- 仪表盘 order_number=1 保持不变
-- 实例分组 order_number=2
-- 告警管理、熔断器监控等向后顺延
-- ============================================
UPDATE sys_menu SET order_number = 3 WHERE parent_id = @monitor_parent_id AND menu_name = '告警管理' AND delFlag = 0;
UPDATE sys_menu SET order_number = 4 WHERE parent_id = @monitor_parent_id AND menu_name = '熔断器监控' AND delFlag = 0;

-- ============================================
-- 4. 实例分组按钮权限
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(120, '新增分组', 'AddInstanceGroup', 3, NULL, NULL, 1, 0, 119, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(121, '编辑分组', 'EditInstanceGroup', 3, NULL, NULL, 2, 0, 119, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(122, '删除分组', 'DeleteInstanceGroup', 3, NULL, NULL, 3, 0, 119, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 5. 角色菜单关联
-- 超级管理员(role_id=1)不需要关联数据，已特殊处理
-- 为网关管理员(role_id=2)和网关运维(role_id=3)添加实例分组菜单
-- ============================================
INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id IN (119, 120, 121, 122) AND delFlag = 0;

INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id IN (119, 120, 121, 122) AND delFlag = 0;

SELECT '实例分组菜单初始化完成!' AS message;
