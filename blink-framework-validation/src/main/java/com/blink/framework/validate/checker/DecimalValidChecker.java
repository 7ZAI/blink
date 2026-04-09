package com.blink.framework.validate.checker;

import com.blink.framework.common.data.FieldConstraintCacheDO;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 小数校验器
 * 校验小数的长度、正则表达式和精度
 *
 * @author binblink
 * @since 2026-03-07
 */
public class DecimalValidChecker extends GeneralValidChecker {

    @Override
    public boolean check(FieldConstraintCacheDO constraint, Object value) {
        BigDecimal decimal = (BigDecimal) value;
        String decimalStr = decimal.toPlainString();

        if (!checkMaxLength(constraint, decimalStr)) {
            return false;
        }

        if (!checkPattern(constraint, decimalStr)) {
            return false;
        }

        // 精度判断
        return !Objects.nonNull(constraint.getDataPrecision()) || decimal.scale() >= constraint.getDataPrecision();
    }
}