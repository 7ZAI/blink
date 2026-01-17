package com.blink.framework.mq.componet;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.entity.MqMsgSendDO;
import com.blink.framework.mq.service.MqMsgSendService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 消息投递到达队列的回调 只有到达了交换机未能投递到队列时会回调
 * 一般情况为路由key配置错误
 */
@Component
public class MessageReturnCallBack implements RabbitTemplate.ReturnsCallback {

    private static final Logger logger = LoggerFactory.getLogger(MessageReturnCallBack.class);

    @Resource
    private MqMsgSendService mqMsgSendService;

    @Override
    public void returnedMessage(ReturnedMessage returned) {

        logger.warn("交换机路由消息至队列失败！ 回退的消息 {}",returned.toString());

        Message message =  returned.getMessage();
        String msgId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");

        //未设置id的消息 不做处理
        if(StringUtils.isBlank(msgId)){
            return;
        }

        MqMsgSendDO mqMsgSendDO = mqMsgSendService.getById(msgId);

        //正常情况不会出现 已发送未被记录的消息 但是为了以防万一
        if(Objects.isNull(mqMsgSendDO)){
            logger.error("被回退的消息未被记录至数据库 ！  消息id {}",msgId);
            return;
        }

        mqMsgSendDO.setSendSts(MqConstant.MQ_STS_RETURN);
        mqMsgSendDO.setRemark(returned.getReplyCode() + " " + returned.getReplyText());

        mqMsgSendService.updateById(mqMsgSendDO);


    }
}
