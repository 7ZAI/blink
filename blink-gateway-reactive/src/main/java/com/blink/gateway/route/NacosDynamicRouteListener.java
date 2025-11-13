package com.blink.gateway.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.blink.gateway.config.prop.GatewayProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.Executor;

/**
 * 动态路由 Nacos 配置中文件实现 作用：外部修改同步路由刷新
 * @author binblink
 */
public class NacosDynamicRouteListener implements ApplicationRunner {

    
    private final NacosConfigManager nacosConfigManager;
    private final ApplicationEventPublisher publisher;
    private final GatewayProperties.Dynamicroute.Nacos nacosProperties;

    public NacosDynamicRouteListener(NacosConfigManager nacosConfigManager,ApplicationEventPublisher publisher,
                                     GatewayProperties.Dynamicroute.Nacos nacosProperties) {
        this.nacosConfigManager = nacosConfigManager;
        this.publisher = publisher;
        this.nacosProperties = nacosProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 添加监听器，监听后续配置变化 监听 Nacos 配置中心 路由配置文件修改事件
        nacosConfigManager.getConfigService().addListener(nacosProperties.getDataId(), nacosProperties.getGroup(), new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 发布路由刷新事件，通知Gateway更新生效 也就是刷新路由 监听器会从仓库中 getRouteDefinitions重新获取路由
                // 实际执行更新路由的类 CachingRouteLocator 也是运行时的路由来源 CachingRouteDefinitionLocator
                publisher.publishEvent(new RefreshRoutesEvent(this));
                // 当Nacos中的配置发生变化时，此方法被回调
//                updateRoutes(configInfo);
            }
            
            @Override
            public Executor getExecutor() {
                return null;
            }
        });
    }

}