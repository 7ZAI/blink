package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 网关路由配置实体类
 * 对应数据库表 ga_route
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName(value = "ga_route", autoResultMap = true)
public class GaRouteDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由ID（主键，业务标识）
     */
    @TableId("route_id")
    private String routeId;

    /**
     * 路由名称
     */
    @TableField("route_name")
    private String routeName;

    /**
     * 目标URI
     * 如 lb://service-name 或 https://example.com
     */
    @TableField("uri")
    private String uri;

    /**
     * 断言配置JSON数组
     * 格式: [{"name": "Path", "args": {"pattern": "/api/**"}}]
     */
    @TableField(value = "predicates", typeHandler = JacksonTypeHandler.class)
    private List<PredicateConfig> predicates;

    /**
     * 过滤器配置JSON数组
     * 格式: [{"name": "StripPrefix", "args": {"parts": "1"}}]
     */
    @TableField(value = "filters", typeHandler = JacksonTypeHandler.class)
    private List<FilterConfig> filters;

    /**
     * 路由顺序
     * 数值越小优先级越高
     */
    @TableField("order_num")
    private Integer orderNum;

    /**
     * 元数据JSON对象
     */
    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 路由分组
     * 用于 Redis 模式下的路由存储分组
     */
    @TableField("routes_group")
    private String routesGroup;

    /**
     * 存储方式
     * redis: 存储在 Redis Hash
     * nacos: 存储在 Nacos Config
     */
    @TableField("storage_mode")
    private String storageMode;

    /**
     * Nacos Data ID
     * 用于 Nacos 模式下的配置存储
     */
    @TableField("nacos_data_id")
    private String nacosDataId;

    /**
     * Nacos Group
     * 用于 Nacos 模式下的配置存储
     */
    @TableField("nacos_group")
    private String nacosGroup;

    /**
     * 状态
     * 1: 启用
     * 0: 禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}