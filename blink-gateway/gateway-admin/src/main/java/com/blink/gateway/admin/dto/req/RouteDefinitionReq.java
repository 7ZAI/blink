package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 路由定义请求DTO
 * 用于保存路由时的单个路由定义
 *
 * @author binblink
 */
@Getter
@Setter
public class RouteDefinitionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String id;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 路由URI
     */
    private String uri;

    /**
     * 断言定义列表
     */
    private String predicates;

    /**
     * 过滤器定义列表
     */
    private String filters;

    /**
     * 路由顺序
     */
    private Integer order;

    /**
     * 元数据
     */
    private String metadata;
}