package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;

import java.io.Serial;
import java.util.List;

/**
 * 路由差异对比响应
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class RouteDiffRsp {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 仓库路由（待推送）
     */
    private List<GaRouteDO> repositoryRoutes;

    /**
     * 仓库路由数量
     */
    private Integer repositoryCount;

    /**
     * 实例路由（当前运行）
     */
    private List<GaRouteDO> instanceRoutes;

    /**
     * 实例路由数量
     */
    private Integer instanceCount;

    /**
     * 差异统计
     */
    private DiffStats diffStats;

    /**
     * 差异详情列表
     */
    private List<RouteDiffItem> diffDetails;
}