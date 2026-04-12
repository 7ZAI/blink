package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.SameValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @Author binblink
 * @Date 2025/8/25
 */
@Slf4j
public class SameValueValidator implements ConstraintValidator<SameValue, Object> {

    private String[] fields;

    /**
     * 初始化
     *
     * @param constraintAnnotation annotation instance for a given constraint declaration
     */
    @Override
    public void initialize(SameValue constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.fields = constraintAnnotation.fields();
    }

    /**
     * 校验值
     *
     * @param value   object to validate 注解下的对象 这里是参数对象
     * @param context context in which the constraint is evaluated
     * @return
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            //BeanWrapperImpl 提供通过字段名访问对象字段的能力
            BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
            Object originVal = wrapper.getPropertyValue(fields[0]);

            // 第一个字段为null时，检查其他字段是否也为null
            // 如果所有字段均为null，返回true（由@NotNull处理null检查）
            // 如果部分字段为null，返回false（值不一致）
            if (originVal == null) {
                for (String field : fields) {
                    if (wrapper.getPropertyValue(field) != null) {
                        return false;
                    }
                }
                // 所有字段均为null，返回true
                return true;
            }

            for (String field : fields) {
                Object temp = wrapper.getPropertyValue(field);
                //originVal不为空 temp 空 失败
                if (temp == null) {
                    return false;
                }
                //到这 两者均不为空 originVal不会触发空指针
                if (!originVal.equals(temp)) {
                    return false;
                }
            }
            //全部相等
            return true;
        } catch (Exception e) {
            log.error("[SameValueValidator] 校验出现异常 | error: {}", e.getMessage(), e);
            return false;
        }
    }
}
