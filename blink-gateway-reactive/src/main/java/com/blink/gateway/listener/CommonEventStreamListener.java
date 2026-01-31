package com.blink.gateway.listener;

import com.blink.base.dto.CacheMsgDTO;
import com.blink.base.dto.RouteSyncMsgDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.component.MultiLevelCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayProperties;
import com.blink.gateway.event.EnableStreamEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private Flux<StreamMsgRecord> commonEventFlux;

    private Disposable disposable;

    private static Boolean initialized = false;

    @PostConstruct
    public void startCheck() {
        // 默认关闭监听stream缓存同步消息
        if (properties.getEventStreamEnable()) {
            this.start();
        }
        CommonEventStreamListener.initialized = true;
    }


    private void start() {

        log.info("<=== 开启 redis stream 连接，进行消息监听与消费");
        //开启 stream 消费
        this.disposable = commonEventFlux.subscribe();

    }

    private void stop() {
        log.info("===> 关闭 redis stream 连接 ！终止stream消息消费");
        if (Objects.nonNull(this.disposable)) {
            this.disposable.dispose();
        }
    }


    /**
     * 监听配置项变动事件 运行时动态开关 redis stream消费
     *
     * @param event
     */
    @EventListener
    public void handleConfigChange(EnableStreamEvent event) {

        log.info("eventStreamEnable 配置项变动 值：{}", event.getNewValue());

        if (!initialized) {
            log.debug("应用启动 首次变动 不做任何处理！");
            return;
        }
        if (event.getNewValue()) {
            start();
        } else {
            stop();
        }

    }



}
