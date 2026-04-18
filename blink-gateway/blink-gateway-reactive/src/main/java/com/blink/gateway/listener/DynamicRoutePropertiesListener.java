package com.blink.gateway.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.blink.gateway.config.prop.BlinkGatewayProperties.DynamicRoute.Nacos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.Executor;

/**
 * 动态路由 Nacos 配置监听器
 * 监听 Nacos 配置中心路由配置文件变更，同步刷新网关路由
 *
 * @author binblink
 */
@Slf4j
public class DynamicRoutePropertiesListener implements ApplicationRunner {

    private final NacosConfigManager nacosConfigManager;
    private final ApplicationEventPublisher publisher;
    private final Nacos nacosProperties;

    public DynamicRoutePropertiesListener(NacosConfigManager nacosConfigManager,
                                           ApplicationEventPublisher publisher,
                                           Nacos nacosProperties) {
        this.nacosConfigManager = nacosConfigManager;
        this.publisher = publisher;
        this.nacosProperties = nacosProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String dataId = nacosProperties.getDataId();
        String group = nacosProperties.getGroup();

        log.info("[DynamicRoute] 注册 Nacos 配置监听器 | dataId: {}, group: {}", dataId, group);

        // 添加监听器，监听后续配置变化
        nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("[DynamicRoute] 收到路由配置变更通知");
                // 发布路由刷新事件，通知 Gateway 更新生效
                publisher.publishEvent(new RefreshRoutesEvent(this));
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        });
    }
}