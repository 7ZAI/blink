-- =====================================================
-- 通知系统数据库初始化脚本
-- @author binblink
-- @since 2026-04-28
-- =====================================================

-- 1. 通知渠道配置表
CREATE TABLE IF NOT EXISTS `sys_notification_channel_config` (
    `config_id` INT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `channel_type` VARCHAR(32) NOT NULL COMMENT '渠道类型',
    `channel_name` VARCHAR(64) NOT NULL COMMENT '渠道名称',
    `config_json` TEXT NOT NULL COMMENT '配置JSON',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `remark` VARCHAR(256) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_channel_type` (`channel_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道配置表';

-- 2. 通知发送失败记录表
CREATE TABLE IF NOT EXISTS `sys_notification_failure_log` (
    `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `channel_type` VARCHAR(32) NOT NULL COMMENT '渠道类型',
    `notification_type` VARCHAR(32) COMMENT '通知类型',
    `business_id` VARCHAR(64) COMMENT '业务ID',
    `title` VARCHAR(256) COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `recipients` VARCHAR(1024) COMMENT '接收人（JSON数组）',
    `error_code` VARCHAR(64) COMMENT '错误码',
    `error_message` TEXT COMMENT '错误信息',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待重试，1-已成功，2-已放弃',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_channel_type` (`channel_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送失败记录表';

-- 3. 插入默认渠道配置（示例）
-- 邮件配置示例（需根据实际情况修改）
INSERT INTO `sys_notification_channel_config`
(`channel_type`, `channel_name`, `config_json`, `enabled`, `remark`)
VALUES
('email', '邮件通知', '{"host":"smtp.example.com","port":465,"username":"noreply@example.com","password":"your_password","sslEnabled":true,"fromAddress":"noreply@example.com","fromName":"Blink Gateway"}', 0, '邮件通知渠道，需配置SMTP服务器'),
('webhook', 'Webhook通知', '{"url":"https://webhook.example.com/notify","method":"POST","timeout":5000,"retryTimes":3}', 0, 'Webhook通知渠道，需配置接收URL');

-- 4. 站内通知渠道始终启用，无需配置
