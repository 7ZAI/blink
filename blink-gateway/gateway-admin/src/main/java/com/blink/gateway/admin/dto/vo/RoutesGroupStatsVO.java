package com.blink.gateway.admin.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 路由分组统计VO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class RoutesGroupStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    private String routesGroup;

    /**
     * 总路由数
     */
    private Integer totalCount;

    /**
     * 启用路由数
     */
    private Integer enabledCount;

    /**
     * 禁用路由数
     */
    private Integer disabledCount;

    /**
     * 已推送路由数
     */
    private Integer pushedCount;

    /**
     * 未推送路由数
     */
    private Integer notPushedCount;

    /**
     * 推送失败路由数
     */
    private Integer pushFailedCount;
}