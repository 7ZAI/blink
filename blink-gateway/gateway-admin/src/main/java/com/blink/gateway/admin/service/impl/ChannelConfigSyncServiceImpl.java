package com.blink.gateway.admin.service.impl;

import com.blink.framework.redis.entity.MessageType;
import com.blink.framework.redis.mq.RedisStreamProducer;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.admin.component.ChannelConfigComponent;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.ChannelConfigSyncService;
import com.blink.gateway.dto.ChannelNacosRefreshMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;

/**
 * 渠道配置异步同步服务实现类
 * 使用 IO 线程池异步执行 Nacos 配置推送和消息通知
 *
 * @author binblink
 */
@Service
@Slf4j
public class ChannelConfigSyncServiceImpl implements ChannelConfigSyncService {

    @Resource
    private ChannelConfigComponent channelConfigComponent;

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Async("ioIntensiveThreadPool")
    @Override
    public void addChannelConfigAsync(GaChannelDO channelDO, Integer operatorUser, String operatorName) {
        try {
            // 1. 添加渠道配置到 Nacos
            channelConfigComponent.addChannelConfig(channelDO);

            // 2. 发送刷新消息通知网关
            ChannelNacosRefreshMsg refreshMsg = ChannelNacosRefreshMsg.singleRefresh(
                    channelDO.getAppKey(), operatorUser, operatorName
            );
            sendRefreshMessage(refreshMsg);

            log.info("[ChannelConfigSync] 添加渠道配置同步成功 | appKey: {}", channelDO.getAppKey());
        } catch (Exception e) {
            log.error("[ChannelConfigSync] 添加渠道配置同步失败 | appKey: {}, error: {}",
                    channelDO.getAppKey(), e.getMessage(), e);
        }
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void modifyChannelConfigAsync(GaChannelDO channelDO, Integer operatorUser, String operatorName) {
        try {
            // 1. 修改渠道配置到 Nacos
            channelConfigComponent.modifyChannelConfig(channelDO);

            // 2. 发送刷新消息通知网关
            ChannelNacosRefreshMsg refreshMsg = ChannelNacosRefreshMsg.singleRefresh(
                    channelDO.getAppKey(), operatorUser, operatorName
            );
            sendRefreshMessage(refreshMsg);

            log.info("[ChannelConfigSync] 修改渠道配置同步成功 | appKey: {}", channelDO.getAppKey());
        } catch (Exception e) {
            log.error("[ChannelConfigSync] 修改渠道配置同步失败 | appKey: {}, error: {}",
                    channelDO.getAppKey(), e.getMessage(), e);
        }
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void deleteChannelConfigAsync(String appKey, Integer operatorUser, String operatorName) {
        try {
            // 1. 删除 Nacos 配置
            channelConfigComponent.deleteChannelConfig(appKey);

            // 2. 发送删除消息通知网关
            ChannelNacosRefreshMsg refreshMsg = ChannelNacosRefreshMsg.deleteRefresh(
                    appKey, operatorUser, operatorName
            );
            sendRefreshMessage(refreshMsg);

            log.info("[ChannelConfigSync] 删除渠道配置同步成功 | appKey: {}", appKey);
        } catch (Exception e) {
            log.error("[ChannelConfigSync] 删除渠道配置同步失败 | appKey: {}, error: {}",
                    appKey, e.getMessage(), e);
        }
    }

    /**
     * 发送刷新消息到 Redis Stream
     *
     * @param refreshMsg 刷新消息
     */
    private void sendRefreshMessage(ChannelNacosRefreshMsg refreshMsg) {
        try {
            StreamMessage<ChannelNacosRefreshMsg> msg = StreamMessage.of(
                    GATEWAY_STREAM_EVENT, MessageType.EVENT, refreshMsg
            );
            msg.setPayloadClass(ChannelNacosRefreshMsg.class.getName());
            gateWayStreamMessageProducer.sendMessageWithRetry(msg, new RedisStreamProducer.Retry(0, 0, 3));

            log.info("[ChannelConfigSync] 发送刷新消息成功 | type: {}, appKey: {}",
                    refreshMsg.getRefreshType(), refreshMsg.getAppKey());
        } catch (Exception e) {
            log.error("[ChannelConfigSync] 发送刷新消息失败 | error: {}", e.getMessage(), e);
        }
    }
}
