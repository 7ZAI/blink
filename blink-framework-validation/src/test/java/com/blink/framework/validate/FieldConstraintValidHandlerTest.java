package com.blink.framework.validate;

import com.blink.framework.common.data.FieldConstraintCacheDO;
import com.blink.framework.validate.checker.DecimalValidChecker;
import com.blink.framework.validate.checker.FieldConstraintValidChecker;
import com.blink.framework.validate.checker.GeneralValidChecker;
import com.blink.framework.validate.constant.ConstraintDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FieldConstraintValidHandler 校验处理器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
class FieldConstraintValidHandlerTest {

    @Nested
    @DisplayName("路由分发测试")
    class RoutingTests {

        @Test
        @DisplayName("DECIMAL类型应路由到DecimalValidChecker")
        void check_decimalType_routesToDecimalChecker() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.DECIMAL.getType());
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            BigDecimal value = new BigDecimal("123.45");

            boolean result = FieldConstraintValidHandler.check(constraint, value);

            assertTrue(result);
        }

        @Test
        @DisplayName("STRING类型应路由到GeneralValidChecker")
        void check_stringType_routesToGeneralChecker() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.STRING.getType());
            constraint.setMaxLength(10);

            String value = "hello";

            boolean result = FieldConstraintValidHandler.check(constraint, value);

            assertTrue(result);
        }

        @Test
        @DisplayName("NUMBER类型应路由到GeneralValidChecker")
        void check_numberType_routesToGeneralChecker() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.NUMBER.getType());
            constraint.setMaxLength(10);

            Integer value = 12345;

            boolean result = FieldConstraintValidHandler.check(constraint, value);

            assertTrue(result);
        }

        @Test
        @DisplayName("未知类型应返回false")
        void check_unknownType_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType("UNKNOWN");

            String value = "test";

            boolean result = FieldConstraintValidHandler.check(constraint, value);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("null参数处理测试")
    class NullParameterTests {

        @Test
        @DisplayName("constraint为null应返回true")
        void check_constraintNull_returnsTrue() {
            boolean result = FieldConstraintValidHandler.check(null, "any value");

            assertTrue(result);
        }

        @Test
        @DisplayName("value为null应返回true")
        void check_valueNull_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.STRING.getType());
            constraint.setMaxLength(10);

            boolean result = FieldConstraintValidHandler.check(constraint, null);

            assertTrue(result);
        }

        @Test
        @DisplayName("两者均为null应返回true")
        void check_bothNull_returnsTrue() {
            boolean result = FieldConstraintValidHandler.check(null, null);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("综合场景测试")
    class CombinedTests {

        @Test
        @DisplayName("STRING类型正则校验应正确")
        void check_stringTypePatternValidation() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.STRING.getType());
            constraint.setMaxLength(20);
            constraint.setDataPattern("^[a-z]+$");

            assertTrue(FieldConstraintValidHandler.check(constraint, "hello"));
            assertFalse(FieldConstraintValidHandler.check(constraint, "Hello"));
        }

        @Test
        @DisplayName("DECIMAL类型精度校验应正确")
        void check_decimalTypePrecisionValidation() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.DECIMAL.getType());
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            // 精度满足
            assertTrue(FieldConstraintValidHandler.check(constraint, new BigDecimal("123.45")));
            // 精度不满足
            assertFalse(FieldConstraintValidHandler.check(constraint, new BigDecimal("123.4")));
        }

        @Test
        @DisplayName("NUMBER类型长度校验应正确")
        void check_numberTypeLengthValidation() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setDataType(ConstraintDataType.NUMBER.getType());
            constraint.setMaxLength(5);

            // 长度满足
            assertTrue(FieldConstraintValidHandler.check(constraint, 12345));
            // 长度超过
            assertFalse(FieldConstraintValidHandler.check(constraint, 123456));
        }
    }
}
