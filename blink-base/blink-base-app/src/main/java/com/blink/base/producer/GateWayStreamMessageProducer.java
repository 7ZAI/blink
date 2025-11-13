package com.blink.base.producer;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.blink.base.dto.CacheMsgDTO;
import com.blink.base.dto.RouteSyncMsgDTO;
import com.blink.base.entity.RedisMqDO;
import com.blink.base.mapper.RedisMqMapper;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.entity.MessageType;
import com.blink.framework.redis.mq.RedisStreamProducer;
import com.blink.framework.redis.mq.StreamMessage;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.blink.base.constans.CommonConstans.*;
import static com.blink.base.constans.RedisKeyConstans.*;


/**
 *
 *
 * @Author binblink
 * @Date 2025/11/5
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class GateWayStreamMessageProducer extends RedisStreamProducer {

    private static final Logger log = LoggerFactory.getLogger(GateWayStreamMessageProducer.class);

    private RedisClient redisClient;

    @Resource
    private RedisMqMapper redisMqMapper;

    @Value("${spring.application.name}")
    private String appName;

    public GateWayStreamMessageProducer(RedisClient redisClient) {
        super(redisClient);
    }


    public void cacheOnChange(String cacheKey) {
        CacheMsgDTO cacheMsgDTO = new CacheMsgDTO();
        cacheMsgDTO.setKey(cacheKey);
        //通知同步
        StreamMessage<CacheMsgDTO> msg = StreamMessage.of("blink:stream:gateway:cache", MessageType.EVENT, cacheMsgDTO)
                .setSender(appName).setPayloadClass(CacheMsgDTO.class.getName());
        //发送并记录
        sendAndRecord(msg, cacheMsgDTO);
    }

    public void routesOnChange(String dynamicRouteKey) {

        RouteSyncMsgDTO routeSyncMsgDTO = new RouteSyncMsgDTO();
        routeSyncMsgDTO.setDynamicRouteKey(dynamicRouteKey);
        //通知同步
        StreamMessage<RouteSyncMsgDTO> msg = StreamMessage.of("blink:stream:gateway:route", MessageType.EVENT, routeSyncMsgDTO)
                .setSender(appName)
                .setPayloadClass(CacheMsgDTO.class.getName());

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

        redisMqDO.setPayload(JSON.toJSONString(t));
        redisMqMapper.insert(redisMqDO);

        //带重试机制的发送消息
        Retry retry = new Retry(0,0,3);
        String streamId = sendMessageWithRetry(msg,retry);

        redisMqDO.setFailTimes(retry.getFailTimes());
        redisMqDO.setRetryTimes(retry.getTryTimes());

        //失败
        if(Objects.isNull(streamId)){
            log.error("给GateWay的同步消息 发送Redis Stream 失败！messageInfo:{}",msg);
            redisMqDO.setMsgStatus(REDIS_MSG_STATUS_SEND_FAILED);
        }
        //消息id
        redisMqDO.setStreamId(streamId);
        redisMqMapper.updateById(redisMqDO);
    }

}
