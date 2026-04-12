package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.IsDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.function.Supplier;


/**
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class IsDateValidator implements ConstraintValidator<IsDate, Object> {

    private Class<? extends Supplier<LocalDate>> dateSupperClazz;

    @Override
    public void initialize(IsDate constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        dateSupperClazz = constraintAnnotation.getDate();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            if (value == null) {
                return true;
            }
            LocalDate date = (LocalDate) value;
            Supplier<LocalDate> supplier = dateSupperClazz.getDeclaredConstructor().newInstance();
            LocalDate dateValue = supplier.get();

            return date.isEqual(dateValue);

        } catch (NoSuchMethodException e) {
            log.error("[IsDateValidator] Supplier类缺少无参构造函数 | supplierClass: {}", dateSupperClazz.getName(), e);
            return false;
        } catch (ClassCastException e) {
            log.error("[IsDateValidator] 值类型转换失败，期望LocalDate | value type: {}", value.getClass().getName(), e);
            return false;
        } catch (Exception e) {
            log.error("[IsDateValidator] 校验出现异常 | supplierClass: {}, error: {}", dateSupperClazz.getName(), e.getMessage(), e);
            return false;
        }
    }
}
