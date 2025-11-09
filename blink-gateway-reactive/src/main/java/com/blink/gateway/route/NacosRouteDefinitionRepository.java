package com.blink.gateway.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


/**
 * 自定义路由仓库 实现从配置中心拉取路由json配置文件
 * 由自定配置类GatewayAutoConfiguration自动配置取代默认的 InMemoryRouteDefinitionRepository
 *
 * @author binblink
 */
public class NacosRouteDefinitionRepository implements RouteDefinitionRepository {

    private final NacosConfigManager nacosConfigManager;

    public NacosRouteDefinitionRepository(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        // 从 Nacos 获取配置（例如，Data ID: "gateway-routes", Group: "DEFAULT_GROUP"）
        String configInfo = null;
        try {
            configInfo = nacosConfigManager.getConfigService().getConfig("gateway-routes", "DEFAULT_GROUP", 5000);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        // 将配置信息（通常是 JSON 或 YAML 格式）反序列化为 RouteDefinition 对象的集合
        List<RouteDefinition> routeDefinitions = JSON.parseArray(configInfo, RouteDefinition.class);
        return Flux.fromIterable(routeDefinitions);
    }

    /**
     * 以下方法用于在gateway端动态添加和删除路由，根据你的需求实现 如提供给actuator端点动态修改用的
     * 目前不实现 因为路由的操作都是在外部系统进行的
     */
    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {

        return Mono.empty();
    }

    /**
     * 需要提供给actuator在去实现 端点动态修改用的
     *
     * @param routeId
     * @return
     */
    @Override
    public Mono<Void> delete(Mono<String> routeId) {

        return Mono.empty();
    }
}