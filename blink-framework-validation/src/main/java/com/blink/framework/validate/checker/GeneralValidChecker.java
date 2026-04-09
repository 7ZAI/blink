package com.blink.framework.validate.checker;

import com.blink.framework.common.data.FieldConstraintCacheDO;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 通用校验器
 * 校验字符串长度和正则表达式
 *
 * @author binblink
 * @since 2026-03-07
 */
public class GeneralValidChecker implements FieldConstraintValidChecker {

    @Override
    public boolean check(FieldConstraintCacheDO constraint, Object value) {
        String valueStr = String.valueOf(value);

        if (!checkMaxLength(constraint, valueStr)) {
            return false;
        }

        return checkPattern(constraint, valueStr);
    }

    /**
     * 校验最大长度
     *
     * @param constraint 约束规则
     * @param valueStr   待校验字符串
     * @return 是否通过校验
     */
    protected boolean checkMaxLength(FieldConstraintCacheDO constraint, String valueStr) {
        return !Objects.nonNull(constraint) || valueStr.length() <= constraint.getMaxLength();
    }

    /**
     * 校验正则表达式
     *
     * @param constraint 约束规则
     * @param valueStr   待校验字符串
     * @return 是否通过校验
     */
    protected boolean checkPattern(FieldConstraintCacheDO constraint, String valueStr) {
        // 正则表达式不为空
        if (constraint.getDataPattern() != null && !constraint.getDataPattern().isEmpty()) {
            return Pattern.matches(constraint.getDataPattern(), valueStr);
        }
        return true;
    }
}