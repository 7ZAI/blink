package com.blink.redis;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author binblink
 * @Date 2026/1/7
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger sequence = new AtomicInteger(0);
    private final String prefix;

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        int seq = this.sequence.getAndIncrement();
        thread.setName(this.prefix + (seq >= 1 ? "-" + seq : ""));
        thread.setDaemon(true);
        return thread;
    }
}
