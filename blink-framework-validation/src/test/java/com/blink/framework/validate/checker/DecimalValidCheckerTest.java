package com.blink.framework.validate.checker;

import com.blink.framework.common.data.FieldConstraintCacheDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DecimalValidChecker 小数校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
class DecimalValidCheckerTest {

    private DecimalValidChecker checker;

    @BeforeEach
    void setUp() {
        checker = new DecimalValidChecker();
    }

    @Nested
    @DisplayName("精度校验测试")
    class PrecisionTests {

        @Test
        @DisplayName("精度满足要求应返回true")
        void check_precisionMet_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            // scale为2，满足精度要求
            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("精度超过要求应返回true")
        void check_precisionExceeds_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            // scale为4，超过精度要求
            boolean result = checker.check(constraint, new BigDecimal("123.4567"));

            assertTrue(result);
        }

        @Test
        @DisplayName("精度不足应返回false")
        void check_precisionInsufficient_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(4);

            // scale为2，不足精度要求4
            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertFalse(result);
        }

        @Test
        @DisplayName("精度为null应跳过精度校验")
        void check_precisionNull_skipsPrecisionCheck() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(null);

            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("整数的scale为0")
        void check_integerScale() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(0);

            boolean result = checker.check(constraint, new BigDecimal("123"));

            assertTrue(result);
        }

        @Test
        @DisplayName("零精度要求整数scale为0")
        void check_zeroPrecisionRequiresScaleZero() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(0);

            // scale为0，满足精度要求0
            assertTrue(checker.check(constraint, new BigDecimal("123")));
            // scale为2，满足精度要求0（2 >= 0）
            assertTrue(checker.check(constraint, new BigDecimal("123.45")));
        }
    }

    @Nested
    @DisplayName("继承父类最大长度校验测试")
    class InheritedMaxLengthTests {

        @Test
        @DisplayName("字符串表示长度满足要求应返回true")
        void check_lengthMet_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataPrecision(2);

            // "123.45" 长度为6
            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("字符串表示长度超过最大值应返回false")
        void check_lengthExceeds_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(5);
            constraint.setDataPrecision(2);

            // "12345.67" 长度为8
            boolean result = checker.check(constraint, new BigDecimal("12345.67"));

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("继承父类正则校验测试")
    class InheritedPatternTests {

        @Test
        @DisplayName("匹配正则表达式应返回true")
        void check_matchesPattern_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);
            constraint.setDataPattern("^\\d+\\.\\d{2}$");

            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("不匹配正则表达式应返回false")
        void check_notMatchesPattern_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);
            constraint.setDataPattern("^\\d+\\.\\d{3}$");

            // 两位小数，正则要求三位
            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("综合场景测试")
    class CombinedTests {

        @Test
        @DisplayName("长度+精度+正则全部满足应返回true")
        void check_allConstraintsMet_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataPrecision(2);
            constraint.setDataPattern("^\\d{3}\\.\\d{2}$");

            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("满足精度但不满足长度应返回false")
        void check_precisionMetButLengthExceeds_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(5);
            constraint.setDataPrecision(2);

            boolean result = checker.check(constraint, new BigDecimal("12345.67"));

            assertFalse(result);
        }

        @Test
        @DisplayName("满足长度但不满足精度应返回false")
        void check_lengthMetButPrecisionInsufficient_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(4);

            boolean result = checker.check(constraint, new BigDecimal("123.45"));

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("大数值应正确处理")
        void check_largeValue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(30);
            constraint.setDataPrecision(10);

            BigDecimal largeValue = new BigDecimal("123456789012345.1234567890");

            boolean result = checker.check(constraint, largeValue);

            assertTrue(result);
        }

        @Test
        @DisplayName("负数应正确处理")
        void check_negativeValue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            boolean result = checker.check(constraint, new BigDecimal("-123.45"));

            assertTrue(result);
        }

        @Test
        @DisplayName("零值应正确处理")
        void check_zeroValue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(0);

            // BigDecimal.ZERO 的 scale 是 0，满足精度要求0
            boolean result = checker.check(constraint, BigDecimal.ZERO);

            assertTrue(result);
        }

        @Test
        @DisplayName("零值精度不足应返回false")
        void check_zeroValuePrecisionInsufficient_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);

            // BigDecimal.ZERO 的 scale 是 0，不满足精度要求2
            boolean result = checker.check(constraint, BigDecimal.ZERO);

            assertFalse(result);
        }

        @Test
        @DisplayName("极小小数应正确处理")
        void check_verySmallDecimal() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(30);
            constraint.setDataPrecision(10);

            BigDecimal smallValue = new BigDecimal("0.0000000001");

            boolean result = checker.check(constraint, smallValue);

            assertTrue(result);
        }
    }
}
