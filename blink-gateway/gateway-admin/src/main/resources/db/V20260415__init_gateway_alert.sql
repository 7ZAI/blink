-- 告警规则表
CREATE TABLE IF NOT EXISTS gateway_alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: RESOURCE/PERFORMANCE/ERROR/INSTANCE',
    conditions TEXT NOT NULL COMMENT '触发条件JSON: [{"metricName":"p99","operator":"gt","threshold":1000,"durationMinutes":3}]',
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING' COMMENT '严重程度: INFO/WARNING/ERROR',
    notify_channels VARCHAR(100) DEFAULT 'IN_APP' COMMENT '通知渠道: IN_APP,EMAIL,WEBHOOK',
    notify_template VARCHAR(500) COMMENT '通知模板，支持变量: {{rule_name}},{{instance_id}},{{metric_name}},{{value}},{{threshold}}',
    suppress_minutes INT DEFAULT 5 COMMENT '重复告警间隔(分钟)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_rule_type (rule_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关告警规则表';

-- 告警历史表
CREATE TABLE IF NOT EXISTS gateway_alert_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    instance_id VARCHAR(200) COMMENT '关联实例ID',
    alert_title VARCHAR(200) NOT NULL COMMENT '告警标题',
    alert_content TEXT COMMENT '告警内容(模板渲染后)',
    triggered_conditions TEXT COMMENT '触发的条件详情JSON',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度: INFO/WARNING/ERROR',
    status VARCHAR(20) NOT NULL DEFAULT 'FIRING' COMMENT '状态: FIRING/RESOLVED/ACKNOWLEDGED',
    fired_time DATETIME NOT NULL COMMENT '触发时间',
    resolved_time DATETIME COMMENT '恢复时间',
    acknowledged_time DATETIME COMMENT '确认时间',
    acknowledged_by INT COMMENT '确认人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_status (status),
    INDEX idx_fired_time (fired_time),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关告警历史表';

-- 插入默认告警规则示例数据
INSERT INTO gateway_alert_rule (rule_name, rule_type, conditions, severity, notify_channels, notify_template, suppress_minutes, enabled) VALUES
('CPU使用率告警', 'RESOURCE', '[{"metricName":"cpuUsage","operator":"gt","threshold":80,"durationMinutes":3}]', 'WARNING', 'IN_APP', '告警规则 {{rule_name}} 触发\n实例 {{instance_id}} 的 CPU使用率 当前值 {{value}}%, 超过阈值 {{threshold}}%', 5, 1),
('内存使用率告警', 'RESOURCE', '[{"metricName":"memoryUsage","operator":"gt","threshold":85,"durationMinutes":5}]', 'WARNING', 'IN_APP', '告警规则 {{rule_name}} 触发\n实例 {{instance_id}} 的 内存使用率 当前值 {{value}}%, 超过阈值 {{threshold}}%', 10, 1),
('P99响应时间告警', 'PERFORMANCE', '[{"metricName":"p99ResponseTime","operator":"gt","threshold":1000,"durationMinutes":3}]', 'WARNING', 'IN_APP', '告警规则 {{rule_name}} 触发\n实例 {{instance_id}} 的 P99响应时间 当前值 {{value}}ms, 超过阈值 {{threshold}}ms', 3, 0),
('错误率告警', 'ERROR', '[{"metricName":"errorRate","operator":"gt","threshold":5,"durationMinutes":2}]', 'ERROR', 'IN_APP', '告警规则 {{rule_name}} 触发\n实例 {{instance_id}} 的 错误率 当前值 {{value}}%, 超过阈值 {{threshold}}%', 5, 0);