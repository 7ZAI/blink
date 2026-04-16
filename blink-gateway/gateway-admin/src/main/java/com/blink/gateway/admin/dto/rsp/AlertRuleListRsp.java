package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 告警规则列表响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AlertRuleListRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 规则列表
     */
    private List<AlertRuleRsp> rules;
}