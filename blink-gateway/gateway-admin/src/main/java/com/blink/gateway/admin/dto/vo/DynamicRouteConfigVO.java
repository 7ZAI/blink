package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关实例动态路由配置 VO
 * 对应配置文件中的 blink.gateway.dynamicRoute 配置
 *
 * @author binblink
 * @since 2026-04-26
 */
@Data
public class DynamicRouteConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由模式：redis 或 nacos
     */
    private String mode;

    /**
     * 路由分组
     */
    private String group;

    /**
     * Redis 配置
     */
    private RedisConfigVO redis;

    /**
     * Nacos 配置
     */
    private NacosConfigVO nacos;

    /**
     * Redis 配置
     */
    @Data
    public static class RedisConfigVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 路由后缀
         */
        private String routeSuffix;
    }

    /**
     * Nacos 配置
     */
    @Data
    public static class NacosConfigVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Data ID
         */
        private String dataId;

        /**
         * Group
         */
        private String group;
    }
}
