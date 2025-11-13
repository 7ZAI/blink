package com.blink.framework.validate.annotation;

import com.blink.framework.validate.validator.IsDateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * 判断当前值是否为指定日期
 * 指定日期可动态指定 场景案例如 交易日期必须在当天进行
 * @author binblink
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsDateValidator.class)
public @interface IsDate {

    String message() default "日期不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    //提供指定日期的函数 这样能够动态指定日期 比如今天
    Class<? extends Supplier<LocalDate>> getDate();
}
