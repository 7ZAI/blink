package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * 查询路由历史请求DTO
 * 用于查询路由的变更历史记录
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
public class QueryRouteHistoryReq extends Page {

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 操作类型：A/M/D
     */
    private String operationType;

    /**
     * 操作人名称
     */
    private String operatorName;
}