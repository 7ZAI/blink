package com.blink.framework.validate.checker;



import com.blink.framework.validate.DictCacheDO;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 *
 * @author binblink
 */
public class GeneralValidChecker implements DictValidChecker {
    @Override
    public boolean check(DictCacheDO dict, Object value) {

        String valueStr = String.valueOf(value);

        if(!checkMaxLength(dict,valueStr)){
            return false;
        }

        return checkPattern(dict, valueStr);
    }

    protected boolean checkMaxLength(DictCacheDO dict,String valueStr){
        //先判断长度
        return !Objects.nonNull(dict) || valueStr.length() <= dict.getMaxLength();
    }

    protected boolean checkPattern(DictCacheDO dict,String valueStr){
        //正则表达式不为空
        if(!StringUtils.isEmpty(dict.getDataPattern())){
            return Pattern.matches(dict.getDataPattern(),valueStr);
        }
        return true;
    }
}
