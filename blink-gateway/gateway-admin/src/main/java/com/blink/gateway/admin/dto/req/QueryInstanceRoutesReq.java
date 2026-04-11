package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;

/**
 * 查询实例路由请求
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
public class QueryInstanceRoutesReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储方式: redis/nacos
     */
    private String storageMode;

    /**
     * 路由分组（Redis模式）
     */
    private String routesGroup;

    /**
     * Nacos Data ID（Nacos模式）
     */
    private String nacosDataId;

    /**
     * Nacos Group（Nacos模式）
     */
    private String nacosGroup;

    /**
     * 目标实例ID（可选，用于查看特定实例的路由）
     */
    private String instanceId;
}