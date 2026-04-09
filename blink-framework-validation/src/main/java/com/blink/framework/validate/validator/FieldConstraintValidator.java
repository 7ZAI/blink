package com.blink.framework.validate.validator;

import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.common.data.FieldConstraintCacheDO;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.validate.FieldConstraintValidHandler;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 字段约束校验器
 * 根据 sys_field_constraint 表中定义的约束规则校验字段值
 *
 * @author binblink
 * @since 2026-03-07
 */
@Slf4j
public class FieldConstraintValidator implements ConstraintValidator<FieldConstraint, Object> {

    private String constraintName;

    @Override
    public void initialize(FieldConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        constraintName = constraintAnnotation.name();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // null 或空字符串（含空白）视为无值，跳过校验
            if (Objects.isNull(value) || (value instanceof String str && str.trim().isEmpty())) {
                return true;
            }

            CacheComponent cacheUtil = ApplicationContextUtil.getBean(CacheComponent.class);

            // 从缓存获取约束规则
            FieldConstraintCacheDO constraint = JacksonUtil.convert(
                    cacheUtil.getFromAllCache(RedisCacheKeyConstant.FIELD_CONSTRAINT_KEY_PREFIX + constraintName),
                    FieldConstraintCacheDO.class
            );

            // 缓存不存在，视为无约束规则，跳过校验
            if (Objects.isNull(constraint)) {
                log.warn("[FieldConstraint] 约束规则不存在，跳过校验 | constraintName: {}", constraintName);
                return true;
            }

            return FieldConstraintValidHandler.check(constraint, value);

        } catch (Exception e) {
            log.error("[FieldConstraint] 校验异常 | constraintName: {}, error: {}", constraintName, e.getMessage(), e);
            return false;
        }
    }
}