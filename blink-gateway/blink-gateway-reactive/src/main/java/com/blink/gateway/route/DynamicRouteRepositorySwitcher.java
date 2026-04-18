package com.blink.gateway.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.config.prop.BlinkGatewayProperties.DynamicRoute;
import com.blink.gateway.event.ChangeType;
import com.blink.gateway.event.DynamicRouteConfigChangedEvent;
import com.blink.gateway.listener.DynamicRoutePropertiesListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.blink.gateway.constant.RouteRepositoryBeanNames.*;

/**
 * 动态路由仓库切换器
 * 监听配置变更事件，执行 Bean 的销毁和重建
 *
 * @author binblink
 */
@Slf4j
@Component
public class DynamicRouteRepositorySwitcher {

    private final ConfigurableApplicationContext context;
    private final ApplicationEventPublisher eventPublisher;
    private final NacosConfigManager nacosConfigManager;
    private final ReactiveRedisClient redisClient;

    public DynamicRouteRepositorySwitcher(ConfigurableApplicationContext context,
                                          ApplicationEventPublisher eventPublisher,
                                          NacosConfigManager nacosConfigManager,
                                          ReactiveRedisClient redisClient) {
        this.context = context;
        this.eventPublisher = eventPublisher;
        this.nacosConfigManager = nacosConfigManager;
        this.redisClient = redisClient;
    }

    @EventListener
    public void onConfigChanged(DynamicRouteConfigChangedEvent event) {
        ChangeType changeType = event.getChangeType();
        DynamicRoute newConfig = event.getNewValue();

        log.info("[DynamicRoute] 检测到配置变更，类型: {}", changeType);

        try {
            switch (changeType) {
                case MODE_SWITCH:
                    handleModeSwitch(event.getOldValue(), newConfig);
                    break;
                case NACOS_CONFIG_CHANGE:
                    handleNacosConfigChange(newConfig);
                    break;
                case REDIS_CONFIG_CHANGE:
                    handleRedisConfigChange(newConfig);
                    break;
            }

            // 刷新路由
            eventPublisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("[DynamicRoute] 路由仓库切换完成，路由已刷新");

        } catch (Exception e) {
            log.error("[DynamicRoute] 路由仓库切换失败", e);
        }
    }

    // ==================== 模式切换处理 ====================

    /**
     * 处理模式切换
     */
    private void handleModeSwitch(DynamicRoute oldConfig, DynamicRoute newConfig) {
        String oldMode = oldConfig.getMode();
        String newMode = newConfig.getMode();

        log.info("[DynamicRoute] 模式切换: {} -> {}", oldMode, newMode);

        // 销毁旧模式的 Bean
        if ("nacos".equals(oldMode)) {
            destroyNacosBeans();
        } else if ("redis".equals(oldMode)) {
            destroyRedisBeans();
        }

        // 创建新模式的 Bean
        if ("nacos".equals(newMode)) {
            createNacosBeans(newConfig);
        } else if ("redis".equals(newMode)) {
            createRedisBeans(newConfig);
        }
    }

    /**
     * 处理 Nacos 配置变化
     */
    private void handleNacosConfigChange(DynamicRoute config) {
        log.info("[DynamicRoute] Nacos 配置变化，重建监听器");

        // 销毁旧的监听器
        destroyBean(NACOS_LISTENER);

        // 创建新的监听器
        registerNacosListener(config.getNacos());
    }

    /**
     * 处理 Redis 配置变化
     */
    private void handleRedisConfigChange(DynamicRoute config) {
        log.info("[DynamicRoute] Redis 配置变化，重建仓库");

        // 销毁旧的仓库
        destroyBean(REDIS_REPOSITORY);

        // 创建新的仓库
        registerRedisRepository(config.getRedis());
    }

    // ==================== Bean 销毁方法 ====================

    private void destroyNacosBeans() {
        destroyBean(NACOS_REPOSITORY);
        destroyBean(NACOS_LISTENER);
    }

    private void destroyRedisBeans() {
        destroyBean(REDIS_REPOSITORY);
    }

    private void destroyBean(String beanName) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        if (beanFactory.containsBeanDefinition(beanName)) {
            log.debug("[DynamicRoute] 销毁 Bean: {}", beanName);
            beanFactory.removeBeanDefinition(beanName);
        }
    }

    // ==================== Bean 注册方法 ====================

    private void createNacosBeans(DynamicRoute config) {
        registerNacosRepository();
        registerNacosListener(config.getNacos());
    }

    private void createRedisBeans(DynamicRoute config) {
        registerRedisRepository(config.getRedis());
    }

    private void registerNacosRepository() {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NacosRouteDefinitionRepository.class);
        builder.addConstructorArgValue(nacosConfigManager);

        beanFactory.registerBeanDefinition(NACOS_REPOSITORY, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 注册 Bean: {}", NACOS_REPOSITORY);
    }

    private void registerNacosListener(DynamicRoute.Nacos nacosConfig) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DynamicRoutePropertiesListener.class);
        builder.addConstructorArgValue(nacosConfigManager);
        builder.addConstructorArgValue(eventPublisher);
        builder.addConstructorArgValue(nacosConfig);

        beanFactory.registerBeanDefinition(NACOS_LISTENER, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 注册 Bean: {}", NACOS_LISTENER);
    }

    private void registerRedisRepository(DynamicRoute.Redis redisConfig) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisRouteDefinitionRepository.class);
        builder.addConstructorArgValue(redisConfig);
        builder.addConstructorArgValue(redisClient);

        beanFactory.registerBeanDefinition(REDIS_REPOSITORY, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 注册 Bean: {}", REDIS_REPOSITORY);
    }
}
