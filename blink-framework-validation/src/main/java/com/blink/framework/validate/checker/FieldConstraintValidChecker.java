package com.blink.framework.validate.checker;

import com.blink.framework.common.data.FieldConstraintCacheDO;

/**
 * 字段约束校验器接口
 *
 * @author binblink
 * @since 2026-03-07
 */
public interface FieldConstraintValidChecker {

    /**
     * 执行校验
     *
     * @param constraint 约束规则
     * @param value      待校验值
     * @return 是否通过校验
     */
    boolean check(FieldConstraintCacheDO constraint, Object value);
}