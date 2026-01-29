package com.blink.gateway.listener;

import com.blink.gateway.config.prop.BlinkGatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * @Author binblink
 */
@Slf4j
@Component
public class CommonEventStreamListener {

    @Resource
    private BlinkGatewayProperties properties;

    @Resource
    private Flux<MapRecord<String,String,Object>> commonEventFlux;

    private Disposable disposable;

    @PostConstruct
    public void startCheck(){
        // 默认开启监听stream缓存同步消息
        if(properties.getEventStreamEnable()){
            this.start();
        }
    }


    private void start(){
        this.disposable = commonEventFlux.subscribe();
    }

    private void stop(){
        if(Objects.nonNull(this.disposable)){
            this.disposable.dispose();
        }
    }

}
