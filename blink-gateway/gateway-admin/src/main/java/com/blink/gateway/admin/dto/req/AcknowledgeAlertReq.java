package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 确认告警请求
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
public class AcknowledgeAlertReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 告警历史 ID
     */
    private Long id;
}