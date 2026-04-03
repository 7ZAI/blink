package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.IsNoNegativeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数字非负校验
 * @author binblink
 */
@Documented
@Constraint(validatedBy = { IsNoNegativeValidator.class})
@Target({ FIELD })
@Retention(RUNTIME)
public @interface NonNegative {

    String message() default "该数字不能为负";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 可选：是否包含零，默认为true（即允许零）
    boolean includeZero() default true;
}
