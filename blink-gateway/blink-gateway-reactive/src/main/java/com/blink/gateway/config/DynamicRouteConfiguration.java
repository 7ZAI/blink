package com.blink.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.config.prop.BlinkGatewayProperties.DynamicRoute;
import com.blink.gateway.listener.DynamicRoutePropertiesListener;
import com.blink.gateway.route.NacosRouteDefinitionRepository;
import com.blink.gateway.route.RedisRouteDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.blink.gateway.constant.RouteRepositoryBeanNames.*;

/**
 * 动态路由配置
 * 根据配置文件在启动时初始化路由仓库，支持运行时动态切换
 *
 * @author binblink
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BlinkGatewayProperties.class)
public class DynamicRouteConfiguration {

    /**
     * 应用启动后根据配置初始化路由仓库
     */
    @Bean
    public CommandLineRunner initRouteRepository(ConfigurableApplicationContext context,
                                                  BlinkGatewayProperties properties,
                                                  NacosConfigManager nacosConfigManager,
                                                  ReactiveRedisClient redisClient,
                                                  ApplicationEventPublisher eventPublisher) {
        return args -> {
            DynamicRoute dynamicRoute = properties.getDynamicroute();
            if (dynamicRoute == null) {
                log.warn("[DynamicRoute] 未配置动态路由，跳过初始化");
                return;
            }

            String mode = dynamicRoute.getMode();
            log.info("[DynamicRoute] 初始化路由仓库，模式: {}", mode);

            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();

            if ("nacos".equals(mode)) {
                registerNacosRepository(beanFactory, nacosConfigManager);
                registerNacosListener(beanFactory, nacosConfigManager, eventPublisher, dynamicRoute.getNacos());
            } else if ("redis".equals(mode)) {
                registerRedisRepository(beanFactory, dynamicRoute.getRedis(), redisClient);
            }
        };
    }

    private void registerNacosRepository(DefaultListableBeanFactory beanFactory,
                                         NacosConfigManager nacosConfigManager) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NacosRouteDefinitionRepository.class);
        builder.addConstructorArgValue(nacosConfigManager);

        beanFactory.registerBeanDefinition(NACOS_REPOSITORY, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 启动注册 Bean: {}", NACOS_REPOSITORY);
    }

    private void registerNacosListener(DefaultListableBeanFactory beanFactory,
                                       NacosConfigManager nacosConfigManager,
                                       ApplicationEventPublisher eventPublisher,
                                       DynamicRoute.Nacos nacosConfig) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DynamicRoutePropertiesListener.class);
        builder.addConstructorArgValue(nacosConfigManager);
        builder.addConstructorArgValue(eventPublisher);
        builder.addConstructorArgValue(nacosConfig);

        beanFactory.registerBeanDefinition(NACOS_LISTENER, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 启动注册 Bean: {}", NACOS_LISTENER);
    }

    private void registerRedisRepository(DefaultListableBeanFactory beanFactory,
                                         DynamicRoute.Redis redisConfig,
                                         ReactiveRedisClient redisClient) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisRouteDefinitionRepository.class);
        builder.addConstructorArgValue(redisConfig);
        builder.addConstructorArgValue(redisClient);

        beanFactory.registerBeanDefinition(REDIS_REPOSITORY, builder.getBeanDefinition());
        log.debug("[DynamicRoute] 启动注册 Bean: {}", REDIS_REPOSITORY);
    }
}
