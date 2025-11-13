package com.blink.framework.redis.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 同步模式下 预存对象
 */
public class IdCache {

    private final AtomicLong localIdGen;
    private final Long localMaxValue;

    //全局最大值，超过该值继续从1开始;该值为null则无穷自增，达到long最大值
    private final Long maxValue;

    public IdCache(Long localIdGen, Long localMaxValue, Long maxValue) {
        this.localIdGen = new AtomicLong(localIdGen);
        this.localMaxValue = localMaxValue;
        this.maxValue = maxValue;
    }

    public Long nextValue() {

        Long value = localIdGen.incrementAndGet();

        if (value > localMaxValue) {
            return -1L;
        }
        if (-1 != maxValue && value > maxValue) {
            return -2L;
        }
        return value;
    }
}
