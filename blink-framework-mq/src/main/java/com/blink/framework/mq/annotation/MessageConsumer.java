package com.blink.framework.mq.annotation;




import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * mq消费者注解 作为切入点 push模式消费用不到 已有@RabbitListener
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageConsumer {

    /**
     * 队列名
     * @return
     */
    String queue() default "";

    /**
     * 实体类 DTO
     * @return
     */
    Class  clazz();
}
