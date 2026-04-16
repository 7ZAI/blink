-- 任务定义表
CREATE TABLE IF NOT EXISTS `sys_job` (
    `job_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name` VARCHAR(64) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '任务分组',
    `job_description` VARCHAR(256) DEFAULT NULL COMMENT '任务描述',
    `cron_expression` VARCHAR(64) NOT NULL COMMENT 'Cron表达式',
    `job_status` TINYINT NOT NULL DEFAULT 1 COMMENT '任务状态: 0-暂停, 1-正常',
    `job_type` TINYINT NOT NULL DEFAULT 1 COMMENT '任务类型: 1-注解方法, 2-接口实现',
    `target_bean` VARCHAR(128) NOT NULL COMMENT '执行目标Bean名称',
    `target_method` VARCHAR(64) DEFAULT NULL COMMENT '执行目标方法名',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `timeout` BIGINT DEFAULT -1 COMMENT '超时时间(毫秒)',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `retry_interval` BIGINT DEFAULT 1000 COMMENT '重试间隔(毫秒)',
    `parameters` TEXT DEFAULT NULL COMMENT '任务参数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`job_id`),
    UNIQUE KEY `uk_job_name_group` (`job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务定义表';

-- 任务执行日志表
CREATE TABLE IF NOT EXISTS `sys_job_log` (
    `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `job_id` BIGINT NOT NULL COMMENT '任务ID',
    `job_name` VARCHAR(64) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(64) NOT NULL COMMENT '任务分组',
    `trigger_time` DATETIME NOT NULL COMMENT '触发时间',
    `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `duration` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态: 0-执行中, 1-成功, 2-失败',
    `execute_count` INT DEFAULT 0 COMMENT '执行次数(重试计数)',
    `result_message` TEXT DEFAULT NULL COMMENT '执行结果消息',
    `error_message` TEXT DEFAULT NULL COMMENT '异常信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_trigger_time` (`trigger_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';
