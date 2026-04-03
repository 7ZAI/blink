-- 网关指标历史记录表
-- 用于存储 gateway-reactive 实例的监控指标历史数据

CREATE TABLE `gateway_metrics_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` varchar(64) NOT NULL COMMENT '实例ID',
  `host` varchar(64) NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `cpu_usage` decimal(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_used` bigint DEFAULT NULL COMMENT '已用内存(bytes)',
  `memory_max` bigint DEFAULT NULL COMMENT '最大内存(bytes)',
  `total_requests` bigint DEFAULT 0 COMMENT '请求总数',
  `success_requests` bigint DEFAULT 0 COMMENT '成功请求数',
  `failed_requests` bigint DEFAULT 0 COMMENT '失败请求数',
  `avg_response_time` bigint DEFAULT 0 COMMENT '平均响应时间(ms)',
  `health_status` varchar(16) DEFAULT NULL COMMENT '健康状态',
  `circuit_breaker_state` varchar(16) DEFAULT NULL COMMENT '熔断器状态',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_time` (`instance_id`, `collect_time`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关指标历史记录';