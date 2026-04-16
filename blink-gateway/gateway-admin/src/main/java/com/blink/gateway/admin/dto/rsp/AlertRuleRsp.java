package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AlertRuleRsp {

    /**
     * 规则 ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 触发条件列表
     */
    private List<AlertConditionRsp> conditions;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 通知渠道
     */
    private List<String> notifyChannels;

    /**
     * 通知模板
     */
    private String notifyTemplate;

    /**
     * 重复告警间隔
     */
    private Integer suppressMinutes;

    /**
     * 是否启用
     */
    private Byte enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 告警条件响应
     */
    @Data
    public static class AlertConditionRsp {
        /**
         * 指标名称
         */
        private String metricName;

        /**
         * 操作符
         */
        private String operator;

        /**
         * 阈值
         */
        private Double threshold;

        /**
         * 持续时间
         */
        private Integer durationMinutes;
    }
}