package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.StartEndDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;

/**
 * 验证开始日期是否早于结束日期
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class DateRangeValidator implements ConstraintValidator<StartEndDate, Object> {
    private String startDateField;
    private String endDateField;

    @Override
    public void initialize(StartEndDate constraintAnnotation) {
        startDateField = constraintAnnotation.startDate();
        endDateField = constraintAnnotation.endDate();
    }


    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
            LocalDate startDate = (LocalDate) wrapper.getPropertyValue(startDateField);
            LocalDate endDate = (LocalDate) wrapper.getPropertyValue(endDateField);
            // 由 @NotNull 处理空值
            if (startDate == null || endDate == null) {
                return true;
            }
            return !startDate.isAfter(endDate);
        } catch (Exception e) {
            log.error("日期校验出现异常{}",e.getMessage(),e);
            return false;
        }
    }
}