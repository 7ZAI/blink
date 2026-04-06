package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询消息通知请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
public class QueryNotificationReq implements Serializable {

    private Integer limit = 20;
}