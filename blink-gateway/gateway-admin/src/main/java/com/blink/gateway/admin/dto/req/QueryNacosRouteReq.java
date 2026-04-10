package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Nacos 路由查询请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryNacosRouteReq extends Page {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Nacos dataId
     */
    private String dataId;

    /**
     * Nacos group
     */
    private String group;
}