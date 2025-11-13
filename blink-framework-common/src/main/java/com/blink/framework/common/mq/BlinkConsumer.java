package com.blink.framework.common.mq;

import com.blink.framework.common.exception.BlinkException;

/**
 *消费者顶级抽象
 *
 * @author binblink
 */
public interface BlinkConsumer<T extends BlinkMessage,R> {

    /**
     * 消费消息
     *
     * @param message 消息
     * @throws BlinkException
     */
     R receiveMessage(T message);
}
