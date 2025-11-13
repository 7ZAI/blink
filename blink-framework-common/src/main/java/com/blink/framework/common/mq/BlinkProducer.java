package com.blink.framework.common.mq;

import com.blink.framework.common.exception.BlinkException;

/**
 * 生产者顶级抽象
 *
 * @author binblink
 */
public interface BlinkProducer<T extends BlinkMessage,R> {

    /**
     * 制作消息
     *
     * @throws BlinkException
     */
    R sendMessage(T message);
}
