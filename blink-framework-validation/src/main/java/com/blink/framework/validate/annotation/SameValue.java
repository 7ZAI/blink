package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.SameValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 多个字段 必须有相同的值
 * @author binblink
 */
@Documented
@Constraint(validatedBy = { SameValueValidator.class})
@Target({ TYPE })
@Retention(RUNTIME)
public @interface SameValue {

    String message() default "值不一致";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    //多个值
    String[] fields();

//    Class<? extends UniqueCombinationChecker> checker();
}
