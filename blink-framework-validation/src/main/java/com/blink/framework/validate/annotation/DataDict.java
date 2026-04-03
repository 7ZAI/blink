package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.DataDictConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数据字段值长度校验
 * @author binblink
 */
@Documented
@Constraint(validatedBy = { DataDictConstraintValidator.class})
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface DataDict {

    String name();

    String message() default "SYS00001";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
