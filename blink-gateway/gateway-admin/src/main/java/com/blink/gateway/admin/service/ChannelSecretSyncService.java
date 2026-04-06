package com.blink.gateway.admin.service;

import com.blink.gateway.admin.entity.GaChannelDO;

/**
 * 渠道密钥配置异步同步服务接口
 * 用于 Nacos 密钥配置的异步推送
 *
 * @author binblink
 */
public interface ChannelSecretSyncService {

    /**
     * 异步添加渠道密钥配置到 Nacos
     *
     * @param channelDO 渠道实体
     */
    void addChannelSecretConfigAsync(GaChannelDO channelDO);

    /**
     * 异步删除渠道密钥配置
     *
     * @param appKey 渠道 appKey
     */
    void deleteChannelSecretConfigAsync(String appKey);
}