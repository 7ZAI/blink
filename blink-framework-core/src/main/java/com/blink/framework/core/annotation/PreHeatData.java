package com.blink.framework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 预加载数据到redis
 * @author binblink
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreHeatData {

    /**
     * 默认开启
     * @return
     */
    boolean enable() default true;

    /**
     * 实际加载缓存的方法
     * @return
     */
    String method() default "";
}
