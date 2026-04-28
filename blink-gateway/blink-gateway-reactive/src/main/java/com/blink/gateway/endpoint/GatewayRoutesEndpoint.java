package com.blink.gateway.endpoint;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关路由 Actuator 端点
 * 用于获取当前实例内存中加载的路由定义
 *
 * <p>使用 RouteDefinitionLocator 聚合所有路由来源（配置文件 + 动态路由）</p>
 *
 * @author binblink
 * @since 2026-04-16
 */
@Endpoint(id = "gateway-routes")
@Component
@Slf4j
public class GatewayRoutesEndpoint {

    /**
     * 使用 RouteDefinitionLocator 聚合所有路由来源
     * 包括：配置文件定义的路由、动态路由（Nacos/Redis）等
     */
    @Resource
    private RouteDefinitionLocator routeDefinitionLocator;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 获取当前实例的所有路由定义
     *
     * @return 路由列表响应
     */
    @ReadOperation
    public GatewayRoutesResponse getRoutes() {
        String instanceId = getInstanceId();

        try {
            // 使用 RouteDefinitionLocator 获取所有路由（聚合所有来源）
            List<RouteDefinition> routes = routeDefinitionLocator
                .getRouteDefinitions()
                .collectList()
                .block();

            GatewayRoutesResponse response = GatewayRoutesResponse.builder()
                .instanceId(instanceId)
                .timestamp(LocalDateTime.now())
                .routes(routes != null ? routes : new ArrayList<>())
                .routeCount(routes != null ? routes.size() : 0)
                .build();

            log.info("[GatewayRoutesEndpoint] 获取实例路由 | instanceId: {}, count: {}",
                instanceId, response.getRouteCount());

            return response;
        } catch (Exception e) {
            log.error("[GatewayRoutesEndpoint] 获取路由失败 | instanceId: {}, error: {}",
                instanceId, e.getMessage(), e);

            return GatewayRoutesResponse.builder()
                .instanceId(instanceId)
                .timestamp(LocalDateTime.now())
                .routes(new ArrayList<>())
                .routeCount(0)
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * 获取实例 ID
     */
    private String getInstanceId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return "gateway-app:" + hostAddress + ":" + serverPort;
        } catch (Exception e) {
            return "gateway-app:unknown:" + serverPort;
        }
    }

    /**
     * 网关路由响应
     */
    @Data
    @Builder
    public static class GatewayRoutesResponse {
        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 获取时间
         */
        private LocalDateTime timestamp;

        /**
         * 路由列表
         */
        private List<RouteDefinition> routes;

        /**
         * 路由数量
         */
        private Integer routeCount;

        /**
         * 错误信息
         */
        private String error;
    }
}
