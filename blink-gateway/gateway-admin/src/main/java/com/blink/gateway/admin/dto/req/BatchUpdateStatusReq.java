package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 批量更新状态请求DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class BatchUpdateStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID列表
     */
    private List<String> routeIds;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

    /**
     * 路由分组（批量更新指定分组下所有路由时使用）
     */
    private String routesGroup;
}