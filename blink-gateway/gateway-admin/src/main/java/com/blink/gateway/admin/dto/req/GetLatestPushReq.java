package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;

/**
 * 获取最新推送请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class GetLatestPushReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;
}
