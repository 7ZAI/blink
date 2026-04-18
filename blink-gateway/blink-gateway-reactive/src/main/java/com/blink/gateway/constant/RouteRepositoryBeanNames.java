package com.blink.gateway.constant;

/**
 * 路由仓库 Bean 名称常量
 *
 * @author binblink
 */
public interface RouteRepositoryBeanNames {

    /**
     * Nacos 路由定义仓库 Bean 名称
     */
    String NACOS_REPOSITORY = "nacosRouteDefinitionRepository";

    /**
     * 动态路由属性监听器 Bean 名称
     */
    String NACOS_LISTENER = "dynamicRoutePropertiesListener";

    /**
     * Redis 路由定义仓库 Bean 名称
     */
    String REDIS_REPOSITORY = "redisRouteDefinitionRepository";
}