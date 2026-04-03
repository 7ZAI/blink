package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.MutuallyExclusive;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class MutuallyExclusiveValidator implements ConstraintValidator<MutuallyExclusive, Object> {

    private String field1;

    private String field2;

    @Override
    public void initialize(MutuallyExclusive constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        field1 = constraintAnnotation.field1();
        field2 = constraintAnnotation.field2();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
            Object value1 = wrapper.getPropertyValue(field1);
            Object value2 = wrapper.getPropertyValue(field2);

            // 两个字段不能同时有值
            return (value1 == null || (value1 instanceof String && ((String) value1).isEmpty())) ||
                    (value2 == null || (value2 instanceof String && ((String) value2).isEmpty()));
        } catch (Exception e) {
            log.error("MutuallyExclusiveValidator校验出现异常{}",e.getMessage(),e);
            return false;
        }
    }
}

