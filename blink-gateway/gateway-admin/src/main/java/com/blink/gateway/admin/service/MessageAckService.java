package com.blink.gateway.admin.service;

import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.rsp.MessageAckRsp;

/**
 * 消息 ACK 确认服务接口
 *
 * @author binblink
 * @since 2026-04-10
 */
public interface MessageAckService {

    /**
     * 处理消息 ACK 确认
     * <p>
     * 根据 streamId 或 msgId 更新 redis_mq 表的消息状态：
     * - success=true: 更新为已确认消费 (status=3)
     * - success=false: 更新为消费失败 (status=4)，记录错误信息
     * </p>
     *
     * @param req ACK 请求参数
     * @return 确认结果
     */
    MessageAckRsp ackMessage(MessageAckReq req);
}