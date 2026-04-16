package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 网关告警历史持久化对象
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
@TableName("gateway_alert_history")
public class GatewayAlertHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则 ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 关联实例 ID
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 告警标题
     */
    @TableField("alert_title")
    private String alertTitle;

    /**
     * 告警内容 (模板渲染后)
     */
    @TableField("alert_content")
    private String alertContent;

    /**
     * 触发的条件详情 JSON
     */
    @TableField("triggered_conditions")
    private String triggeredConditions;

    /**
     * 严重程度: INFO/WARNING/ERROR
     */
    @TableField("severity")
    private String severity;

    /**
     * 状态: FIRING/RESOLVED/ACKNOWLEDGED
     */
    @TableField("status")
    private String status;

    /**
     * 触发时间
     */
    @TableField("fired_time")
    private LocalDateTime firedTime;

    /**
     * 恢复时间
     */
    @TableField("resolved_time")
    private LocalDateTime resolvedTime;

    /**
     * 确认时间
     */
    @TableField("acknowledged_time")
    private LocalDateTime acknowledgedTime;

    /**
     * 确认人 ID
     */
    @TableField("acknowledged_by")
    private Integer acknowledgedBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}