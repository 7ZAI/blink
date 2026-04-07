package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 查询历史消息请求
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryHistoryReq extends Page {

    private String type;

    private String severity;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}