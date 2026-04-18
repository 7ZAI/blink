-- 网关实例分组表
-- 用于管理网关实例的分组信息，支持路由按分组隔离
-- @author binblink
-- @since 2026-04-18

SET NAMES utf8mb4;

-- 创建实例分组表
CREATE TABLE IF NOT EXISTS `gateway_instance_group` (
    `group_id` INT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `group_key` VARCHAR(64) NOT NULL COMMENT '分组标识（业务唯一键）',
    `group_name` VARCHAR(128) NOT NULL COMMENT '分组名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`group_id`),
    UNIQUE KEY `uk_group_key` (`group_key`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关实例分组表';

-- 实例表新增分组字段
ALTER TABLE `gateway_instance`
ADD COLUMN `group_key` VARCHAR(64) DEFAULT 'default' COMMENT '分组标识' AFTER `instance_id`,
ADD COLUMN `storage_mode` VARCHAR(16) DEFAULT 'redis' COMMENT '存储方式：redis/nacos' AFTER `group_key`,
ADD INDEX `idx_group_key` (`group_key`);

-- 初始化默认分组
INSERT INTO `gateway_instance_group` (`group_key`, `group_name`, `status`, `remark`)
VALUES ('default', '默认分组', 1, '系统默认分组');

SELECT '实例分组表创建完成' AS message;
