package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 标记已读请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class MarkReadReq implements Serializable {

    private Long notificationId;

    private Boolean markAll = false;
}