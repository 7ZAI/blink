package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据范围实体注解
 * 用于标记实体类，表示该实体类可以在数据过滤规则配置时作为下拉选项
 *
 * @author binblink
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeEntity {

    /**
     * 实体类中文名称
     *
     * @return 中文名称
     */
    String name();

    /**
     * 实体类英文名称（可选，默认使用类简单名称）
     *
     * @return 英文名称
     */
    String enName() default "";
}