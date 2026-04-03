package com.blink.framework;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.annotation.MessageProducer;
import com.blink.framework.mq.constant.MqConstant;
import org.springframework.stereotype.Component;

/**
 * @author binblink
 */
@Component
public class MessageProdutor {


    @MessageProducer(exchange = MqConstant.DEFAULT_EXCHANGE_NAME, key =MqConstant.DEFAULT_ROUTING_KEY)
    public MqGenericDTO<Person> sendPerson(Person person){

        MqGenericDTO<Person> mqDto = new MqGenericDTO();

        mqDto.setProducerBean(person.getClass().getName());
        mqDto.setMsgId("64564564");
        mqDto.setBody(person);
        return mqDto;

    }

    @MessageProducer(exchange = MqConstant.DEFAULT_EXCHANGE_NAME, key ="default")
    public MqGenericDTO<Person> sendPerson2(Person person){

        MqGenericDTO<Person> mqDto = new MqGenericDTO();

        mqDto.setProducerBean(person.getClass().getName());
        mqDto.setMsgId("56456");
        mqDto.setBody(person);
        return mqDto;

    }

}
