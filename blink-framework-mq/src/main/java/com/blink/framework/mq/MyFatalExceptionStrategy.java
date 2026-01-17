package com.blink.framework.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

/**
 * 代码来自官方demo 消费端全局异常处理
 */
public class MyFatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {
    private final Logger logger = LoggerFactory.getLogger(MyFatalExceptionStrategy.class);

    @Override
    public boolean isFatal(Throwable t) {
        if (t instanceof ListenerExecutionFailedException lefe) {
            logger.error("Failed to process inbound message from queue "
                    + lefe.getFailedMessage().getMessageProperties().getConsumerQueue()
                    + "; failed message: " + lefe.getFailedMessage(), t);
        }
        return super.isFatal(t);
    }
}
