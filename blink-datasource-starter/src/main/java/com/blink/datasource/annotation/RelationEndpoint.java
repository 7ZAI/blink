package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 关联关系端点注解
 * 定义关联表的一个端点实体
 *
 * @author binblink
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationEndpoint {

    /**
     * 端点名称（中文，如"用户"、"角色"）
     */
    String name();

    /**
     * 端点英文名称（可选）
     */
    String enName() default "";

    /**
     * 实体表名（如 sys_user）
     */
    String table();

    /**
     * 实体表关联字段（如 user_id）
     */
    String field();

    /**
     * 关联表中的关联字段（如 user_id）
     * 如果不指定，默认与 field 相同
     */
    String relationField() default "";
}