-- 网关流量历史记录表
-- 用于存储聚合后的流量趋势数据（分钟级、小时级）

CREATE TABLE `gateway_traffic_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `time_bucket` datetime NOT NULL COMMENT '时间桶（分钟/小时级别）',
  `granularity` varchar(10) NOT NULL COMMENT '粒度：MINUTE/HOUR',
  `request_count` bigint NOT NULL DEFAULT 0 COMMENT '请求增量',
  `success_count` bigint NOT NULL DEFAULT 0 COMMENT '成功请求增量',
  `failed_count` bigint NOT NULL DEFAULT 0 COMMENT '失败请求增量',
  `avg_response_time` bigint NOT NULL DEFAULT 0 COMMENT '平均响应时间(ms)',
  `peak_qps` int NOT NULL DEFAULT 0 COMMENT '峰值QPS（秒级最大值）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_time_granularity` (`time_bucket`, `granularity`),
  KEY `idx_time_bucket` (`time_bucket`),
  KEY `idx_granularity` (`granularity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关流量历史记录';