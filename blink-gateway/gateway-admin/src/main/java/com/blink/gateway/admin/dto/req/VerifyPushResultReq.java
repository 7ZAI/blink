package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;

/**
 * 验证推送结果请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class VerifyPushResultReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推送记录ID
     */
    private Long pushId;

    /**
     * 实例ID（可选，不传则验证所有目标实例）
     */
    private String instanceId;
}
