package com.blink.framework.mq.aop;


import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.annotation.MessageProducer;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.service.MqMsgSendService;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class SendMqMessageAspect {

    private static final Logger logger = LoggerFactory.getLogger(SendMqMessageAspect.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MqMsgSendService mqMsgSendService;



    @Around("@annotation(producer)")
    public void sendMessage(ProceedingJoinPoint pjp, MessageProducer producer){

        String key = producer.key();
        String exchange = producer.exchange();
        Boolean saveOrNot = producer.enableSaveMsg();
        String msgId = "";
        try {

            MqGenericDTO mqDto = (MqGenericDTO) pjp.proceed();

            msgId = mqDto.getMsgId();
            mqDto.setMqExchange(exchange);
            mqDto.setMqRoutingKey(key);
            mqDto.setEnableRetry(MqConstant.MQ_ENABLE_RETRY);

            logger.info("发送MQ消息请求参数：{}", mqDto);

            if(saveOrNot){
                mqMsgSendService.insertMqMsgWhenSend(mqDto);
            }
            //回调参数 保存消息id 用于回调函数 更新数据库记录状态
            CorrelationData correlationData = new CorrelationData(msgId);


            // 这样是给每个消息都配置了 回调confirm callback 对比在rabbitTemplate中配置更加灵活 排除了rabbitTemplate的限制
            // 可以做到存在多 rabbitTemplate实例时 全局统一配置生产者确认机制
            correlationData.getFuture().thenAccept(

                result -> {
                    try {
                        if(result.isAck()){

                            logger.info("消息投递至交换机 成功！，消息id {}",correlationData.getId());
                            mqMsgSendService.updateMqMsgSts(correlationData.getId(),MqConstant.MQ_STS_SUCCESS);

                        }else{

                            logger.info("消息投递至交换机 失败！！，消息id {}",correlationData.getId());
                            mqMsgSendService.updateMqMsgSts(correlationData.getId(), MqConstant.MQ_STS_FAIL);
                        }
                    } catch (Exception e) {
                        logger.error("消息投递更新数据库异常 {}",e);
                        throw new RuntimeException(e);
                    }
                }
            ).exceptionally(ex -> {
                logger.error("mq Exception: " + ex.getMessage());
                // 因为thenAccept返回的是CompletableFuture<Void>，所以这里必须返回null
                return null;
            });;

            rabbitTemplate.convertAndSend(exchange,key,mqDto,correlationData);

        } catch (Throwable e) {

            logger.info("mq send message fail! id:{}",msgId);

            logger.error("mq send message error!",e);

            throw new RuntimeException(e);
        }

    }


}
