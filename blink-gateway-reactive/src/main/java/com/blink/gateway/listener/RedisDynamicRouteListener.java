package com.blink.gateway.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.connection.stream.MapRecord;
import reactor.core.publisher.Flux;

/**
 * 通过发布刷路由事件刷新路由
 * 因为不管是nacos 修改配置json 还是 通过管理系统修改redis路由 都是在gateway外部系统中完成的
 * 修改后 nacos通过监听修改配置文件事件通知gateway刷新
 * redis 通过订阅管道消息 通知gateway刷新
 * 综上 routeDefinitionRepository 中的 delete save 方法可以空实现
 * @author binblink
 */
public class RedisDynamicRouteListener implements ApplicationEventPublisherAware, ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisDynamicRouteListener.class);

    /**
     * 使用抽象接口对象 可以同时适配 Nacos配置 或者 Redis模式
     */
    private RouteDefinitionRepository routeDefinitionRepository;

    private Flux<MapRecord<String,String,Object>> redisRouteFlux;
    
    private ApplicationEventPublisher publisher;



    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new RefreshRoutesEvent(this));
        routeDefinitionRepository.getRouteDefinitions();
    }

}