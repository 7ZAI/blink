package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 查询告警规则请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryAlertRuleReq extends Page {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则类型筛选
     */
    private String ruleType;

    /**
     * 是否启用
     */
    private Byte enabled;
}