package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.ConditionalRequired;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class ConditionalRequiredValidator implements ConstraintValidator<ConditionalRequired,Object> {

    private String conditionField;
    private String conditionValue;
    private String requiredField;

    @Override
    public void initialize(ConditionalRequired constraintAnnotation) {
        conditionField = constraintAnnotation.conditionField();
        conditionValue = constraintAnnotation.conditionValue();
        requiredField = constraintAnnotation.requiredField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
            Object conditionFieldValue = wrapper.getPropertyValue(conditionField);
            Object requiredFieldValue = wrapper.getPropertyValue(requiredField);

            // 如果条件字段值匹配，则检查必填字段是否有值
            if (conditionValue.equals(conditionFieldValue != null ? conditionFieldValue.toString() : null)) {
                return requiredFieldValue != null &&
                        (!(requiredFieldValue instanceof String) ||
                                !((String) requiredFieldValue).trim().isEmpty());
            }
            return true;
        } catch (Exception e) {
            log.error("条件校验出现异常{}",e.getMessage(),e);
            return false;
        }
    }
}
