package com.blink.gateway.component;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * gateway 配置启动或关闭需要执行的程序
 *
 * @Author binblink
 */
//@Component
public class BlinkGatewayApplicationRunner implements ApplicationRunner, DisposableBean {

//    @Autowired
    private StreamMessageListenerContainer container;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        container.start();
    }


    @Override
    public void destroy() throws Exception {
        container.stop();
    }
}
