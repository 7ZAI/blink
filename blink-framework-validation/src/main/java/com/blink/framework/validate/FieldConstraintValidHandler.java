package com.blink.framework.validate;

import com.blink.framework.common.data.FieldConstraintCacheDO;
import com.blink.framework.validate.checker.DecimalValidChecker;
import com.blink.framework.validate.checker.FieldConstraintValidChecker;
import com.blink.framework.validate.checker.GeneralValidChecker;
import com.blink.framework.validate.constant.ConstraintDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 字段约束校验处理器
 *
 * @author binblink
 * @since 2026-03-07
 */
public class FieldConstraintValidHandler {

    /**
     * 校验器映射
     */
    private static final Map<String, FieldConstraintValidChecker> checkerMap = new HashMap<>(16);

    static {
        GeneralValidChecker generalValidChecker = new GeneralValidChecker();
        checkerMap.put(ConstraintDataType.DECIMAL.getType(), new DecimalValidChecker());
        checkerMap.put(ConstraintDataType.STRING.getType(), generalValidChecker);
        checkerMap.put(ConstraintDataType.NUMBER.getType(), generalValidChecker);
    }

    /**
     * 执行校验
     *
     * @param constraint 约束规则
     * @param value      待校验值
     * @return 是否通过校验
     */
    public static boolean check(FieldConstraintCacheDO constraint, Object value) {
        if (Objects.isNull(constraint) || Objects.isNull(value)) {
            return true;
        }

        FieldConstraintValidChecker checker = checkerMap.get(constraint.getDataType());

        if (Objects.isNull(checker)) {
            return false;
        }

        return checker.check(constraint, value);
    }
}