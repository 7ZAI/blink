package com.blink.framework.validate.annotation;


import com.blink.framework.validate.validator.IPAddressValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * ip地址格式校验
 * 支持ipv4 ipv6
 * 支持单个String 和多个ip集合List<String>
 * 空值返回 true 可以搭配@Notnull使用
 * @author binblink
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IPAddressValidator.class)
@Documented
public @interface ValidIPAddress {

    String message() default "无效的IP地址";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    
    // 可选：允许的IP类型
    IPType type() default IPType.ALL;

    //目标是单个还是多个
    TargetType targetType() default TargetType.INDIVIDUAL;
    // ip 类型 明确v4 明确 v6 All 混合
    enum IPType {
        IPV4, IPV6, ALL
    }

    enum TargetType {

        INDIVIDUAL, MULTIPLE
    }
}