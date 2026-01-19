package com.blink.framework;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.constant.MqConstant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BlinkFrameworkMqApplication.class)
public class Test {

    @Autowired
    private MqOperatorComponet rabbitMQComponet;

    @Autowired
    private MessageProdutor mqMessageSender;

    @org.junit.jupiter.api.Test
    public void test() throws Exception {


        Person p = new Person();
        p.setAge(18);
        p.setName("Mike");
        p.setMale(1);

        MqGenericDTO<Person> mqGenericDTO = new MqGenericDTO();

        mqGenericDTO.setBody(p);
        mqGenericDTO.setExchange(MqConstant.DEFAULT_EXCHANGE_NAME);
        mqGenericDTO.setRoutingKey(MqConstant.DEFAULT_ROUTING_KEY);
        mqGenericDTO.setMsgId("65777");

        rabbitMQComponet.sendMessage(mqGenericDTO,mqGenericDTO.getRoutingKey(),mqGenericDTO.getExchange());
    }


    @org.junit.jupiter.api.Test
    public void testAop(){

        Person p = new Person();
        p.setAge(30);
        p.setName("Furina");
        p.setMale(0);

        mqMessageSender.sendPerson(p);
    }

}
