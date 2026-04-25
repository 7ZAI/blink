package com.blink.gateway.admin.service;

import com.blink.gateway.admin.entity.GaChannelDO;

/**
 * 渠道配置异步同步服务接口
 * 负责异步同步渠道配置到 Nacos 并发送刷新通知
 *
 * @author binblink
 */
public interface ChannelConfigSyncService {

    /**
     * 异步添加渠道配置
     *
     * @param channelDO 渠道信息
     * @param operatorUser 操作人ID
     * @param operatorName 操作人名称
     */
    void addChannelConfigAsync(GaChannelDO channelDO, Integer operatorUser, String operatorName);

    /**
     * 异步修改渠道配置
     *
     * @param channelDO 渠道信息
     * @param operatorUser 操作人ID
     * @param operatorName 操作人名称
     */
    void modifyChannelConfigAsync(GaChannelDO channelDO, Integer operatorUser, String operatorName);

    /**
     * 异步删除渠道配置
     *
     * @param appKey 渠道标识
     * @param operatorUser 操作人ID
     * @param operatorName 操作人名称
     */
    void deleteChannelConfigAsync(String appKey, Integer operatorUser, String operatorName);
}
