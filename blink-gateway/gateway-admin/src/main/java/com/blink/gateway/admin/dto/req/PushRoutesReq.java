package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 推送路由请求
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PushRoutesReq extends Page {

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
     * 要推送的路由ID列表
     */
    private List<String> routeIds;

    /**
     * 推送模式: broadcast/specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式）
     */
    private List<String> targetInstanceIds;

    /**
     * 备注说明
     */
    private String remark;
}