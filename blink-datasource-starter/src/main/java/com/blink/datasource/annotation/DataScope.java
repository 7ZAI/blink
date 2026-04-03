package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据范围权限注解
 * 用于标记Mapper方法，控制是否启用数据过滤
 *
 * @author binblink
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 是否启用数据过滤，默认启用
     *
     * @return true启用过滤，false跳过过滤
     */
    boolean enabled() default true;

    /**
     * 指定实体类（用于明确指定，不解析SQL）
     * 主要用于JOIN查询场景
     *
     * @return 实体类Class
     */
    Class<?> entity() default Void.class;

    /**
     * 指定表别名（用于JOIN场景）
     *
     * @return 表别名
     */
    String tableAlias() default "";
}