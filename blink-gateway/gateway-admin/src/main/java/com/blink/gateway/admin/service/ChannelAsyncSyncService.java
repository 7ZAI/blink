package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ChannelInfoRedisDO;

/**
 * 渠道异步同步服务接口
 * 用于渠道信息变更后异步同步到网关
 * 通过 CacheMsg 的 operator 字段区分操作类型：A(新增)/M(修改)/D(删除)
 *
 * @author binblink
 */
public interface ChannelAsyncSyncService {

    /**
     * 异步同步新增渠道数据到网关
     * 发送 CacheMsg(operator="A") 实现新增缓存
     *
     * @param appKey       渠道 appKey，作为缓存 key 的一部分
     * @param channelInfo  渠道信息数据
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncAddChannel(String appKey, ChannelInfoRedisDO channelInfo, Integer operatorUser, String operatorName);

    /**
     * 异步同步修改渠道数据到网关
     * 发送 CacheMsg(operator="M") 实现直接更新缓存，而非删除重建
     *
     * @param appKey       渠道 appKey，作为缓存 key 的一部分
     * @param channelInfo  渠道信息数据
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncModifyChannel(String appKey, ChannelInfoRedisDO channelInfo, Integer operatorUser, String operatorName);

    /**
     * 异步同步删除渠道数据到网关
     * 发送 CacheMsg(operator="D") 实现删除缓存
     *
     * @param appKey       渠道 appKey，作为缓存 key 的一部分
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncDeleteChannel(String appKey, Integer operatorUser, String operatorName);
}