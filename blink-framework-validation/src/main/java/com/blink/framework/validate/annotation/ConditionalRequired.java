package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.ConditionalRequiredValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件必填验证
 * 当一个字段为特定值时，另一个字段必须填写
 * @author binblink
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalRequiredValidator.class)
public @interface ConditionalRequired {

    String message() default "当{conditionField}为{conditionValue}时，{requiredField}必须填写";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String conditionField();

    String conditionValue();

    String requiredField();
}
