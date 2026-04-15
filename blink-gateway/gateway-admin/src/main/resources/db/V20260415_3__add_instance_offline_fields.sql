-- 实例管理优化：添加下线原因和下线类型字段
-- 支持审计日志记录和下线类型区分

SET NAMES utf8mb4;

-- 添加下线原因和下线类型字段
ALTER TABLE gateway_instance
ADD COLUMN offline_reason VARCHAR(500) COMMENT '下线原因' AFTER offline_time,
ADD COLUMN offline_type VARCHAR(20) DEFAULT 'MANUAL' COMMENT '下线类型: MANUAL-主动/FAULT-被动/DRAINING-排空中' AFTER offline_reason;

-- 添加状态索引优化查询性能
ALTER TABLE gateway_instance ADD INDEX idx_status (status);

SELECT '实例管理优化字段添加完成' AS message;