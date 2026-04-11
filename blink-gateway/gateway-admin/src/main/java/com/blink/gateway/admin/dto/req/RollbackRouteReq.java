package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 回滚路由请求DTO
 * 用于将路由配置回滚到指定的历史版本
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
public class RollbackRouteReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由ID
     */
    private String routeId;

    /**
     * 历史记录ID
     * 指定回滚到此历史版本
     */
    private Long historyId;

    /**
     * 是否同步到运行时存储
     * 默认为 true
     */
    private Boolean syncToStorage;
}