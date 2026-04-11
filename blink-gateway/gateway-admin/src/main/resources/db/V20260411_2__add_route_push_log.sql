-- 路由推送记录表
-- 用于记录路由推送历史，支持回滚和审计
-- @author binblink
-- @since 2026-04-11

CREATE TABLE IF NOT EXISTS ga_route_push_log (
    push_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '推送记录ID',
    storage_mode VARCHAR(16) NOT NULL COMMENT '存储方式: redis/nacos',
    routes_group VARCHAR(64) DEFAULT NULL COMMENT '路由分组（Redis模式）',
    nacos_data_id VARCHAR(128) DEFAULT NULL COMMENT 'Nacos Data ID（Nacos模式）',
    nacos_group VARCHAR(64) DEFAULT 'DEFAULT_GROUP' COMMENT 'Nacos Group（Nacos模式）',
    route_ids TEXT NOT NULL COMMENT '推送的路由ID列表(JSON数组)',
    route_snapshot JSON DEFAULT NULL COMMENT '路由配置快照(JSON数组)',
    push_mode VARCHAR(16) NOT NULL COMMENT '推送模式: broadcast/specified',
    target_instance_ids TEXT DEFAULT NULL COMMENT '目标实例ID列表(JSON数组)',
    instance_count INT DEFAULT 0 COMMENT '目标实例数量',
    success_count INT DEFAULT 0 COMMENT '成功推送实例数量',
    push_result TINYINT DEFAULT 0 COMMENT '推送结果: 0-成功, 1-部分失败, 2-失败',
    push_detail JSON DEFAULT NULL COMMENT '各实例推送详情(JSON对象)',
    operator_id INT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    push_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    PRIMARY KEY (push_id),
    INDEX idx_routes_group (routes_group),
    INDEX idx_storage_mode (storage_mode),
    INDEX idx_push_time (push_time),
    INDEX idx_operator_id (operator_id),
    INDEX idx_push_result (push_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路由推送记录表';

-- 实例路由菜单（在网关管理目录 menu_id=49 下）
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(57, '实例路由', 'InstanceRoute', 2, 'Connection', '/instanceRoute', 4, 0, 49, 2, 'views/instanceRoute/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 实例路由按钮权限
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(58, '推送路由', 'PushRoute', 3, NULL, NULL, 1, 0, 57, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(59, '查看历史', 'ViewHistory', 3, NULL, NULL, 2, 0, 57, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(60, '回滚推送', 'RollbackPush', 3, NULL, NULL, 3, 0, 57, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- 超级管理员角色菜单关联
INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (57, 58, 59, 60) AND delFlag = 0;