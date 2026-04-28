package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;

/**
 * 查询配置中心路由请求
 * 从 Redis/Nacos 配置中心查询已推送的路由配置
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
public class QueryStorageRoutesReq {

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
}
