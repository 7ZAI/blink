package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 查询实例推送历史请求
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstancePushHistoryReq extends Page {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 网关实例ID
     */
    private String instanceId;
}