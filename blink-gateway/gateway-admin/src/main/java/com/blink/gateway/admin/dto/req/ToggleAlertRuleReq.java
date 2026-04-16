package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 切换告警规则启用状态请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class ToggleAlertRuleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则 ID
     */
    private Long id;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    private Byte enabled;
}