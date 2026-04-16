-- 路由实例关联表
-- 用于记录每个路由在每个实例上的推送状态，实现实例级粒度的状态跟踪
-- 解决问题 P0-1.2 推送状态粒度不足

CREATE TABLE IF NOT EXISTS ga_route_instance_rela (
    rela_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    route_id VARCHAR(64) NOT NULL COMMENT '路由ID',
    instance_id VARCHAR(128) NOT NULL COMMENT '实例ID，格式：gateway-app:host:port',
    push_id BIGINT COMMENT '推送记录ID，关联 ga_route_push_log.push_id',
    push_status TINYINT DEFAULT 0 COMMENT '推送状态: 0-未推送 1-已推送 2-推送失败',
    push_time DATETIME COMMENT '推送时间',
    load_status TINYINT DEFAULT 0 COMMENT '加载状态: 0-未知 1-已加载 2-加载失败',
    load_time DATETIME COMMENT '加载确认时间',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_route_instance (route_id, instance_id),
    KEY idx_instance_id (instance_id),
    KEY idx_push_id (push_id),
    KEY idx_push_status (push_status),
    KEY idx_push_time (push_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由实例关联表';
