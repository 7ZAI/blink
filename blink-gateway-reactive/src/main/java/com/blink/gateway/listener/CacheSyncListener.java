package com.blink.gateway.listener;

import com.blink.gateway.config.prop.BlinkGatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @Author binblink
 * @Date 2025/11/11
 */
@Slf4j
@Component
public class CacheSyncListener {

    @Resource
    private BlinkGatewayProperties properties;

    @Resource
    private Flux<MapRecord<String,String,Object>> localCacheFlux;

    private Disposable disposable;

    @PostConstruct
    public void startCheck(){
        //开启本地缓存 默认开启监听stream缓存同步消息
        if(properties.getCache().getLocalCacheEnable()){
            if(properties.getCache().getSyncListenerOpen()){
                this.start();
            }
        }
    }

    // 添加监听器，监听后续配置变化 监听 Nacos 配置中心 路由配置文件修改事件
//        nacosConfigManager.getConfigService().addListener(nacosProperties.getDataId(), nacosProperties.getGroup(), new Listener() {
//        @Override
//        public void receiveConfigInfo(String configInfo) {
//            // 发布路由刷新事件，通知Gateway更新生效 也就是刷新路由 监听器会从仓库中 getRouteDefinitions重新获取路由
//            // 实际执行更新路由的类 CachingRouteLocator 也是运行时的路由来源 CachingRouteDefinitionLocator
//            publisher.publishEvent(new RefreshRoutesEvent(this));
//            // 当Nacos中的配置发生变化时，此方法被回调
////                updateRoutes(configInfo);
//        }
//
//        @Override
//        public Executor getExecutor() {
//            return null;
//        }
//    });

    private void start(){
        this.disposable = localCacheFlux.subscribe();
    }

    private void stop(){
        if(Objects.nonNull(this.disposable)){
            this.disposable.dispose();
        }
    }



}
