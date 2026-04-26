package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.util.List;

/**
 * 从实例同步路由响应
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class SyncRoutesFromInstanceRsp {

    /**
     * 新增路由数量
     */
    private Integer addedCount;

    /**
     * 更新路由数量
     */
    private Integer updatedCount;

    /**
     * 新增的路由ID列表
     */
    private List<String> addedRoutes;

    /**
     * 更新的路由ID列表
     */
    private List<String> updatedRoutes;
}