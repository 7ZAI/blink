package com.blink.framework.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * redis缓存 更新注解 只适用于key - value的redis的数据类型
 * 其他map list set 不适用
 *
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheDoubleDelete {

    /**
     * 缓存key前缀 必填
     *
     * @return
     */
    String keyPrefix();

    /**
     * 参数中实际key字段 选填
     *
     * @return
     */
    String fieldName() default "";

    /**
     * 第二次删除 延迟时间 毫秒
     *
     * @return
     */
    long delayTime() default 0;
}
