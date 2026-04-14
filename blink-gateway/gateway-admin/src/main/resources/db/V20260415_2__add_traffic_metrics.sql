-- 添加响应时间分布字段
ALTER TABLE gateway_traffic_history
ADD COLUMN p50_response_time BIGINT DEFAULT 0 COMMENT 'P50响应时间(ms)',
ADD COLUMN p95_response_time BIGINT DEFAULT 0 COMMENT 'P95响应时间(ms)',
ADD COLUMN p99_response_time BIGINT DEFAULT 0 COMMENT 'P99响应时间(ms)',
ADD COLUMN max_response_time BIGINT DEFAULT 0 COMMENT '最大响应时间(ms)';

-- 添加错误分类字段
ALTER TABLE gateway_traffic_history
ADD COLUMN error_4xx_count BIGINT DEFAULT 0 COMMENT '4xx错误数',
ADD COLUMN error_5xx_count BIGINT DEFAULT 0 COMMENT '5xx错误数',
ADD COLUMN error_rate DOUBLE DEFAULT 0 COMMENT '错误率(%)';

-- 添加实时指标字段
ALTER TABLE gateway_traffic_history
ADD COLUMN current_qps INT DEFAULT 0 COMMENT '实时QPS';