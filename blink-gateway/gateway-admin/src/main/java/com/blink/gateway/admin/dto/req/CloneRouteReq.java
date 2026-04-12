package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 克隆路由请求DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class CloneRouteReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源路由ID
     */
    private String sourceRouteId;

    /**
     * 新路由ID
     */
    private String newRouteId;

    /**
     * 新路由名称
     */
    private String newRouteName;
}