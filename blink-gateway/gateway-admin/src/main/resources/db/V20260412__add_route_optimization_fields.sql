-- 路由管理功能优化字段
-- 增加乐观锁版本号、推送状态、失败详情等字段
-- @author binblink
-- @since 2026-04-12

-- ==================== ga_route 表优化 ====================

-- 增加乐观锁版本号
ALTER TABLE ga_route ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- 增加最后推送时间
ALTER TABLE ga_route ADD COLUMN IF NOT EXISTS last_push_time DATETIME DEFAULT NULL COMMENT '最后推送时间';

-- 增加推送状态：0-未推送 1-已推送 2-推送失败
ALTER TABLE ga_route ADD COLUMN IF NOT EXISTS push_status TINYINT DEFAULT 0 COMMENT '推送状态：0-未推送 1-已推送 2-推送失败';

-- 增加复合索引（分组+状态）
CREATE INDEX IF NOT EXISTS idx_route_group_status ON ga_route(routes_group, status);

-- 增加推送状态索引
CREATE INDEX IF NOT EXISTS idx_push_status ON ga_route(push_status);

-- ==================== ga_route_push_log 表优化 ====================

-- 增加失败实例ID列表
ALTER TABLE ga_route_push_log ADD COLUMN IF NOT EXISTS failed_instance_ids TEXT DEFAULT NULL COMMENT '失败实例ID列表(JSON数组)';

-- 增加各实例错误信息
ALTER TABLE ga_route_push_log ADD COLUMN IF NOT EXISTS instance_errors JSON DEFAULT NULL COMMENT '各实例错误信息(JSON对象: instanceId -> errorMsg)';

-- 增加确认状态：0-待确认 1-已确认 2-超时
ALTER TABLE ga_route_push_log ADD COLUMN IF NOT EXISTS confirm_status TINYINT DEFAULT 0 COMMENT '确认状态：0-待确认 1-已确认 2-超时';

-- ==================== ga_route_history 表优化 ====================

-- 增加变更字段列表（用于差异对比）
ALTER TABLE ga_route_history ADD COLUMN IF NOT EXISTS changed_fields TEXT DEFAULT NULL COMMENT '变更字段列表(JSON数组)';

-- ==================== 新增路由管理菜单权限 ====================

-- 批量状态更新按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 61, '批量状态', 'BatchStatus', 3, NULL, NULL, 5, 0, 56, 3, NULL, NULL, 0, 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 61);

-- 全量推送按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 62, '全量推送', 'FullPush', 3, NULL, NULL, 6, 0, 56, 3, NULL, NULL, 0, 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 62);

-- 路由导入按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 63, '导入路由', 'ImportRoutes', 3, NULL, NULL, 7, 0, 56, 3, NULL, NULL, 0, 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 63);

-- 路由导出按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 64, '导出路由', 'ExportRoutes', 3, NULL, NULL, 8, 0, 56, 3, NULL, NULL, 0, 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 64);

-- 路由克隆按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag)
SELECT 65, '克隆路由', 'CloneRoute', 3, NULL, NULL, 9, 0, 56, 3, NULL, NULL, 0, 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 65);