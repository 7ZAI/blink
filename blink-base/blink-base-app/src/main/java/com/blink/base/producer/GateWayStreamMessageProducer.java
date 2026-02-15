package com.blink.base.producer;

import cn.hutool.core.bean.BeanUtil;

import com.blink.base.dto.CacheMsg;
import com.blink.base.dto.RouteSyncMsg;
import com.blink.base.entity.RedisMqDO;
import com.blink.base.mapper.RedisMqMapper;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.entity.MessageType;
import com.blink.framework.redis.mq.RedisStreamProducer;
import com.blink.framework.redis.mq.StreamMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.blink.base.constans.CommonConstans.*;
import static com.blink.base.constans.RedisKeyConstans.*;


/**
 * 事件发布生产者
 * gateway缓存、路由同步
 * 它们公用一个stream 相当于事件总线
 * @Author binblink
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class GateWayStreamMessageProducer extends RedisStreamProducer {


    @Resource
    private RedisMqMapper redisMqMapper;


    @Value("${spring.application.name}")
    private String appName;

    public GateWayStreamMessageProducer(RedisClient redisClient) {
        super(redisClient);
    }

    /**
     * 数据写操作后 发送同步事件 删除数据缓存
     * 缓存同步
     * @param cacheKey
     */
    public void cacheOnChange(String cacheKey) {
        CacheMsg cacheMsg = new CacheMsg();
        cacheMsg.setKey(cacheKey);
        //发送通知同步
        StreamMessage<CacheMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, cacheMsg);
        msg.setSender(appName);
        msg.setPayloadClass(CacheMsg.class.getName());
        //发送并记录
        sendAndRecord(msg, cacheMsg);
    }

    /**
     * 路由同步
     * @param dynamicRouteKey
     */
    public void routesOnChange(String dynamicRouteKey) {

        RouteSyncMsg routeSyncMsgDTO = new RouteSyncMsg();
        routeSyncMsgDTO.setDynamicRouteKey(dynamicRouteKey);
        //发送消息通知同步
        StreamMessage<RouteSyncMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT,MessageType.EVENT, routeSyncMsgDTO);
        msg.setSender(appName);
        msg.setPayloadClass(RouteSyncMsg.class.getName());
        //发送并记录
        sendAndRecord(msg, routeSyncMsgDTO);

    }

    /**
     * 发送并记录
     *
     * @param msg 消息
     * @param t 消息 payload 对象
     * @param <T> payload类型
     */
    private <T> void sendAndRecord(StreamMessage<T> msg,T t){

        RedisMqDO redisMqDO = new RedisMqDO();
        BeanUtil.copyProperties(msg, redisMqDO);

        redisMqDO.setPayload(JacksonUtil.toJson(t));
        redisMqMapper.insert(redisMqDO);

        //带重试机制的发送消息
        Retry retry = new Retry(0,0,3);
        String streamId = sendMessageWithRetry(msg,retry);

        redisMqDO.setFailTimes(retry.getFailTimes());
        redisMqDO.setRetryTimes(retry.getTryTimes());

        //失败
        if(Objects.isNull(streamId)){
            log.error("响Gateway发送Redis Stream 消息 失败！messageInfo:{}",msg);
            redisMqDO.setMsgStatus(REDIS_MSG_STATUS_SEND_FAILED);
        }
        //消息id
        redisMqDO.setStreamId(streamId);
        redisMqMapper.updateById(redisMqDO);
    }



}
