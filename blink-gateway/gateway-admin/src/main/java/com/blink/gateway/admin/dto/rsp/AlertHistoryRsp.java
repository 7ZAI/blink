package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 告警历史响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AlertHistoryRsp {

    /**
     * 告警 ID
     */
    private Long id;

    /**
     * 规则 ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 关联实例 ID
     */
    private String instanceId;

    /**
     * 告警标题
     */
    private String alertTitle;

    /**
     * 告警内容
     */
    private String alertContent;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 状态: FIRING/RESOLVED/ACKNOWLEDGED
     */
    private String status;

    /**
     * 触发时间
     */
    private LocalDateTime firedTime;

    /**
     * 恢复时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 确认时间
     */
    private LocalDateTime acknowledgedTime;

    /**
     * 确认人 ID
     */
    private Integer acknowledgedBy;
}