-- ============================================
-- 网关监控配置初始化脚本
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 添加监控配置分组（如果不存在）
-- ============================================

-- 查找或创建网关监控配置分组
INSERT INTO sys_config_group (group_key, group_name, parent_id, order_num, status, create_by, create_time, remark)
SELECT 'gateway_monitor', '网关监控配置', 0, 100, 0, 'admin', NOW(), '网关监控相关配置项'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config_group WHERE group_key = 'gateway_monitor'
);

-- 获取分组ID（用于后续插入）
SET @monitor_group_id = (SELECT id FROM sys_config_group WHERE group_key = 'gateway_monitor');

-- ============================================
-- 2. 添加监控配置项
-- ============================================

-- 监控开关配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.enabled', '监控开关', 'true', 2, @monitor_group_id, '是否启用网关监控（true/false）', 0, 0, 'admin', NOW(), '控制gateway-admin消费监控消息和gateway-reactive推送监控指标'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.enabled'
);

-- 指标推送间隔配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.interval-ms', '推送间隔', '5000', 1, @monitor_group_id, '指标推送间隔（毫秒）', 0, 0, 'admin', NOW(), 'gateway-reactive向Redis Stream推送指标的间隔，5秒实现准实时监控'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.interval-ms'
);

-- 首次推送延迟配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.initial-delay-ms', '首次延迟', '5000', 1, @monitor_group_id, '首次推送延迟（毫秒）', 0, 0, 'admin', NOW(), 'gateway-reactive启动后首次推送指标的延迟'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.initial-delay-ms'
);

-- 历史数据保留天数
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.history-retention-days', '历史保留天数', '7', 1, @monitor_group_id, '监控历史数据保留天数', 0, 0, 'admin', NOW(), 'gateway-admin清理过期监控数据的天数'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.history-retention-days'
);

-- CPU使用率变化阈值
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.cpu-change-threshold', 'CPU变化阈值', '10', 1, @monitor_group_id, 'CPU使用率变化阈值（百分比）', 0, 0, 'admin', NOW(), 'CPU使用率变化超过此阈值才记录历史数据'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.cpu-change-threshold'
);

-- 堆内存使用率变化阈值
INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
SELECT 'monitor.heap-change-threshold', '堆内存变化阈值', '10', 1, @monitor_group_id, '堆内存使用率变化阈值（百分比）', 0, 0, 'admin', NOW(), '堆内存使用率变化超过此阈值才记录历史数据'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'monitor.heap-change-threshold'
);

-- ============================================
-- 说明：
-- 1. monitor.enabled: 控制监控消息的推送和消费
--    - true: gateway-reactive 推送指标到 Redis Stream，gateway-admin 消费消息
--    - false: 停止推送和消费，不影响 gateway-reactive 自身的 Micrometer 数据采集
--
-- 2. monitor.interval-ms: 指标推送间隔，单位毫秒，默认 5 秒（准实时监控）
--
-- 3. monitor.initial-delay-ms: 首次推送延迟，单位毫秒，默认 5 秒
--
-- 4. 配置变更会通过 Redis Stream 同步到所有 gateway-reactive 实例
-- ============================================
