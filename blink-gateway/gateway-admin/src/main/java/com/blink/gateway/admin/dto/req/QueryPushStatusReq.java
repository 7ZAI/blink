package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 查询推送状态请求DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class QueryPushStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID列表
     */
    private List<String> routeIds;

    /**
     * 路由分组
     */
    private String routesGroup;

    /**
     * 推送状态：0-未推送 1-已推送 2-推送失败
     */
    private Byte pushStatus;
}