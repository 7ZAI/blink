package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.FieldConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 字段约束校验注解
 * 根据 sys_field_constraint 表中定义的约束规则进行校验
 *
 * @author binblink
 * @since 2026-03-07
 */
@Documented
@Constraint(validatedBy = { FieldConstraintValidator.class })
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface FieldConstraint {

    /**
     * 约束名称，对应 sys_field_constraint 表中的 constraint_name
     */
    String name();

    String message() default "SYS00001";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}