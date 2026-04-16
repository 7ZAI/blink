package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 告警规则 ID 请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AlertRuleIdReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则 ID
     */
    private Long id;
}