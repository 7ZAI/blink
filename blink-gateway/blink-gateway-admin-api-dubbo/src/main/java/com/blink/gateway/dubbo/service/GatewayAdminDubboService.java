package com.blink.gateway.dubbo.service;


import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.req.QueryChannelConfigReq;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dto.rsp.MessageAckRsp;

import java.util.concurrent.CompletableFuture;

/**
 * Dubbo 服务接口
 * 服务间调用
 * <p>
 * 提供同步和异步两种调用方式：
 * - 同步方法：直接返回 ResponseDTO
 * - 异步方法（Async后缀）：返回 CompletableFuture，适合响应式场景
 * </p>
 *
 * @author binblink
 */
public interface GatewayAdminDubboService {

    // ==================== 同步方法 ====================

    /**
     * 根据appkey值获取单个渠道信息
     * @param reqDto 请求参数
     * @return ResponseDTO<ChannelInfoRedisDO> 渠道信息
     */
    ResponseDTO<ChannelInfoRedisDO> getChannelInfo(RequestDTO<QueryOneChannelReq> reqDto);

    /**
     * 根据配置key值获取单个配置参数信息
     * @param reqDto 请求参数
     * @return ResponseDTO<SysConfigCacheDO> 配置信息
     */
    ResponseDTO<SysConfigCacheDO> getChannelConfig(RequestDTO<QueryChannelConfigReq> reqDto);

    // ==================== 异步方法（原生 CompletableFuture）====================

    /**
     * 根据appkey值获取单个渠道信息（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> 渠道信息
     */
    CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> getChannelInfoAsync(RequestDTO<QueryOneChannelReq> reqDto);

    /**
     * 根据配置key值获取单个配置参数信息（异步）
     * @param reqDto 请求参数
     * @return CompletableFuture<ResponseDTO<SysConfigCacheDO>> 配置信息
     */
    CompletableFuture<ResponseDTO<SysConfigCacheDO>> getChannelConfigAsync(RequestDTO<QueryChannelConfigReq> reqDto);

    // ==================== 消息 ACK 确认 ====================

    /**
     * 消息消费 ACK 确认（异步）
     * <p>
     * gateway-reactive 消费消息后调用此方法反馈消费结果，
     * gateway-admin 更新 redis_mq 表的消息状态。
     * </p>
     *
     * @param reqDto ACK 请求参数，包含 streamId、msgId、success、errorMsg
     * @return CompletableFuture<ResponseDTO<MessageAckRsp>> 确认结果
     */
    CompletableFuture<ResponseDTO<MessageAckRsp>> ackMessageAsync(RequestDTO<MessageAckReq> reqDto);

}