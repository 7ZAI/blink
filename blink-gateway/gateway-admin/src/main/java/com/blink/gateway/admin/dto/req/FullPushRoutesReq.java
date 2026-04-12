package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 全量推送路由请求DTO
 * 一键推送指定分组下所有启用状态路由
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class FullPushRoutesReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储方式：redis/nacos
     */
    private String storageMode;

    /**
     * 路由分组（Redis模式）
     * 为空时推送所有分组的路由
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