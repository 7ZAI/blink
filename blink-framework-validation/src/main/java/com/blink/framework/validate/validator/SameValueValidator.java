package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.SameValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @Author binblink
 * @Date 2025/8/25
 */
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
            //按一般情况 配置的字段值不应该为空 如果全部为空默认校验失败 不符合SameValue初衷
            if (originVal == null) {
                return false;
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
            return false;
        }
    }
}
