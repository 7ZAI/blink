package com.blink.gateway.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.util.JacksonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NacosRouteDefinitionRepository implements RouteDefinitionRepository {

    private final NacosConfigManager nacosConfigManager;

    @Resource
    private BlinkGatewayProperties properties;

    public NacosRouteDefinitionRepository(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {

        return Flux.defer(() -> {
            String configInfo = null;
            String dataId = properties.getDynamicroute().getNacos().getDataId();
            String group = properties.getDynamicroute().getNacos().getGroup();
            try {
                configInfo = nacosConfigManager.getConfigService().getConfig(dataId, group, 5000);
            } catch (NacosException e) {
                return Flux.error(e);
            }
            log.debug("gateway-routes config:{}", configInfo);

            List<RouteDefinition> routeDefinitions =  JacksonUtil.fromJsonToList(configInfo, RouteDefinition.class);
            return Flux.fromIterable(routeDefinitions);
        }).onErrorResume(ex -> {
            log.error(" 从配置中心获取路由失败 get gateway-routes error", ex);
            return Flux.error(ex);
        });
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