-- ============================================
-- 会话管理配置项迁移脚本
-- @author binblink
-- @since 2026-03-26
-- ============================================

-- ============================================
-- 新增最大并发会话数配置项
-- ============================================

INSERT INTO sys_config (config_key, config_name, config_value, config_type, group_id, description, readonly, status, create_by, create_time, remark)
VALUES ('base:session:maxConcurrent', '最大并发会话数', '3', 1, 4, '控制同一用户可同时登录的最大设备数量', 0, 0, 'admin', NOW(), '控制同一用户可同时登录的最大设备数量，默认为3')
ON DUPLICATE KEY UPDATE config_value = '3', update_time = NOW();