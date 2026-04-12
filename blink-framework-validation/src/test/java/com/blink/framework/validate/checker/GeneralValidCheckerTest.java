package com.blink.framework.validate.checker;

import com.blink.framework.common.data.FieldConstraintCacheDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GeneralValidChecker 通用校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
class GeneralValidCheckerTest {

    private GeneralValidChecker checker;

    @BeforeEach
    void setUp() {
        checker = new GeneralValidChecker();
    }

    @Nested
    @DisplayName("最大长度校验测试")
    class MaxLengthTests {

        @Test
        @DisplayName("字符串长度小于最大长度应返回true")
        void check_stringLengthLessThanMax_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);

            boolean result = checker.check(constraint, "hello");

            assertTrue(result);
        }

        @Test
        @DisplayName("字符串长度等于最大长度应返回true")
        void check_stringLengthEqualsMax_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(5);

            boolean result = checker.check(constraint, "hello");

            assertTrue(result);
        }

        @Test
        @DisplayName("字符串长度超过最大长度应返回false")
        void check_stringLengthExceedsMax_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(3);

            boolean result = checker.check(constraint, "hello");

            assertFalse(result);
        }

        @Test
        @DisplayName("最大长度设置为0时，只有空字符串通过")
        void check_maxLengthZero_onlyEmptyStringPasses() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(0);

            assertTrue(checker.check(constraint, ""));
            assertFalse(checker.check(constraint, "a"));
        }

        @Test
        @DisplayName("空字符串长度为0应通过校验")
        void check_emptyStringLengthZero_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);

            boolean result = checker.check(constraint, "");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("正则表达式校验测试")
    class PatternTests {

        @Test
        @DisplayName("匹配正则表达式应返回true")
        void check_matchesPattern_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(100); // 设置足够大的maxLength避免NPE
            constraint.setDataPattern("^[a-z]+$");

            boolean result = checker.check(constraint, "hello");

            assertTrue(result);
        }

        @Test
        @DisplayName("不匹配正则表达式应返回false")
        void check_notMatchesPattern_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(100);
            constraint.setDataPattern("^[a-z]+$");

            boolean result = checker.check(constraint, "Hello123");

            assertFalse(result);
        }

        @Test
        @DisplayName("手机号正则表达式校验")
        void check_phonePattern_returnsCorrect() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(100);
            constraint.setDataPattern("^1[3-9]\\d{9}$");

            assertTrue(checker.check(constraint, "13812345678"));
            assertTrue(checker.check(constraint, "15912345678"));
            assertFalse(checker.check(constraint, "12812345678"));
            assertFalse(checker.check(constraint, "1381234567"));
            assertFalse(checker.check(constraint, "138123456789"));
        }

        @Test
        @DisplayName("邮箱正则表达式校验")
        void check_emailPattern_returnsCorrect() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(100);
            constraint.setDataPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

            assertTrue(checker.check(constraint, "test@example.com"));
            assertTrue(checker.check(constraint, "user.name@example.org"));
            assertFalse(checker.check(constraint, "invalid-email"));
            assertFalse(checker.check(constraint, "test@.com"));
        }
    }

    @Nested
    @DisplayName("综合校验测试")
    class CombinedTests {

        @Test
        @DisplayName("同时满足长度和正则应返回true")
        void check_meetsBothLengthAndPattern_returnsTrue() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataPattern("^[a-z]+$");

            boolean result = checker.check(constraint, "hello");

            assertTrue(result);
        }

        @Test
        @DisplayName("满足正则但超过长度应返回false")
        void check_matchesPatternButExceedsLength_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(3);
            constraint.setDataPattern("^[a-z]+$");

            boolean result = checker.check(constraint, "hello");

            assertFalse(result);
        }

        @Test
        @DisplayName("满足长度但不匹配正则应返回false")
        void check_meetsLengthButNotMatchesPattern_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataPattern("^[a-z]+$");

            boolean result = checker.check(constraint, "Hello");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("中文字符串长度计算应正确")
        void check_chineseStringLengthCorrect() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(5);

            // 中文字符每个占1个长度（按字符数计算）
            boolean result = checker.check(constraint, "中文测试五");

            assertTrue(result);
        }

        @Test
        @DisplayName("特殊字符字符串长度计算应正确")
        void check_specialCharStringLengthCorrect() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(5);

            boolean result = checker.check(constraint, "!@#$%");

            assertTrue(result);
        }

        @Test
        @DisplayName("超长字符串应返回false")
        void check_veryLongString_returnsFalse() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);

            String longString = "a".repeat(1000);

            boolean result = checker.check(constraint, longString);

            assertFalse(result);
        }

        @Test
        @DisplayName("数字类型转换为字符串后校验")
        void check_numberConvertedToString() {
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataPattern("^\\d+$");

            boolean result = checker.check(constraint, 12345);

            assertTrue(result);
        }
    }
}
