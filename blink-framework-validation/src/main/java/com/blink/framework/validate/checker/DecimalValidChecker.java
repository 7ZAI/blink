package com.blink.framework.validate.checker;




import com.blink.framework.common.data.DictCacheDO;

import java.math.BigDecimal;
import java.util.Objects;

public class DecimalValidChecker extends GeneralValidChecker {

    @Override
    public boolean check(DictCacheDO dict, Object value) {

        BigDecimal decimal = (BigDecimal) value;
        String decimalStr = decimal.toPlainString();

        if(!checkMaxLength(dict,decimalStr)){
            return false;
        }

        if(!checkPattern(dict,decimalStr)){
            return false;
        }

        //精度判断 精度为2  0.0 无法通过校验
        return !Objects.nonNull(dict.getDataPrecision()) || decimal.scale() >= dict.getDataPrecision();
    }
}
