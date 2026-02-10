package com.blink.framework.validate.validator;

import com.blink.framework.common.utils.IPAddressUtils;
import com.blink.framework.validate.annotation.ValidIPAddress;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * ip格式校验
 *
 * @Author binblink
 */
@Slf4j
public class IPAddressValidator implements ConstraintValidator<ValidIPAddress, Object> {

    private ValidIPAddress.IPType type;

    private ValidIPAddress.TargetType targetType;

    @Override
    public void initialize(ValidIPAddress constraintAnnotation) {
        this.type = constraintAnnotation.type();
        this.targetType = constraintAnnotation.targetType();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext constraintValidatorContext) {
        //空默认 true
        if(obj == null){
            return true;
        }

        //单个ip
        if (targetType.equals(ValidIPAddress.TargetType.INDIVIDUAL)) {
            String target = "";
            if (obj instanceof String) {
                target = (String) obj;
            } else {
                return false;
            }
            return judgeIp(target);
        }
        //集合ip
        if (targetType.equals(ValidIPAddress.TargetType.MULTIPLE)) {
            Collection<?> ips;
            if (obj instanceof Collection<?>) {
                ips = (Collection<?>) obj;
            } else {
                return false;
            }

            for (Object o : ips) {
                //不是String类型
                String ip;
                if (!(o instanceof String)) {
                    return false;
                }
                ip = (String) o;
                boolean r = judgeIp(ip);
                //一旦有非法ip 直接返回false
                if (!r) {
                    return false;
                }
            }
            //全部判断完 返回
            return true;
        }

        //兜底
        return true;
    }

    //判断ip
    private boolean judgeIp(String ip) {

        //ip4 ip6全部校验
        if (type.equals(ValidIPAddress.IPType.ALL)) {

            if (IPAddressUtils.isIPv4Valid(ip)) {
                return true;
            }
            return IPAddressUtils.isIPv6Valid(ip);
        }
        //单ip4 校验
        if (type.equals(ValidIPAddress.IPType.IPV4)) {
            return IPAddressUtils.isIPv4Valid(ip);
        }
        //单ip6 校验
        if (type.equals(ValidIPAddress.IPType.IPV6)) {
            return IPAddressUtils.isIPv6Valid(ip);
        }

        return false;
    }


}
