package com.blink.framework.mq;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.entity.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;



@Component
public class ListenerTest {

    private static final Logger logger = LoggerFactory.getLogger(ListenerTest.class);


    @RabbitListener(queues = MqConstant.DEFAULT_QUEUE)
    public void  test1(MqGenericDTO<Person> dto)  {

        logger.info("test1 consume data" + Thread.currentThread().getName() + dto.toString());

        logger.info("test1 bussiness data " +  dto.getBody().toString());

//        int a = 1/0;

//        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

//    @RabbitListener(queues = MqConstant.DEFAULT_QUEUE,containerFactory = "customListenerContainer" )
//    public void test2(Message message, Channel channel) throws IOException {
//
//        System.out.println("test2 consume data" + new String(message.getBody()));
//
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//
//    }

}
