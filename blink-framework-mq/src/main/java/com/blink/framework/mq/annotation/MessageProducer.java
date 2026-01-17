package com.blink.framework.mq.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义 声明为发送mq方法的 注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageProducer {

    /**
     * 队列名
     * @return
     */
    String queues() default "";

    /**
     * exchange
     * @return
     */
    String exchange();


    /**
     * 路由key
     * @return
     */
    String key();

    /**
     * 是否允许重试
     * @return
     */
    boolean retryEnable() default true;

    /**
     * 是否开启记录mq发送记录
     * @return
     */
    boolean enableSaveMsg() default true;





}
