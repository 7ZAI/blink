package com.blink.gateway.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;

import java.util.Map;

/**
 * 路由同步监听 监听stream消息
 *
 * @Author binblink
 * @Date 2025/11/5
 */
@Slf4j
public class RouteUpdateStreamListener implements StreamListener<String, MapRecord<String,String,Object>> {

    private final ApplicationEventPublisher publisher;

    public RouteUpdateStreamListener(ApplicationEventPublisher publisher){
        this.publisher = publisher;
    }
    /**
     * 路由同步消息监听
     * 消费消息 同步路由
     *
     * @param message never {@literal null}.
     */
    @Override
    public void onMessage(MapRecord message) {


        log.info("接收到来自redis stream的路由同步消息！message: {}",message);

        //发布同步事件
        publisher.publishEvent(new RefreshRoutesEvent(this));
        Map<String,Object> map = (Map<String, Object>) message.getValue();

//        StreamMessage.convertMapToMessage(map,Rout)
    }
}
