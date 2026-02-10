package com.blink.framework.validate.validator;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.validate.annotation.NonNegative;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 非负校验 大于等于0
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class IsNoNegativeValidator implements ConstraintValidator<NonNegative, Object> {

    private boolean includeZero;

    @Override
    public void initialize(NonNegative constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
       includeZero = constraintAnnotation.includeZero();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // 如果值为null，由@NotNull或其他注解处理
            if (value == null) {
                return true;
            }
            // 根据不同类型进行比较
            if (value instanceof BigDecimal decimalValue) {
                return includeZero ?
                        decimalValue.compareTo(BigDecimal.ZERO) >= 0 :
                        decimalValue.compareTo(BigDecimal.ZERO) > 0;
            }
            else if (value instanceof BigInteger integerValue) {
                return includeZero ?
                        integerValue.compareTo(BigInteger.ZERO) >= 0 :
                        integerValue.compareTo(BigInteger.ZERO) > 0;
            }
            else if (value instanceof Integer intValue) {
                return includeZero ? intValue >= 0 : intValue > 0;
            }
            else if (value instanceof Long longValue) {
                return includeZero ? longValue >= 0 : longValue > 0;
            }
            else if (value instanceof Double doubleValue) {
                return includeZero ? doubleValue >= 0 : doubleValue > 0;
            }
            else if (value instanceof Float floatValue) {
                return includeZero ? floatValue >= 0 : floatValue > 0;
            }
            else if (value instanceof Short shortValue) {
                return includeZero ? shortValue >= 0 : shortValue > 0;
            }
            else if (value instanceof Byte byteValue) {
                return includeZero ? byteValue >= 0 : byteValue > 0;
            }else {
                // 不支持的类型，默认通过验证（或者可以抛出异常）
                throw new BlinkException("not support the field type ti validate!");
            }

        }catch (Exception e){
            log.error("非负校验 校验出现异常{}",e.getMessage(),e);
            return false;
        }

    }
}
