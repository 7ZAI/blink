package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Data;

import java.io.Serial;
import java.util.List;

/**
 * 路由差异项
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class RouteDiffItem {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 差异类型: added, modified, deleted, unchanged
     */
    private String diffType;

    /**
     * 仓库版本的路由配置
     */
    private GaRouteDO repositoryRoute;

    /**
     * 实例版本的路由配置
     */
    private GaRouteDO instanceRoute;

    /**
     * 字段级差异（仅 modified 时有）
     */
    private List<FieldDiff> fieldDiffs;
}