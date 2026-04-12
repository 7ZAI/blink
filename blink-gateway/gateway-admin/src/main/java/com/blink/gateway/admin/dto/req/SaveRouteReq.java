package com.blink.gateway.admin.dto.req;

import com.blink.gateway.admin.entity.PredicateConfig;
import com.blink.gateway.admin.entity.FilterConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 保存路由请求DTO
 * 用于新增单个路由配置
 *
 * @author binblink
 */
@Getter
@Setter
public class SaveRouteReq implements Serializable {

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
     * 目标URI（必填）
     * 如 lb://service-name 或 https://example.com
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
     * 路由分组（用于 Redis 模式）
     */
    private String routesGroup;

    /**
     * 存储方式：redis/nacos
     */
    private String storageMode;

    /**
     * Nacos Data ID（用于 Nacos 模式）
     */
    private String nacosDataId;

    /**
     * Nacos Group（用于 Nacos 模式）
     */
    private String nacosGroup;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否自动同步到运行时存储
     * true: 保存后自动推送
     * false: 仅保存到数据库，需手动推送
     */
    private Boolean autoSync;
}