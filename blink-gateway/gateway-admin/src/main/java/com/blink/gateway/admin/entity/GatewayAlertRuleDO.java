package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关告警规则持久化对象
 *
 * 支持多条件 AND 逻辑，条件存储为 JSON 格式
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
@TableName("gateway_alert_rule")
public class GatewayAlertRuleDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则类型: RESOURCE/PERFORMANCE/ERROR/INSTANCE
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 触发条件 JSON
     * 格式: [{"metricName":"p99","operator":"gt","threshold":1000,"durationMinutes":3}]
     */
    @TableField("conditions")
    private String conditions;

    /**
     * 严重程度: INFO/WARNING/ERROR
     */
    @TableField("severity")
    private String severity;

    /**
     * 通知渠道: IN_APP,EMAIL,WEBHOOK (逗号分隔)
     */
    @TableField("notify_channels")
    private String notifyChannels;

    /**
     * 通知模板
     * 支持变量: {{rule_name}},{{instance_id}},{{metric_name}},{{value}},{{threshold}}
     */
    @TableField("notify_template")
    private String notifyTemplate;

    /**
     * 重复告警间隔 (分钟)
     */
    @TableField("suppress_minutes")
    private Integer suppressMinutes;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    @TableField("enabled")
    private Byte enabled;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}