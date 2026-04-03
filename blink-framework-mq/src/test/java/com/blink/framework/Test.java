package com.blink.framework;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.constant.MqConstant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BlinkFrameworkMqApplication.class)
public class Test {

    @Autowired
    private MessageProdutor mqMessageSender;


    @org.junit.jupiter.api.Test
    public void testAop(){

        Person p = new Person();
        p.setAge(30);
        p.setName("Furina");
        p.setMale(0);

        mqMessageSender.sendPerson(p);
    }

}
