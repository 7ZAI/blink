-- 添加唯一索引防止同一实例同一时间重复记录
-- 注意：如果已存在重复数据，需要先清理后再执行

-- 添加唯一索引（instance_id + collect_time）
ALTER TABLE `gateway_metrics_history`
ADD UNIQUE KEY `uk_instance_collect_time` (`instance_id`, `collect_time`);