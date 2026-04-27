-- ============================================
-- Gateway Admin 初始化脚本
-- 用于部署 gateway-admin 服务
-- 包含表结构和必要的初始化数据
-- 
-- 执行方式：
--   mysql -uroot -p123456 < gateway_admin_init.sql
-- 
-- @author binblink
-- @since 2026-04-26
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS `gateway` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `gateway`;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ga_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_channel` (
  `channel_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道ID',
  `channel_name` varchar(32) DEFAULT NULL COMMENT '渠道名',
  `app_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用key值',
  `app_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用秘钥',
  `rela_user_id` varchar(64) DEFAULT NULL COMMENT '关联用户',
  `access_token` varchar(64) DEFAULT NULL COMMENT '认证token',
  `system_publickey` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '系统公钥',
  `system_privatekey` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '系统私钥',
  `channel_publickey` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '渠道公钥',
  `channel_privatekey` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '渠道私钥',
  `enable` tinyint DEFAULT '0' COMMENT '渠道开关 0 开启 1关闭',
  `encryption_switch` tinyint DEFAULT '1' COMMENT '加密开关 0 开启 1关闭',
  `token_type` tinyint DEFAULT '1' COMMENT '认证方式 0 带状态的token 1 jwt -1用户名密码',
  `authority_switch` tinyint DEFAULT '0' COMMENT '权限校验开关 0 开启 1关闭',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`channel_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=COMPACT COMMENT='对接渠道';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_config_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_config_history` (
  `history_id` int NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `operation_type` tinyint NOT NULL COMMENT '操作类型：0-新增 1-修改 2-删除',
  `before_value` text COMMENT '变更前的值',
  `after_value` text COMMENT '变更后的值',
  `operator_id` int DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人名称',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`history_id`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置变更历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_config_push_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_config_push_log` (
  `push_id` int NOT NULL AUTO_INCREMENT COMMENT '推送ID',
  `push_mode` varchar(20) NOT NULL COMMENT '推送模式：broadcast-广播 specified-指定',
  `target_instance_ids` text COMMENT '目标实例ID列表(JSON数组)',
  `instance_count` int NOT NULL DEFAULT '0' COMMENT '目标实例数量',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功推送数量',
  `push_result` tinyint NOT NULL COMMENT '推送结果：0-全部成功 1-部分成功 2-全部失败',
  `push_detail` text COMMENT '推送详情(JSON格式，记录每个实例的推送结果)',
  `config_snapshot` text COMMENT '推送时的配置快照(JSON格式)',
  `operator_id` int DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人名称',
  `push_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`push_id`),
  KEY `idx_push_time` (`push_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置推送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_config_repository`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_config_repository` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_value` text NOT NULL COMMENT '配置值',
  `config_type` tinyint NOT NULL DEFAULT '0' COMMENT '配置类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组',
  `config_group` varchar(50) NOT NULL COMMENT '配置分组：security-安全 ip-IP过滤 route-路由 system-系统',
  `description` varchar(500) DEFAULT NULL COMMENT '配置说明',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-正常 1-禁用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0-正常 1-删除',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关配置仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_instance_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_instance_config` (
  `instance_id` varchar(100) NOT NULL COMMENT '实例ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `config_version` int NOT NULL DEFAULT '1' COMMENT '配置版本号',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`instance_id`,`config_key`),
  KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实例配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_route` (
  `route_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由ID（主键，业务标识）',
  `route_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由名称',
  `uri` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标URI（如 lb://service-name 或 https://example.com）',
  `predicates` json DEFAULT NULL COMMENT '断言配置JSON数组',
  `filters` json DEFAULT NULL COMMENT '过滤器配置JSON数组',
  `order_num` int DEFAULT '0' COMMENT '路由顺序（数值越小优先级越高）',
  `metadata` json DEFAULT NULL COMMENT '元数据JSON对象',
  `routes_group` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '路由分组（用于 Redis 模式）',
  `storage_mode` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT 'redis' COMMENT '存储方式：redis/nacos',
  `nacos_data_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Nacos Data ID（用于 Nacos 模式）',
  `nacos_group` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'DEFAULT_GROUP' COMMENT 'Nacos Group（用于 Nacos 模式）',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `last_push_time` datetime DEFAULT NULL COMMENT '最后推送时间',
  `push_status` tinyint DEFAULT '0' COMMENT '推送状态：0-未推送 1-已推送 2-推送失败',
  PRIMARY KEY (`route_id`),
  KEY `idx_routes_group` (`routes_group`),
  KEY `idx_storage_mode` (`storage_mode`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`),
  KEY `idx_route_group_status` (`routes_group`,`status`),
  KEY `idx_push_status` (`push_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_route_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_route_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
  `route_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '路由ID',
  `route_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由名称（变更时的值）',
  `operation_type` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型：A新增/M修改/D删除',
  `before_data` json DEFAULT NULL COMMENT '变更前数据快照（修改/删除时记录）',
  `after_data` json DEFAULT NULL COMMENT '变更后数据快照（新增/修改时记录）',
  `operator_id` int DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人名称',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `changed_fields` text COLLATE utf8mb4_unicode_ci COMMENT '变更字段列表(JSON数组)',
  PRIMARY KEY (`history_id`),
  KEY `idx_route_id` (`route_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2048251871788900354 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由历史审计表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_route_instance_rela`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_route_instance_rela` (
  `rela_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `route_id` varchar(64) NOT NULL COMMENT '路由ID',
  `instance_id` varchar(128) NOT NULL COMMENT '实例ID',
  `push_id` bigint DEFAULT NULL COMMENT '推送记录ID',
  `push_status` tinyint DEFAULT '0' COMMENT '推送状态: 0-未推送 1-已推送 2-推送失败',
  `push_time` datetime DEFAULT NULL COMMENT '推送时间',
  `load_status` tinyint DEFAULT '0' COMMENT '加载状态: 0-未知 1-已加载 2-加载失败',
  `load_time` datetime DEFAULT NULL COMMENT '加载确认时间',
  `error_msg` text COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rela_id`),
  UNIQUE KEY `uk_route_instance` (`route_id`,`instance_id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_push_id` (`push_id`),
  KEY `idx_push_status` (`push_status`),
  KEY `idx_push_time` (`push_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2044802431816056835 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路由实例关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ga_route_push_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ga_route_push_log` (
  `push_id` bigint NOT NULL AUTO_INCREMENT COMMENT '推送记录ID',
  `storage_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储方式: redis/nacos',
  `routes_group` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由分组（Redis模式）',
  `nacos_data_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Nacos Data ID（Nacos模式）',
  `nacos_group` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'DEFAULT_GROUP' COMMENT 'Nacos Group（Nacos模式）',
  `route_ids` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推送的路由ID列表(JSON数组)',
  `route_snapshot` json DEFAULT NULL COMMENT '路由配置快照(JSON数组)',
  `push_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '推送模式: broadcast/specified',
  `target_instance_ids` text COLLATE utf8mb4_unicode_ci COMMENT '目标实例ID列表(JSON数组)',
  `instance_count` int DEFAULT '0' COMMENT '目标实例数量',
  `success_count` int DEFAULT '0' COMMENT '成功推送实例数量',
  `push_result` tinyint DEFAULT '0' COMMENT '推送结果: 0-成功, 1-部分失败, 2-失败',
  `push_detail` json DEFAULT NULL COMMENT '各实例推送详情(JSON对象)',
  `operator_id` int DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人名称',
  `push_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `failed_instance_ids` text COLLATE utf8mb4_unicode_ci COMMENT '失败实例ID列表(JSON数组)',
  `instance_errors` json DEFAULT NULL COMMENT '各实例错误信息(JSON对象: instanceId -> errorMsg)',
  `confirm_status` tinyint DEFAULT '0' COMMENT '确认状态：0-待确认 1-已确认 2-超时',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirm_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '确认人',
  PRIMARY KEY (`push_id`),
  KEY `idx_routes_group` (`routes_group`),
  KEY `idx_storage_mode` (`storage_mode`),
  KEY `idx_push_time` (`push_time`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_push_result` (`push_result`)
) ENGINE=InnoDB AUTO_INCREMENT=2044802431732170755 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路由推送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_alert_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_alert_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `instance_id` varchar(200) DEFAULT NULL COMMENT '关联实例ID',
  `alert_title` varchar(200) NOT NULL COMMENT '告警标题',
  `alert_content` text COMMENT '告警内容(模板渲染后)',
  `triggered_conditions` text COMMENT '触发的条件详情JSON',
  `severity` varchar(20) NOT NULL COMMENT '严重程度: INFO/WARNING/ERROR',
  `status` varchar(20) NOT NULL DEFAULT 'FIRING' COMMENT '状态: FIRING/RESOLVED/ACKNOWLEDGED',
  `fired_time` datetime NOT NULL COMMENT '触发时间',
  `resolved_time` datetime DEFAULT NULL COMMENT '恢复时间',
  `acknowledged_time` datetime DEFAULT NULL COMMENT '确认时间',
  `acknowledged_by` int DEFAULT NULL COMMENT '确认人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_fired_time` (`fired_time`),
  KEY `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关告警历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_alert_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_alert_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型: RESOURCE/PERFORMANCE/ERROR/INSTANCE',
  `conditions` text NOT NULL COMMENT '触发条件JSON: [{"metricName":"p99","operator":"gt","threshold":1000,"durationMinutes":3}]',
  `severity` varchar(20) NOT NULL DEFAULT 'WARNING' COMMENT '严重程度: INFO/WARNING/ERROR',
  `notify_channels` varchar(100) DEFAULT 'IN_APP' COMMENT '通知渠道: IN_APP,EMAIL,WEBHOOK',
  `notify_template` varchar(500) DEFAULT NULL COMMENT '通知模板，支持变量: {{rule_name}},{{instance_id}},{{metric_name}},{{value}},{{threshold}}',
  `suppress_minutes` int DEFAULT '5' COMMENT '重复告警间隔(分钟)',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用: 0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关告警规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_instance` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实例ID',
  `group_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'default' COMMENT '分组标识',
  `storage_mode` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT 'redis' COMMENT '存储方式：redis/nacos',
  `service_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '服务ID',
  `host` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主机地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `uri` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'URI',
  `metadata` text COLLATE utf8mb4_unicode_ci COMMENT '元数据',
  `status` tinyint DEFAULT '0' COMMENT '实例状态：0-在线，1-离线，2-下线',
  `online_time` datetime DEFAULT NULL COMMENT '上线时间',
  `offline_time` datetime DEFAULT NULL COMMENT '下线时间',
  `offline_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下线原因',
  `offline_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MANUAL' COMMENT '下线类型: MANUAL-主动/FAULT-被动/DRAINING-排空中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instance_id` (`instance_id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_status` (`status`),
  KEY `idx_group_key` (`group_key`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关实例表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_instance_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_instance_group` (
  `group_id` int NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `group_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组标识（业务唯一键）',
  `group_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `uk_group_key` (`group_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关实例分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_metrics_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_metrics_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` varchar(64) NOT NULL COMMENT '实例ID',
  `host` varchar(64) NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口',
  `cpu_usage` decimal(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_used` bigint DEFAULT NULL COMMENT '已用内存(bytes)',
  `memory_max` bigint DEFAULT NULL COMMENT '最大内存(bytes)',
  `total_requests` bigint DEFAULT '0' COMMENT '请求总数',
  `success_requests` bigint DEFAULT '0' COMMENT '成功请求数',
  `failed_requests` bigint DEFAULT '0' COMMENT '失败请求数',
  `avg_response_time` bigint DEFAULT '0' COMMENT '平均响应时间(ms)',
  `health_status` varchar(16) DEFAULT NULL COMMENT '健康状态',
  `circuit_breaker_state` varchar(16) DEFAULT NULL COMMENT '熔断器状态',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instance_collect_time` (`instance_id`,`collect_time`),
  KEY `idx_instance_time` (`instance_id`,`collect_time`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关指标历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_route_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_route_group` (
  `group_id` int NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `group_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组标识（业务唯一键）',
  `group_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `storage_mode` varchar(16) DEFAULT 'nacos' COMMENT '存储方式：nacos/redis',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `uk_group_key` (`group_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型: channel/route/config',
  `sync_mode` tinyint DEFAULT '0' COMMENT '同步模式: 0-全量, 1-增量/单项',
  `sync_keys` text COMMENT '同步的key列表(JSON数组)',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-成功, 1-部分失败, 2-失败',
  `instance_count` int DEFAULT NULL COMMENT '同步实例数量',
  `success_count` int DEFAULT NULL COMMENT '成功实例数量',
  `detail` text COMMENT '详细结果(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据同步日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `gateway_traffic_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gateway_traffic_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `time_bucket` datetime NOT NULL COMMENT '时间桶（分钟/小时级别）',
  `granularity` varchar(10) NOT NULL COMMENT '粒度：MINUTE/HOUR',
  `request_count` bigint NOT NULL DEFAULT '0' COMMENT '请求增量',
  `success_count` bigint NOT NULL DEFAULT '0' COMMENT '成功请求增量',
  `failed_count` bigint NOT NULL DEFAULT '0' COMMENT '失败请求增量',
  `avg_response_time` bigint NOT NULL DEFAULT '0' COMMENT '平均响应时间(ms)',
  `peak_qps` int NOT NULL DEFAULT '0' COMMENT '峰值QPS（秒级最大值）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `p50_response_time` bigint DEFAULT '0' COMMENT 'P50响应时间(ms)',
  `p95_response_time` bigint DEFAULT '0' COMMENT 'P95响应时间(ms)',
  `p99_response_time` bigint DEFAULT '0' COMMENT 'P99响应时间(ms)',
  `max_response_time` bigint DEFAULT '0' COMMENT '最大响应时间(ms)',
  `error_4xx_count` bigint DEFAULT '0' COMMENT '4xx错误数',
  `error_5xx_count` bigint DEFAULT '0' COMMENT '5xx错误数',
  `error_rate` double DEFAULT '0' COMMENT '错误率(%)',
  `current_qps` int DEFAULT '0' COMMENT '实时QPS',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_time_granularity` (`time_bucket`,`granularity`),
  KEY `idx_time_bucket` (`time_bucket`),
  KEY `idx_granularity` (`granularity`)
) ENGINE=InnoDB AUTO_INCREMENT=2002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关流量历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `mq_msg_rece`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mq_msg_rece` (
  `msg_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息id',
  `receive_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收者标识',
  `buss_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务id',
  `req_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求id',
  `receive_sts` int NOT NULL DEFAULT '0' COMMENT '消息接收状态 ‘0’ 未消费 1 消费成功 2 消费失败',
  `mq_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N' COMMENT '消息类型 N 普通 B 业务 ',
  `mq_mode` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'S' COMMENT '工作模式 S 单消费  M 多消费 ',
  `mq_context` json NOT NULL COMMENT '消息内容',
  `send_sys` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发送者',
  `receive_sys` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '接收者',
  `receive_time` timestamp NULL DEFAULT NULL COMMENT '接收时间',
  `consumer_times` int NOT NULL DEFAULT '0' COMMENT '消费次数',
  `fail_times` int NOT NULL DEFAULT '0' COMMENT '失败次数',
  `remark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`msg_id`,`receive_id`),
  UNIQUE KEY `msg_id` (`msg_id`,`receive_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='消息消费记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `mq_msg_send`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mq_msg_send` (
  `msg_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息id',
  `buss_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务id',
  `req_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求id',
  `send_sts` int NOT NULL DEFAULT '0' COMMENT '消息发送状态 ‘0’未发送 1 发送成功 2 发送失败',
  `mq_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'N' COMMENT '消息类型 N 普通 B 业务 ',
  `mq_mode` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'S' COMMENT '工作模式 S 单消费  M 多消费 ',
  `mq_context` json NOT NULL COMMENT '消息内容',
  `mq_context_class` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息类',
  `mq_exchange` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息交换机',
  `mq_routing_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息路由key',
  `send_sys` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发送者',
  `send_time` timestamp NULL DEFAULT NULL COMMENT '初始发送时间',
  `last_send_time` timestamp NULL DEFAULT NULL COMMENT '最新发送时间',
  `enable_retry` int NOT NULL DEFAULT '0' COMMENT '是否允许重发 0 开启 1关闭 ',
  `retry_times` int NOT NULL DEFAULT '0' COMMENT '发送次数',
  `fail_times` int NOT NULL DEFAULT '0' COMMENT '失败次数',
  `remark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`msg_id`),
  UNIQUE KEY `msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='消息发送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `redis_mq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `redis_mq` (
  `msg_id` varchar(64) DEFAULT NULL,
  `msg_status` varchar(1) DEFAULT NULL,
  `stream_id` varchar(64) DEFAULT NULL,
  `topic` varchar(64) DEFAULT NULL,
  `msg_type` varchar(32) DEFAULT NULL,
  `payload` json NOT NULL,
  `payload_class` varchar(128) DEFAULT NULL,
  `sender` varchar(32) DEFAULT NULL,
  `receiver` varchar(32) DEFAULT NULL,
  `version` varchar(32) DEFAULT NULL,
  `retry_times` int NOT NULL DEFAULT '0',
  `fail_times` int NOT NULL DEFAULT '0',
  `extra` json DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `seq_no`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seq_no` (
  `seq_id` int NOT NULL AUTO_INCREMENT COMMENT '顺序号ID',
  `seq_name` varchar(255) NOT NULL COMMENT '顺序号名称',
  `current_number` bigint NOT NULL DEFAULT '1' COMMENT '当前值',
  `seq_incr` int NOT NULL DEFAULT '1' COMMENT '增量',
  `start_number` bigint NOT NULL DEFAULT '1' COMMENT '起始值',
  `max_number` bigint NOT NULL DEFAULT '999999' COMMENT '最大值',
  `warn_number` int NOT NULL DEFAULT '99999' COMMENT '预警值',
  PRIMARY KEY (`seq_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='顺序号表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sync_type` varchar(32) NOT NULL COMMENT '同步类型: channel/route/config',
  `sync_mode` tinyint DEFAULT '0' COMMENT '同步模式: 0-全量, 1-增量/单项',
  `sync_keys` text COMMENT '同步的key列表(JSON数组)',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-成功, 1-部分失败, 2-失败',
  `instance_count` int DEFAULT NULL COMMENT '同步实例数量',
  `success_count` int DEFAULT NULL COMMENT '成功实例数量',
  `detail` text COMMENT '详细结果(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据同步日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '参数键名',
  `config_name` varchar(100) NOT NULL COMMENT '参数名称',
  `config_value` text NOT NULL COMMENT '参数值',
  `config_type` tinyint NOT NULL DEFAULT '0' COMMENT '参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组',
  `group_id` int NOT NULL DEFAULT '0' COMMENT '参数分组ID',
  `description` varchar(500) DEFAULT NULL COMMENT '参数描述',
  `readonly` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否只读：0-可修改 1-只读',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-禁用  0-启用',
  `create_by` varchar(50) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config_group` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_key` varchar(50) NOT NULL COMMENT '分组键名',
  `group_name` varchar(50) NOT NULL COMMENT '分组名称',
  `parent_id` int NOT NULL DEFAULT '0' COMMENT '父分组ID',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：1-禁用 0-启用',
  `create_by` varchar(50) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数分组表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_data_filter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_data_filter` (
  `data_filter_id` int NOT NULL AUTO_INCREMENT,
  `data_filter_name` varchar(50) NOT NULL,
  `data_filter_en_name` varchar(50) DEFAULT NULL,
  `entity_class` varchar(200) NOT NULL,
  `table_name` varchar(100) NOT NULL,
  `rule_type` varchar(30) NOT NULL,
  `rule_config` text,
  `status` tinyint NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(30) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(30) DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`data_filter_id`),
  KEY `idx_entity_class` (`entity_class`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据过滤规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` int NOT NULL AUTO_INCREMENT COMMENT '字典数据主键id',
  `dict_type` varchar(100) NOT NULL COMMENT '关联字典类型编码',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签（显示值）',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值（实际值）',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（用于前端显示样式）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认：0-否 1-是',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-启用 1-禁用',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `locale` varchar(10) DEFAULT 'zh_cn' COMMENT '语言标识',
  PRIMARY KEY (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` int NOT NULL AUTO_INCREMENT COMMENT '字典主键id',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型编码（唯一标识）',
  `dict_name` varchar(100) NOT NULL COMMENT '字典类型名称',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-启用 1-禁用',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_field_constraint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_field_constraint` (
  `constraint_id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `constraint_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '约束名称（字段名称）',
  `constraint_description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '约束描述',
  `data_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'S' COMMENT '数据类型（C-char N-number D-decimal S-string T-time）',
  `max_length` int DEFAULT NULL COMMENT '最大长度',
  `data_pattern` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据正则校验模式',
  `data_precision` int DEFAULT NULL COMMENT '数据精度（小数位数）',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-启用 1-禁用',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`constraint_id`),
  UNIQUE KEY `uk_constraint_name` (`constraint_name`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段约束表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` int NOT NULL AUTO_INCREMENT COMMENT '菜单id',
  `menu_name` varchar(30) DEFAULT NULL COMMENT '菜单名称',
  `menu_en_name` varchar(30) DEFAULT NULL COMMENT '菜单英文名称',
  `type` tinyint DEFAULT NULL COMMENT '菜单类型',
  `icon` varchar(255) DEFAULT NULL COMMENT '菜单图标',
  `url` varchar(255) DEFAULT NULL COMMENT '菜单地址',
  `order_number` int DEFAULT NULL COMMENT '排序序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0显示 1隐藏',
  `parent_id` int DEFAULT NULL COMMENT '父菜单id',
  `menu_level` int DEFAULT NULL COMMENT '菜单层级',
  `component_path` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `perm_id` int DEFAULT NULL COMMENT '权限ID',
  `hasChildren` tinyint(1) DEFAULT '0' COMMENT '是否有子菜单（按钮不算）',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `delFlag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统菜单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_msg_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_msg_info` (
  `msg_id` int NOT NULL AUTO_INCREMENT COMMENT '数据字典id',
  `msg_code` varchar(16) NOT NULL COMMENT '消息代码',
  `msg_info` varchar(200) DEFAULT NULL COMMENT '消息描述',
  `msg_type` varchar(8) DEFAULT NULL COMMENT '消息类型 错误E 警告W 成功S',
  `msg_lang` varchar(10) NOT NULL DEFAULT 'zh_cn' COMMENT '消息语言',
  PRIMARY KEY (`msg_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='消息码信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `title` varchar(100) NOT NULL COMMENT '消息标题',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `type` varchar(20) NOT NULL COMMENT '消息类型: SYSTEM/OPERATION/ALERT',
  `severity` varchar(20) NOT NULL DEFAULT 'INFO' COMMENT '严重级别: INFO/WARNING/ERROR/SUCCESS',
  `target_type` varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '目标类型: ALL/USER',
  `target_user_id` int DEFAULT NULL COMMENT '目标用户ID，定向推送时使用',
  `source_ref` varchar(100) DEFAULT NULL COMMENT '来源关联ID，如同步任务ID、配置ID',
  `created_by` int DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间，过期后不再展示',
  PRIMARY KEY (`notification_id`),
  KEY `idx_target_user` (`target_user_id`,`created_time`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_type_severity` (`type`,`severity`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_notification_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL COMMENT '消息ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
  PRIMARY KEY (`read_id`),
  UNIQUE KEY `uk_notification_user` (`notification_id`,`user_id`),
  KEY `idx_user_read` (`user_id`,`read_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息读取状态表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` int DEFAULT NULL COMMENT '操作用户ID',
  `login_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录名',
  `log_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPERATION' COMMENT '日志类型: LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `request_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法: POST/GET/PUT/DELETE等',
  `request_params` json DEFAULT NULL COMMENT '请求参数（已脱敏）',
  `response_data` json DEFAULT NULL COMMENT '响应数据（已脱敏，失败时为空）',
  `execute_status` tinyint NOT NULL DEFAULT '0' COMMENT '执行状态: 0成功 1失败',
  `error_msg` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息（失败时记录）',
  `execute_time_ms` int DEFAULT '0' COMMENT '执行时长(毫秒)',
  `ip_address` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器UA',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_name` (`login_name`),
  KEY `idx_execute_status` (`execute_status`),
  KEY `idx_log_type` (`log_type`)
) ENGINE=InnoDB AUTO_INCREMENT=260 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_operation_log_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log_history` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` int DEFAULT NULL COMMENT '操作用户ID',
  `login_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录名',
  `log_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPERATION' COMMENT '日志类型: LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作描述',
  `request_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法: POST/GET/PUT/DELETE等',
  `request_params` json DEFAULT NULL COMMENT '请求参数（已脱敏）',
  `response_data` json DEFAULT NULL COMMENT '响应数据（已脱敏，失败时为空）',
  `execute_status` tinyint NOT NULL DEFAULT '0' COMMENT '执行状态: 0成功 1失败',
  `error_msg` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息（失败时记录）',
  `execute_time_ms` int DEFAULT '0' COMMENT '执行时长(毫秒)',
  `ip_address` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器UA',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `archive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_archive_time` (`archive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志历史归档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `ac_id` int NOT NULL AUTO_INCREMENT COMMENT '权限id',
  `ac_name` varchar(30) DEFAULT NULL COMMENT '权限名称',
  `ac_en_name` varchar(30) DEFAULT NULL COMMENT '权限英文名称',
  `ac_identity` varchar(30) DEFAULT NULL COMMENT '权限标识',
  `ac_type` tinyint DEFAULT NULL COMMENT '权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限',
  `url` varchar(255) DEFAULT NULL COMMENT '权限地址',
  `data_filter_id` int DEFAULT NULL COMMENT '数据过滤器id',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ac_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='权限菜单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` int NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `role_name` varchar(64) DEFAULT NULL COMMENT '角色名称',
  `role_en_name` varchar(64) DEFAULT NULL COMMENT '角色英文名称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '角色状态',
  `role_code` varchar(60) DEFAULT '' COMMENT '角色代码',
  `role_type` tinyint DEFAULT '2' COMMENT '角色类型: 0-系统角色, 2-自定义角色',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `delFlag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统角色';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu_rela`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu_rela` (
  `role_id` int NOT NULL COMMENT '角色id',
  `menu_id` int NOT NULL COMMENT '菜单id',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=COMPACT COMMENT='角色关联菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_perm_rela`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_perm_rela` (
  `role_id` int NOT NULL COMMENT '角色id',
  `ac_id` int NOT NULL COMMENT '权限id',
  PRIMARY KEY (`role_id`,`ac_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='角色权限关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `login_name` varchar(30) DEFAULT NULL COMMENT '登录名',
  `password` varchar(64) DEFAULT NULL COMMENT '密码',
  `username` varchar(30) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `avatar_style` varchar(50) DEFAULT 'fun-emoji' COMMENT '头像样式(DiceBear)',
  `sex` tinyint DEFAULT '3' COMMENT '性别 1男 2女 3不确定',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `last_login_time` timestamp NULL DEFAULT NULL COMMENT '上次登录时间',
  `locked` tinyint DEFAULT '0' COMMENT '锁定状态 0 未锁定 1 管理员锁定 2 输错密码锁定',
  `salt` varchar(64) DEFAULT NULL COMMENT '加密盐值',
  `psw_retry` tinyint DEFAULT '0' COMMENT '密码重试次数',
  `superFlag` tinyint DEFAULT '0' COMMENT '超级管理员标志 0否 1是',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `lock_time` timestamp NULL DEFAULT NULL COMMENT '锁定时间',
  `delFlag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `password_reset` tinyint(1) DEFAULT '1' COMMENT '密码是否需要重置: 0-否, 1-是',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统用户';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_preference` (
  `preference_id` int NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `theme` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'light' COMMENT '主题: light/dark/auto',
  `language` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'zh_cn' COMMENT '语言: zh_cn/en_us',
  `sidebar_collapsed` tinyint(1) DEFAULT '0' COMMENT '侧边栏收起: 0否 1是',
  `font_size` int DEFAULT '14' COMMENT '字体大小',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`preference_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role_rela`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role_rela` (
  `user_id` int NOT NULL COMMENT '用户id',
  `role_id` int NOT NULL COMMENT '角色id',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='用户角色关系表 多对多';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- ============================================
-- 初始化数据部分
-- ============================================

-- ============================================
-- 1. 角色数据
-- ============================================
INSERT INTO sys_role (role_id, role_name, role_en_name, status, role_code, role_type, create_by, create_time, delFlag) VALUES
(1, '超级管理员', 'SuperAdmin', 0, 'superAdmin', 0, 'admin', NOW(), 0),
(2, '网关管理员', 'GatewayAdmin', 0, 'gatewayAdmin', 0, 'admin', NOW(), 0),
(3, '网关运维', 'GatewayOps', 0, 'gatewayOps', 0, 'admin', NOW(), 0),
(4, '渠道管理员', 'ChannelAdmin', 0, 'channelAdmin', 2, 'admin', NOW(), 0);

-- ============================================
-- 2. 超级管理员用户
-- 密码: 123456 (BCrypt加密)
-- ============================================
INSERT INTO sys_user (user_id, login_name, password, username, avatar, avatar_style, sex, phone, email, locked, salt, psw_retry, superFlag, remark, create_by, create_time, delFlag) VALUES
(21, 'admin', '$2a$10$5InOZPGXsu4I9mEIfpYkdesbyLTrfn3YmQwISWqicSZ01OZzgLDkG', '超级管理员', NULL, 'fun-emoji', 1, NULL, NULL, 0, NULL, 0, 1, '系统超级管理员', 'admin', NOW(), 0);

-- ============================================
-- 3. 用户角色关联
-- ============================================
INSERT INTO sys_user_role_rela (user_id, role_id) VALUES (21, 1);

-- ============================================
-- 4. 配置分组
-- ============================================
INSERT INTO sys_config_group (id, group_key, group_name, parent_id, order_num, status, create_by, create_time, remark) VALUES
(1, 'base', '基础设置', 0, 1, 0, 'admin', NOW(), '基础配置'),
(2, 'system', '系统设置', 0, 2, 0, 'admin', NOW(), '系统相关配置'),
(3, 'security', '安全设置', 0, 3, 0, 'admin', NOW(), '安全相关配置'),
(4, 'log', '日志设置', 0, 1, 0, 'admin', NOW(), '日志相关配置'),
(10, 'login', '登录配置', 0, 4, 0, 'admin', NOW(), '登录认证配置'),
(11, 'gateway', '网关设置', 0, 5, 0, 'admin', NOW(), '网关相关配置');


-- ============================================
-- 5. 菜单数据
-- ============================================

-- 一级菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(2, '仪表盘', 'Dashboard', 2, 'HomeFilled', '/dashboard', 1, 0, 0, 1, '/views/dashboard/index.vue', 0, 'admin', NOW(), 0),
(49, '网关管理', 'GatewayAdmin', 1, 'mdi:router-network', '/', 2, 0, 0, 1, NULL, 1, 'admin', NOW(), 0),
(7, '系统管理', 'System', 1, 'Tools', '/system', 6, 0, 0, 1, NULL, 1, 'admin', NOW(), 0),
(100, '监控中心', 'MonitorCenter', 1, 'Monitor', NULL, 4, 0, 0, 1, NULL, 1, 'admin', NOW(), 0);

-- 网关管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(65, '实例管理', 'InstanceManagement', 2, 'Operation', '/instance', 1, 0, 49, 2, 'views/instance/index.vue', 0, 'admin', NOW(), 0),
(3, '渠道管理', 'Channel', 2, 'Connection', '/channel', 3, 0, 49, 2, 'views/channel/index.vue', 0, 'admin', NOW(), 0),
(4, '路由管理', 'RouteManagement', 1, 'Guide', '/route', 2, 0, 49, 2, NULL, 1, 'admin', NOW(), 0),
(5, '网关配置', 'ConfigManagement', 2, 'mdi:table-large', '/config', 4, 0, 49, 2, NULL, 0, 'admin', NOW(), 0),
(50, '数据同步', 'DataSync', 2, 'Refresh', '/dataSync', 5, 0, 49, 2, 'views/dataSync/index.vue', 0, 'admin', NOW(), 0);

-- 路由管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(130, '路由分组', 'RouteGroup', 2, 'Grid', '/route/group', 1, 0, 4, 2, 'views/routeGroup/index.vue', 0, 'admin', NOW(), 0),
(61, '路由仓库', 'RouteRepository', 2, 'Document', '/route/repository', 2, 0, 4, 2, 'views/route/index.vue', 0, 'admin', NOW(), 0),
(67, '路由推送', 'PushRoute', 2, 'Upload', '/route/push', 3, 0, 4, 2, 'views/pushRoute/index.vue', 0, 'admin', NOW(), 0),
(66, '推送历史', 'PushHistory', 2, 'Clock', '/route/push-history', 4, 0, 4, 2, NULL, 0, 'admin', NOW(), 0);

-- 路由分组按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(131, '新增分组', 'AddRouteGroup', 3, NULL, NULL, 1, 0, 130, 3, NULL, 0, 'admin', NOW(), 0),
(132, '编辑分组', 'EditRouteGroup', 3, NULL, NULL, 2, 0, 130, 3, NULL, 0, 'admin', NOW(), 0),
(133, '删除分组', 'DeleteRouteGroup', 3, NULL, NULL, 3, 0, 130, 3, NULL, 0, 'admin', NOW(), 0);

-- 渠道管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(53, '新增渠道', 'AddChannel', 3, NULL, NULL, 0, 0, 3, 2, NULL, 0, 'admin', NOW(), 0);

-- 数据同步按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(51, '执行同步', 'SyncData', 3, NULL, NULL, 1, 0, 50, 3, NULL, 0, 'admin', NOW(), 0),
(52, '一致性检查', 'CheckConsistency', 3, NULL, NULL, 2, 0, 50, 3, NULL, 0, 'admin', NOW(), 0);

-- 系统管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(8, '用户管理', 'SystemUser', 2, 'User', '/system/user', 1, 0, 7, 2, 'views/system/user/index.vue', 0, 'admin', NOW(), 0),
(9, '角色管理', 'SystemRole', 2, 'UserFilled', '/system/role', 2, 0, 7, 2, 'views/system/role/index.vue', 0, 'admin', NOW(), 0),
(10, '菜单管理', 'SystemMenu', 2, 'Menu', '/system/menu', 3, 0, 7, 2, 'views/system/menu/index.vue', 0, 'admin', NOW(), 0),
(11, '权限管理', 'Permission', 1, 'Lock', NULL, 5, 0, 7, 2, NULL, 1, 'admin', NOW(), 0),
(15, '操作日志', 'OperationLog', 2, 'Document', '/system/operation-log', 6, 0, 7, 2, 'views/system/operation-log/index.vue', 0, 'admin', NOW(), 0),
(16, '字典管理', 'Dict', 1, 'Notebook', NULL, 7, 0, 7, 2, NULL, 1, 'admin', NOW(), 0),
(19, '系统配置', 'SystemConfig', 2, 'Tools', '/system/config', 8, 0, 7, 2, 'views/system/config/index.vue', 0, 'admin', NOW(), 0);

-- 权限管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(12, '接口权限', 'ApiPermission', 2, 'Key', '/system/permission/api-permission', 1, 0, 11, 3, 'views/system/permission/index.vue', 0, 'admin', NOW(), 0),
(13, '数据权限', 'DataPermission', 1, 'DataLine', NULL, 2, 0, 11, 3, NULL, 1, 'admin', NOW(), 0);

-- 数据权限子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(48, '数据权限列表', 'DataPermissionList', 2, 'List', '/system/permission/data-permission/list', 1, 0, 13, 4, 'views/system/permission/index.vue', 0, 'admin', NOW(), 0),
(14, '过滤规则', 'DataFilter', 2, 'Filter', '/system/permission/data-permission/rule', 2, 0, 13, 4, 'views/system/data-filter/index.vue', 0, 'admin', NOW(), 0);

-- 字典管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(17, '字典类型', 'DictType', 2, 'Collection', '/system/dict/type', 1, 0, 16, 3, 'views/system/dict-type/index.vue', 0, 'admin', NOW(), 0),
(18, '字典数据', 'DictData', 2, 'List', '/system/dict/data', 2, 0, 16, 3, 'views/system/dict-data/index.vue', 0, 'admin', NOW(), 0);

-- 监控中心子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(101, '仪表盘', 'Dashboard', 2, 'Odometer', '/monitor/dashboard', 1, 0, 100, 2, 'views/monitor/dashboard/index.vue', 0, 'admin', NOW(), 0),
(119, '实例分组', 'InstanceGroup', 2, 'Grid', '/monitor/instance-group', 2, 0, 100, 2, 'views/monitor/instance-group/index.vue', 0, 'admin', NOW(), 0),
(106, '熔断器监控', 'CircuitBreaker', 2, 'Connection', '/monitor/circuit-breaker', 4, 0, 100, 2, 'views/monitor/circuit-breaker/index.vue', 0, 'admin', NOW(), 0),
(103, '告警管理', 'Alert', 1, 'Bell', NULL, 3, 0, 100, 2, NULL, 1, 'admin', NOW(), 0);

-- 告警管理子菜单
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(104, '告警规则', 'AlertRule', 2, 'SetUp', '/monitor/alert-rule', 1, 0, 103, 3, 'views/monitor/alert-rule/index.vue', 0, 'admin', NOW(), 0),
(105, '告警历史', 'AlertHistory', 2, 'List', '/monitor/alert-history', 2, 0, 103, 3, 'views/monitor/alert-history/index.vue', 0, 'admin', NOW(), 0);

-- 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, hasChildren, create_by, create_time, delFlag) VALUES
(20, '新增用户', 'AddUser', 3, NULL, NULL, 1, 0, 8, 3, NULL, 0, 'admin', NOW(), 0),
(21, '编辑用户', 'EditUser', 3, NULL, NULL, 2, 0, 8, 3, NULL, 0, 'admin', NOW(), 0),
(22, '删除用户', 'DeleteUser', 3, NULL, NULL, 3, 0, 8, 3, NULL, 0, 'admin', NOW(), 0),
(23, '重置密码', 'ResetPassword', 3, NULL, NULL, 4, 0, 8, 3, NULL, 0, 'admin', NOW(), 0),
(24, '锁定用户', 'LockUser', 3, NULL, NULL, 5, 0, 8, 3, NULL, 0, 'admin', NOW(), 0),
(25, '新增角色', 'AddRole', 3, NULL, NULL, 1, 0, 9, 3, NULL, 0, 'admin', NOW(), 0),
(26, '编辑角色', 'EditRole', 3, NULL, NULL, 2, 0, 9, 3, NULL, 0, 'admin', NOW(), 0),
(27, '删除角色', 'DeleteRole', 3, NULL, NULL, 3, 0, 9, 3, NULL, 0, 'admin', NOW(), 0),
(28, '分配权限', 'AssignPermission', 3, NULL, NULL, 4, 0, 9, 3, NULL, 0, 'admin', NOW(), 0),
(29, '分配菜单', 'AssignMenu', 3, NULL, NULL, 5, 0, 9, 3, NULL, 0, 'admin', NOW(), 0),
(30, '新增菜单', 'AddMenu', 3, NULL, NULL, 1, 0, 10, 3, NULL, 0, 'admin', NOW(), 0),
(31, '编辑菜单', 'EditMenu', 3, NULL, NULL, 2, 0, 10, 3, NULL, 0, 'admin', NOW(), 0),
(32, '删除菜单', 'DeleteMenu', 3, NULL, NULL, 3, 0, 10, 3, NULL, 0, 'admin', NOW(), 0),
(33, '新增权限', 'AddPermission', 3, NULL, NULL, 1, 0, 12, 4, NULL, 0, 'admin', NOW(), 0),
(34, '编辑权限', 'EditPermission', 3, NULL, NULL, 2, 0, 12, 4, NULL, 0, 'admin', NOW(), 0),
(35, '删除权限', 'DeletePermission', 3, NULL, NULL, 3, 0, 12, 4, NULL, 0, 'admin', NOW(), 0),
(36, '新增规则', 'AddDataFilter', 3, NULL, NULL, 1, 0, 14, 5, NULL, 0, 'admin', NOW(), 0),
(37, '编辑规则', 'EditDataFilter', 3, NULL, NULL, 2, 0, 14, 5, NULL, 0, 'admin', NOW(), 0),
(38, '删除规则', 'DeleteDataFilter', 3, NULL, NULL, 3, 0, 14, 5, NULL, 0, 'admin', NOW(), 0),
(39, '新增字典类型', 'AddDictType', 3, NULL, NULL, 1, 0, 17, 4, NULL, 0, 'admin', NOW(), 0),
(40, '编辑字典类型', 'EditDictType', 3, NULL, NULL, 2, 0, 17, 4, NULL, 0, 'admin', NOW(), 0),
(41, '删除字典类型', 'DeleteDictType', 3, NULL, NULL, 3, 0, 17, 4, NULL, 0, 'admin', NOW(), 0),
(42, '新增字典数据', 'AddDictData', 3, NULL, NULL, 1, 0, 18, 4, NULL, 0, 'admin', NOW(), 0),
(43, '编辑字典数据', 'EditDictData', 3, NULL, NULL, 2, 0, 18, 4, NULL, 0, 'admin', NOW(), 0),
(44, '删除字典数据', 'DeleteDictData', 3, NULL, NULL, 3, 0, 18, 4, NULL, 0, 'admin', NOW(), 0),
(45, '查询日志', 'SearchLog', 3, NULL, NULL, 1, 0, 15, 3, NULL, 0, 'admin', NOW(), 0),
(46, '查看详情', 'ViewLogDetail', 3, NULL, NULL, 2, 0, 15, 3, NULL, 0, 'admin', NOW(), 0),
(47, '保存配置', 'SaveConfig', 3, NULL, NULL, 1, 0, 19, 3, NULL, 0, 'admin', NOW(), 0),
(54, '新增数据权限', 'AddDataPermission', 3, NULL, NULL, 1, 0, 48, 5, NULL, 0, 'admin', NOW(), 0),
(55, '编辑数据权限', 'EditDataPermission', 3, NULL, NULL, 2, 0, 48, 5, NULL, 0, 'admin', NOW(), 0),
(56, '删除数据权限', 'DeleteDataPermission', 3, NULL, NULL, 3, 0, 48, 5, NULL, 0, 'admin', NOW(), 0),
(107, '新增规则', 'AddAlertRule', 3, NULL, NULL, 1, 0, 104, 4, NULL, 0, 'admin', NOW(), 0),
(108, '编辑规则', 'EditAlertRule', 3, NULL, NULL, 2, 0, 104, 4, NULL, 0, 'admin', NOW(), 0),
(109, '删除规则', 'DeleteAlertRule', 3, NULL, NULL, 3, 0, 104, 4, NULL, 0, 'admin', NOW(), 0),
(110, '启用/禁用', 'ToggleAlertRule', 3, NULL, NULL, 4, 0, 104, 4, NULL, 0, 'admin', NOW(), 0),
(111, '查询历史', 'SearchAlertHistory', 3, NULL, NULL, 1, 0, 105, 4, NULL, 0, 'admin', NOW(), 0),
(112, '确认告警', 'AcknowledgeAlert', 3, NULL, NULL, 2, 0, 105, 4, NULL, 0, 'admin', NOW(), 0),
(117, '查询配置', 'SearchCircuitBreaker', 3, NULL, NULL, 1, 0, 106, 3, NULL, 0, 'admin', NOW(), 0),
(118, '查看详情', 'ViewCircuitBreakerDetail', 3, NULL, NULL, 2, 0, 106, 3, NULL, 0, 'admin', NOW(), 0),
(120, '新增分组', 'AddInstanceGroup', 3, NULL, NULL, 1, 0, 119, 3, NULL, 0, 'admin', NOW(), 0),
(121, '编辑分组', 'EditInstanceGroup', 3, NULL, NULL, 2, 0, 119, 3, NULL, 0, 'admin', NOW(), 0),
(122, '删除分组', 'DeleteInstanceGroup', 3, NULL, NULL, 3, 0, 119, 3, NULL, 0, 'admin', NOW(), 0);


-- ============================================
-- 6. 角色菜单关联 (超级管理员拥有所有菜单)
-- ============================================
INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE delFlag = 0;


-- ============================================
-- 7. 系统配置数据
-- ============================================
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark) VALUES
('gate:admin:system:title', '系统标题', 'Blink Gateway', 0, 1, '系统显示的标题名称', 0, 0, 'admin', NOW(), '系统站点名称'),
('gate:admin:system:logo', '系统Logo', '/logo.png', 0, 1, '系统Logo图片URL', 0, 0, 'admin', NOW(), '系统Logo路径'),
('gate:admin:system:footer', '页脚信息', '© 2026 Blink Gateway Admin', 0, 1, '系统页脚显示的版权信息', 0, 0, 'admin', NOW(), '页脚显示内容'),
('gate:admin:upload:maxSize', '上传文件最大大小(MB)', '10', 1, 2, '单个文件上传的最大大小，单位MB', 0, 0, 'admin', NOW(), '文件上传大小限制'),
('gate:admin:upload:allowTypes', '允许上传的文件类型', '["jpg","jpeg","png","gif","pdf","doc","docx","xls","xlsx"]', 3, 2, '系统允许上传的文件类型列表', 0, 0, 'admin', NOW(), '文件上传类型限制'),
('gate:admin:user:defaultAvatar', '用户默认头像', 'initials', 0, 2, '新用户注册时的默认头像URL', 0, 0, 'admin', NOW(), '用户默认头像'),
('gate:admin:user:passwordMinLength', '密码最小长度', '6', 1, 3, '用户密码的最小长度要求', 0, 0, 'admin', NOW(), '密码最小长度'),
('gate:admin:session:kickoutAfter', '踢出后登录', 'true', 2, 3, '超过最大会话数时是否踢出旧会话', 0, 0, 'admin', NOW(), '踢出后是否允许重新登录'),
('gate:admin:log:enableOperationLog', '启用操作日志', 'true', 2, 4, '是否记录用户操作日志', 0, 0, 'admin', NOW(), '启用操作日志记录'),
('gate:admin:log:enableLoginLog', '启用登录日志', 'true', 2, 4, '是否记录用户登录日志', 0, 0, 'admin', NOW(), '启用登录日志记录'),
('gate:admin:log:retentionDays', '日志保留天数', '30', 1, 4, '系统日志保留的天数', 0, 0, 'admin', NOW(), '日志保留天数'),
('gate:admin:log:enabled', '日志总开关', 'true', 2, 4, '日志功能总开关，关闭后所有日志都不记录', 0, 0, 'admin', NOW(), '日志功能总开关'),
('gate:admin:login:password:maxRetry', '密码最大重试次数', '3', 1, 10, '密码输入错误的最大次数，超过后锁定账户', 0, 0, 'admin', NOW(), '密码最大重试次数'),
('gate:admin:login:captcha:enabled', '登录验证码开关', 'false', 2, 10, '是否开启登录验证码，开启时登录需要验证', 0, 0, 'admin', NOW(), '是否启用验证码'),
('gate:admin:session:maxConcurrent', '最大并发会话数', '1', 1, 10, '控制同一用户可同时登录的最大设备数量', 0, 0, 'admin', NOW(), '最大并发会话数'),
('gate:admin:login:password:lockTime', '账户锁定时间(分钟)', '60', 1, 10, '密码错误次数超限后账户锁定的时间（分钟）', 0, 0, 'admin', NOW(), '账户锁定时间'),
('gateway:route:dynamic:mode', '动态路由模式', 'nacos', 0, 11, '动态路由存储模式：nacos 或 redis', 0, 0, 'admin', NOW(), '动态路由模式'),
('gateway:route:nacos:dataId', 'Nacos路由DataId', 'gateway-routes', 0, 11, 'Nacos配置中心路由配置的DataId', 0, 0, 'admin', NOW(), 'Nacos DataId'),
('gateway:route:nacos:group', 'Nacos路由Group', 'DEFAULT_GROUP', 0, 11, 'Nacos配置中心路由配置的Group', 0, 0, 'admin', NOW(), 'Nacos Group'),
('gateway:route:redis:suffix', 'Redis路由后缀', 'default', 0, 11, 'Redis路由存储的键后缀，用于区分多实例', 0, 0, 'admin', NOW(), 'Redis路由后缀'),
('gateway:local:cache:enable', '本地缓存开关', 'true', 2, 11, '是否开启本地缓存，提升性能', 0, 0, 'admin', NOW(), '本地缓存开关'),
('gateway:signature:enable', '签名校验开关', 'true', 2, 11, '是否开启请求签名校验，开启后需要验证请求签名', 0, 0, 'admin', NOW(), '签名校验开关'),
('gateway:ip:filter:enable', 'IP过滤总开关', 'true', 2, 11, '是否开启IP过滤功能', 0, 0, 'admin', NOW(), 'IP过滤开关'),
('gateway:ip:blacklist:enable', 'IP黑名单开关', 'true', 2, 11, '是否开启IP黑名单，开启后禁止黑名单IP访问', 0, 0, 'admin', NOW(), 'IP黑名单开关'),
('gateway:ip:whitelist:enable', 'IP白名单开关', 'true', 2, 11, '是否开启IP白名单，开启后仅允许白名单IP访问', 0, 0, 'admin', NOW(), 'IP白名单开关'),
('gateway:ip:blacklist:ips', 'IP黑名单列表', '[]', 4, 11, '禁止访问的IP地址列表，支持单个IP、CIDR网段', 0, 0, 'admin', NOW(), 'IP黑名单'),
('gateway:ip:whitelist:ips', 'IP白名单列表', '[]', 4, 11, '允许访问的IP地址列表，支持单个IP、CIDR网段', 0, 0, 'admin', NOW(), 'IP白名单'),
('gateway:replay:defend:enable', '防重放开关', 'false', 2, 11, '是否开启请求重放防护，防止请求被重复提交', 0, 0, 'admin', NOW(), '防重放开关'),
('gateway:replay:effectTime', '防重放有效时间(ms)', '6000', 1, 11, '请求防重放的有效时间，单位毫秒', 0, 0, 'admin', NOW(), '防重放有效时间'),
('gateway:replay:nonceExpireTime', '随机值过期时间(ms)', '6000', 1, 11, '请求随机值的过期时间，单位毫秒', 0, 0, 'admin', NOW(), '随机值过期时间'),
('gateway:api:disable:switch', 'API下线开关', 'false', 2, 11, '是否开启API下线过滤功能', 0, 0, 'admin', NOW(), 'API下线开关'),
('gateway:api:disable:list', '下线API列表', '[]', 4, 11, '临时下线的API路径列表', 0, 0, 'admin', NOW(), '下线API列表'),
('gateway:event:stream:enable', '事件流开关', 'false', 2, 11, '是否开启Redis Stream事件监听', 0, 0, 'admin', NOW(), '事件流开关');


-- ============================================
-- 8. 字典类型数据
-- ============================================
INSERT INTO sys_dict_type (dict_id, dict_type, dict_name, status, order_num, create_by, create_time, remark, locale) VALUES
(1, 'sys_sex', '性别', 0, 1, 'admin', NOW(), '用户性别字典', 'zh_cn'),
(2, 'sys_user_status', '用户状态', 0, 2, 'admin', NOW(), '用户锁定状态', 'zh_cn'),
(3, 'sys_menu_type', '菜单类型', 0, 3, 'admin', NOW(), '菜单类型字典', 'zh_cn'),
(4, 'sys_normal_status', '通用状态', 0, 4, 'admin', NOW(), '通用启用禁用状态', 'zh_cn'),
(5, 'sys_locale', '语言类型', 0, 5, 'admin', NOW(), '语言类型字典', 'zh_cn'),
(6, 'gateway_storage_mode', '路由存储模式', 0, 6, 'admin', NOW(), '动态路由存储模式', 'zh_cn'),
(7, 'gateway_instance_status', '实例状态', 0, 7, 'admin', NOW(), '网关实例状态', 'zh_cn');

-- ============================================
-- 9. 字典数据 - 中文
-- ============================================
-- 性别 (sys_sex)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_sex', '男', '1', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_sex', '女', '2', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_sex', '不确定', '3', NULL, 'info', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 用户状态 (sys_user_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_user_status', '正常', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '管理员锁定', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_user_status', '密码锁定', '2', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 菜单类型 (sys_menu_type)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_menu_type', '目录', '1', NULL, 'primary', 0, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '菜单', '2', NULL, 'success', 1, 0, 2, 'admin', NOW(), 'zh_cn'),
('sys_menu_type', '按钮', '3', NULL, 'warning', 0, 0, 3, 'admin', NOW(), 'zh_cn');

-- 通用状态 (sys_normal_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_normal_status', '启用', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_normal_status', '禁用', '1', NULL, 'danger', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 语言类型 (sys_locale)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('sys_locale', '简体中文', 'zh_cn', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('sys_locale', 'English', 'en_us', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 路由存储模式 (gateway_storage_mode)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('gateway_storage_mode', 'Nacos', 'nacos', NULL, 'primary', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('gateway_storage_mode', 'Redis', 'redis', NULL, 'success', 0, 0, 2, 'admin', NOW(), 'zh_cn');

-- 实例状态 (gateway_instance_status)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, list_class, is_default, status, order_num, create_by, create_time, locale) VALUES
('gateway_instance_status', '在线', '0', NULL, 'success', 1, 0, 1, 'admin', NOW(), 'zh_cn'),
('gateway_instance_status', '离线', '1', NULL, 'warning', 0, 0, 2, 'admin', NOW(), 'zh_cn'),
('gateway_instance_status', '下线', '2', NULL, 'danger', 0, 0, 3, 'admin', NOW(), 'zh_cn');


-- ============================================
-- 10. 默认路由分组
-- ============================================
INSERT INTO gateway_route_group (group_id, group_key, group_name, storage_mode, status, remark, create_by, create_time) VALUES
(1, 'default', '默认分组', 'nacos', 1, '系统默认分组', 'admin', NOW());

-- ============================================
-- 11. 监控配置分组
-- ============================================
INSERT INTO sys_config_group (id, group_key, group_name, parent_id, order_num, status, create_by, create_time, remark) VALUES
(100, 'gateway_monitor', '网关监控配置', 0, 100, 0, 'admin', NOW(), '网关监控相关配置项');

-- 监控配置项
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark) VALUES
('monitor.enabled', '监控开关', 'true', 2, 100, '是否启用网关监控（true/false）', 0, 0, 'admin', NOW(), '控制gateway-admin消费监控消息和gateway-reactive推送监控指标'),
('monitor.interval-ms', '推送间隔', '5000', 1, 100, '指标推送间隔（毫秒）', 0, 0, 'admin', NOW(), 'gateway-reactive向Redis Stream推送指标的间隔'),
('monitor.initial-delay-ms', '首次延迟', '5000', 1, 100, '首次推送延迟（毫秒）', 0, 0, 'admin', NOW(), 'gateway-reactive启动后首次推送指标的延迟'),
('monitor.history-retention-days', '历史保留天数', '7', 1, 100, '监控历史数据保留天数', 0, 0, 'admin', NOW(), 'gateway-admin清理过期监控数据的天数'),
('monitor.cpu-change-threshold', 'CPU变化阈值', '10', 1, 100, 'CPU使用率变化阈值（百分比）', 0, 0, 'admin', NOW(), 'CPU使用率变化超过此阈值才记录历史数据'),
('monitor.heap-change-threshold', '堆内存变化阈值', '10', 1, 100, '堆内存使用率变化阈值（百分比）', 0, 0, 'admin', NOW(), '堆内存使用率变化超过此阈值才记录历史数据');

-- ============================================
-- 完成初始化
-- ============================================
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Gateway Admin 初始化完成!' AS message;
SELECT CONCAT('已创建 ', COUNT(*), ' 个菜单') AS menu_count FROM sys_menu WHERE delFlag = 0;
SELECT CONCAT('已创建 ', COUNT(*), ' 个角色') AS role_count FROM sys_role WHERE delFlag = 0;
SELECT CONCAT('已创建 ', COUNT(*), ' 个配置项') AS config_count FROM sys_config WHERE status = 0;
SELECT CONCAT('已创建 ', COUNT(*), ' 个字典类型') AS dict_type_count FROM sys_dict_type WHERE status = 0;
SELECT CONCAT('已创建 ', COUNT(*), ' 个字典数据') AS dict_data_count FROM sys_dict_data WHERE status = 0;

-- ============================================
-- 增量变更：路由分组表增加存储方式字段
-- ============================================
-- ALTER TABLE gateway_route_group ADD COLUMN storage_mode VARCHAR(16) DEFAULT 'nacos' COMMENT '存储方式：nacos/redis' AFTER group_name;
-- UPDATE gateway_route_group SET storage_mode = 'nacos' WHERE storage_mode IS NULL;

