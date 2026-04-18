package com.blink.gateway.event;

/**
 * 动态路由配置变化类型
 *
 * @author binblink
 */
public enum ChangeType {

    /**
     * 模式切换：nacos ↔ redis
     */
    MODE_SWITCH,

    /**
     * 路由分组变化：group 变化
     */
    ROUTE_GROUP_CHANGE,

    /**
     * Nacos 配置变化：dataId 或 group 变化
     */
    NACOS_CONFIG_CHANGE,

    /**
     * Redis 配置变化：routeSuffix 变化
     */
    REDIS_CONFIG_CHANGE
}