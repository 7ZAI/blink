package com.blink.framework.mq.componet;

import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.service.MqMsgSendService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Objects;

/**
 * 消息投递到交换机 回调确认
 */
//@Component
public class MessageConfirmReturn implements RabbitTemplate.ConfirmCallback {

    private static final Logger logger = LoggerFactory.getLogger(MessageConfirmReturn.class);

    @Resource
    private MqMsgSendService mqMsgSendService;


    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

        String msgId = "";

        if(Objects.nonNull(correlationData)){
            msgId = correlationData.getId();
        }
        try {
            if(ack){
                logger.info("消息成功投递到交换机 消息id:{}",msgId);
                //不做记录的mq消息
                if(msgId.length()>0){
                    mqMsgSendService.updateMqMsgSts(msgId,MqConstant.MQ_STS_SUCCESS);
                }
            }

            if(!ack) {
                logger.info("消息投递到交换机失败 消息id:{},失败信息:{}",msgId,cause);
                //不做记录的mq消息
                if(msgId.length()>0){
                    mqMsgSendService.updateMqMsgSts(msgId,MqConstant.MQ_STS_FAIL);
                }
            }
        } catch (Exception e) {
            logger.error("消息状态更新失败！ 消息id:{},失败信息:{}",msgId,e);
            throw new RuntimeException(e);
        }

    }
}
