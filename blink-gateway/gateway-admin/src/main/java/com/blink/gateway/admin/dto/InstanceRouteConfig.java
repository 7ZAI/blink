package com.blink.gateway.admin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例路由配置
 * 用于存储从实例获取的路由配置信息
 *
 * @author binblink
 * @since 2026-04-16
 */
@Data
public class InstanceRouteConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储方式：redis 或 nacos
     */
    private String storageMode;

    /**
     * 路由分组（Redis 模式使用）
     */
    private String routesGroup;

    /**
     * Nacos Data ID（Nacos 模式使用）
     */
    private String nacosDataId;

    /**
     * Nacos Group（Nacos 模式使用）
     */
    private String nacosGroup;
}
