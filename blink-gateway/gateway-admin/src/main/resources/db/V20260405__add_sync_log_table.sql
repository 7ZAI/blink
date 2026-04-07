-- ============================================
-- 1. 同步日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(32) NOT NULL COMMENT '同步类型: channel/route/config',
    sync_mode TINYINT DEFAULT 0 COMMENT '同步模式: 0-全量, 1-增量/单项',
    sync_keys TEXT COMMENT '同步的key列表(JSON数组)',
    operator VARCHAR(64) COMMENT '操作人',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-部分失败, 2-失败',
    instance_count INT COMMENT '同步实例数量',
    success_count INT COMMENT '成功实例数量',
    detail TEXT COMMENT '详细结果(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sync_type (sync_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步日志表';

-- ============================================
-- 2. 数据同步菜单 (在网关管理目录 menu_id=49 下)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(50, '数据同步', 'DataSync', 2, 'Refresh', '/dataSync', 5, 0, 49, 2, 'views/dataSync/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 数据同步按钮权限
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(51, '执行同步', 'SyncData', 3, NULL, NULL, 1, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(52, '一致性检查', 'CheckConsistency', 3, NULL, NULL, 2, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 角色菜单关联 (超级管理员拥有所有菜单)
-- ============================================
INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (50, 51, 52) AND delFlag = 0;