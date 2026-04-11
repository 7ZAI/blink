-- 网关路由配置表
-- 用于存储动态路由配置，支持 Redis 和 Nacos 两种存储方式
-- @author binblink
-- @since 2026-04-11

CREATE TABLE IF NOT EXISTS `ga_route` (
    `route_id` VARCHAR(64) NOT NULL COMMENT '路由ID（主键，业务标识）',
    `route_name` VARCHAR(128) DEFAULT NULL COMMENT '路由名称',
    `uri` VARCHAR(256) NOT NULL COMMENT '目标URI（如 lb://service-name 或 https://example.com）',
    `predicates` JSON DEFAULT NULL COMMENT '断言配置JSON数组',
    `filters` JSON DEFAULT NULL COMMENT '过滤器配置JSON数组',
    `order_num` INT DEFAULT 0 COMMENT '路由顺序（数值越小优先级越高）',
    `metadata` JSON DEFAULT NULL COMMENT '元数据JSON对象',
    `routes_group` VARCHAR(64) DEFAULT 'default' COMMENT '路由分组（用于 Redis 模式）',
    `storage_mode` VARCHAR(16) DEFAULT 'redis' COMMENT '存储方式：redis/nacos',
    `nacos_data_id` VARCHAR(128) DEFAULT NULL COMMENT 'Nacos Data ID（用于 Nacos 模式）',
    `nacos_group` VARCHAR(64) DEFAULT 'DEFAULT_GROUP' COMMENT 'Nacos Group（用于 Nacos 模式）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`route_id`),
    INDEX `idx_routes_group` (`routes_group`),
    INDEX `idx_storage_mode` (`storage_mode`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由配置表';

-- 网关路由历史审计表
-- 用于记录路由配置的变更历史，支持回滚和审计
-- @author binblink
-- @since 2026-04-11

CREATE TABLE IF NOT EXISTS `ga_route_history` (
    `history_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `route_id` VARCHAR(64) NOT NULL COMMENT '路由ID',
    `route_name` VARCHAR(128) DEFAULT NULL COMMENT '路由名称（变更时的值）',
    `operation_type` VARCHAR(8) NOT NULL COMMENT '操作类型：A新增/M修改/D删除',
    `before_data` JSON DEFAULT NULL COMMENT '变更前数据快照（修改/删除时记录）',
    `after_data` JSON DEFAULT NULL COMMENT '变更后数据快照（新增/修改时记录）',
    `operator_id` INT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    PRIMARY KEY (`history_id`),
    INDEX `idx_route_id` (`route_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_operate_time` (`operate_time`),
    INDEX `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由历史审计表';