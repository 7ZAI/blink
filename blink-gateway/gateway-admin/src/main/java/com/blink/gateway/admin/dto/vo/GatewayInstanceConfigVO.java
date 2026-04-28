package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关实例配置 VO
 * 对应配置文件中的 blink.gateway 配置
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class GatewayInstanceConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例标识
     */
    private String instanceId;

    /**
     * 实例是否启用
     */
    private Boolean instanceEnabled;

    /**
     * 维护模式
     */
    private Boolean maintenanceMode;

    /**
     * 实例权重
     */
    private Integer instanceWeight;

    /**
     * 签名配置
     */
    private SignatureConfigVO signature;

    /**
     * 重放防御配置
     */
    private ReplayDefendConfigVO replayDefend;

    /**
     * 本地缓存启用
     */
    private Boolean localCacheEnable;

    /**
     * 事件流启用
     */
    private Boolean eventStreamEnable;

    /**
     * 动态路由配置
     */
    private DynamicRouteConfigVO dynamicRoute;

    /**
     * 签名配置
     */
    @Data
    public static class SignatureConfigVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否启用
         */
        private Boolean enable;
    }

    /**
     * 重放防御配置
     */
    @Data
    public static class ReplayDefendConfigVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否启用
         */
        private Boolean enable;
    }
}
