-- ============================================
-- 监控中心菜单初始化脚本
-- 添加监控中心、告警管理、熔断器监控等菜单
-- 使用 menu_id 100-118 避免与现有菜单冲突
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 监控中心菜单 (顶级目录，parent_id=0)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(100, '监控中心', 'MonitorCenter', 1, 'Monitor', NULL, 4, 0, 0, 1, NULL, NULL, 1, 'admin', NOW(), 0);

-- ============================================
-- 2. 监控中心子菜单 (parent_id=100)
-- ============================================

-- 仪表盘
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(101, '仪表盘', 'Dashboard', 2, 'Odometer', '/monitor/dashboard', 1, 0, 100, 2, 'views/monitor/dashboard/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 告警管理 (目录)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(103, '告警管理', 'Alert', 1, 'Bell', NULL, 2, 0, 100, 2, NULL, NULL, 1, 'admin', NOW(), 0);

-- 告警规则
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(104, '告警规则', 'AlertRule', 2, 'SetUp', '/monitor/alert-rule', 1, 0, 103, 3, 'views/monitor/alert-rule/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 告警历史
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(105, '告警历史', 'AlertHistory', 2, 'List', '/monitor/alert-history', 2, 0, 103, 3, 'views/monitor/alert-history/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 熔断器监控
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(106, '熔断器监控', 'CircuitBreaker', 2, 'Connection', '/monitor/circuit-breaker', 3, 0, 100, 2, 'views/monitor/circuit-breaker/index.vue', NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 告警管理按钮权限 (parent_id=104, 105)
-- ============================================

-- 告警规则按钮 (parent_id=104)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(107, '新增规则', 'AddAlertRule', 3, NULL, NULL, 1, 0, 104, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(108, '编辑规则', 'EditAlertRule', 3, NULL, NULL, 2, 0, 104, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(109, '删除规则', 'DeleteAlertRule', 3, NULL, NULL, 3, 0, 104, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(110, '启用/禁用', 'ToggleAlertRule', 3, NULL, NULL, 4, 0, 104, 4, NULL, NULL, 0, 'admin', NOW(), 0);

-- 告警历史按钮 (parent_id=105)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(111, '查询历史', 'SearchAlertHistory', 3, NULL, NULL, 1, 0, 105, 4, NULL, NULL, 0, 'admin', NOW(), 0),
(112, '确认告警', 'AcknowledgeAlert', 3, NULL, NULL, 2, 0, 105, 4, NULL, NULL, 0, 'admin', NOW(), 0);

-- 熔断器监控按钮 (parent_id=106)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(117, '查询配置', 'SearchCircuitBreaker', 3, NULL, NULL, 1, 0, 106, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(118, '查看详情', 'ViewCircuitBreakerDetail', 3, NULL, NULL, 2, 0, 106, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 4. 角色菜单关联
-- 超级管理员(role_id=1)不需要关联数据，已特殊处理
-- 为网关管理员(role_id=2)和网关运维(role_id=3)添加监控中心菜单
-- ============================================

INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id >= 100 AND menu_id <= 118 AND menu_id NOT IN (102, 113, 114, 115, 116) AND delFlag = 0;

INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id >= 100 AND menu_id <= 118 AND menu_id NOT IN (102, 113, 114, 115, 116) AND delFlag = 0;

SELECT '监控中心菜单初始化完成!' AS message;