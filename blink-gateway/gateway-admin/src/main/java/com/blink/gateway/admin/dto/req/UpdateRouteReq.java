package com.blink.gateway.admin.dto.req;

import com.blink.gateway.admin.entity.PredicateConfig;
import com.blink.gateway.admin.entity.FilterConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 更新路由请求DTO
 * 用于修改单个路由配置
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
public class UpdateRouteReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID（必填，业务唯一标识）
     */
    private String routeId;

    /**
     * 路由名称
     */
    private String routeName;

    /**
     * 目标URI
     */
    private String uri;

    /**
     * 断言配置列表
     */
    private List<PredicateConfig> predicates;

    /**
     * 过滤器配置列表
     */
    private List<FilterConfig> filters;

    /**
     * 路由顺序
     */
    private Integer orderNum;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 路由分组
     */
    private String routesGroup;

    /**
     * 存储方式
     */
    private String storageMode;

    /**
     * Nacos Data ID
     */
    private String nacosDataId;

    /**
     * Nacos Group
     */
    private String nacosGroup;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;
}