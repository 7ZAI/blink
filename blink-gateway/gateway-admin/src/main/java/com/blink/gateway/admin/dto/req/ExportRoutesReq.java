package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 导出路由请求DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class ExportRoutesReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID列表（为空时导出全部）
     */
    private List<String> routeIds;

    /**
     * 路由分组（导出指定分组）
     */
    private String routesGroup;

    /**
     * 存储方式筛选
     */
    private String storageMode;
}