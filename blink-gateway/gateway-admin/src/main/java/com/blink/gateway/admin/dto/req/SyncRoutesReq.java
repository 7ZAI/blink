package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 同步路由到实例请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class SyncRoutesReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储方式: redis / nacos
     */
    private String storageMode;

    /**
     * 路由组（Redis模式必填）
     */
    private String routesGroup;

    /**
     * Nacos dataId（Nacos模式必填）
     */
    private String dataId;

    /**
     * Nacos group（Nacos模式必填）
     */
    private String group;

    /**
     * 推送模式: broadcast / specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式必填）
     */
    private List<String> targetInstanceIds;

    /**
     * 待同步的路由ID列表（可选，为空则同步全部）
     */
    private List<String> routeIds;
}