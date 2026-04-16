package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增/更新告警规则请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AddAlertRuleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则 ID (更新时必填)
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型: RESOURCE/PERFORMANCE/ERROR/INSTANCE
     */
    private String ruleType;

    /**
     * 触发条件列表 (AND 逻辑)
     */
    private List<AlertConditionReq> conditions;

    /**
     * 严重程度: INFO/WARNING/ERROR
     */
    private String severity;

    /**
     * 通知渠道: IN_APP,EMAIL,WEBHOOK
     */
    private List<String> notifyChannels;

    /**
     * 通知模板
     */
    private String notifyTemplate;

    /**
     * 重复告警间隔 (分钟)
     */
    private Integer suppressMinutes;

    /**
     * 是否启用
     */
    private Byte enabled;

    /**
     * 告警条件请求
     */
    @Data
    public static class AlertConditionReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 指标名称
         */
        private String metricName;

        /**
         * 操作符: gt,lt,gte,lte
         */
        private String operator;

        /**
         * 阈值
         */
        private Double threshold;

        /**
         * 持续时间 (分钟)
         */
        private Integer durationMinutes;
    }
}