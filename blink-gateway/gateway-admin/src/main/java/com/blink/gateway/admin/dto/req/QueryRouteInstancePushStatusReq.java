package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.util.List;

/**
 * 查询路由实例推送状态请求
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class QueryRouteInstancePushStatusReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由ID列表
     */
    private List<String> routeIds;

    /**
     * 单个路由ID（可选）
     */
    private String routeId;
}
