-- 路由推送记录表增强字段
-- 解决问题 P1-2.1 推送无闭环确认、P1-2.2 推送失败信息不完整
-- @author binblink
-- @since 2026-04-16

-- 添加失败实例ID列表字段
ALTER TABLE ga_route_push_log
ADD COLUMN failed_instance_ids JSON DEFAULT NULL COMMENT '失败实例ID列表(JSON数组)' AFTER remark;

-- 添加各实例错误信息字段
ALTER TABLE ga_route_push_log
ADD COLUMN instance_errors JSON DEFAULT NULL COMMENT '各实例错误信息(JSON对象, key: instanceId, value: errorMsg)' AFTER failed_instance_ids;

-- 添加确认状态字段
ALTER TABLE ga_route_push_log
ADD COLUMN confirm_status TINYINT DEFAULT 0 COMMENT '确认状态: 0-待确认, 1-已确认, 2-超时' AFTER instance_errors;

-- 添加确认时间字段
ALTER TABLE ga_route_push_log
ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT '确认时间' AFTER confirm_status;

-- 添加确认人字段
ALTER TABLE ga_route_push_log
ADD COLUMN confirm_by VARCHAR(64) DEFAULT NULL COMMENT '确认人' AFTER confirm_time;

-- 添加索引
CREATE INDEX idx_confirm_status ON ga_route_push_log(confirm_status);
