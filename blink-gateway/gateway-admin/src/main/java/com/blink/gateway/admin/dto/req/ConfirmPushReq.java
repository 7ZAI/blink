package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 确认推送请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class ConfirmPushReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推送记录ID
     */
    private Long pushId;

    /**
     * 备注（可选）
     */
    private String remark;
}
