package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.IsDate;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IsDateValidator 日期校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class IsDateValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private IsDateValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IsDateValidator();
    }

    /**
     * 固定日期 Supplier - 返回 2025-06-15
     */
    public static class FixedDateSupplier implements Supplier<LocalDate> {
        @Override
        public LocalDate get() {
            return LocalDate.of(2025, 6, 15);
        }
    }

    /**
     * 当前日期 Supplier - 返回当前日期
     */
    public static class CurrentDateSupplier implements Supplier<LocalDate> {
        @Override
        public LocalDate get() {
            return LocalDate.now();
        }
    }

    /**
     * 年初日期 Supplier - 返回 2025-01-01
     */
    public static class YearStartSupplier implements Supplier<LocalDate> {
        @Override
        public LocalDate get() {
            return LocalDate.of(2025, 1, 1);
        }
    }

    /**
     * 年末日期 Supplier - 返回 2025-12-31
     */
    public static class YearEndSupplier implements Supplier<LocalDate> {
        @Override
        public LocalDate get() {
            return LocalDate.of(2025, 12, 31);
        }
    }

    /**
     * 闰日 Supplier - 返回 2024-02-29
     */
    public static class LeapDaySupplier implements Supplier<LocalDate> {
        @Override
        public LocalDate get() {
            return LocalDate.of(2024, 2, 29);
        }
    }

    @Nested
    @DisplayName("null值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("null值应返回true（由@NotNull处理）")
        void isValid_nullValue_returnsTrue() {
            validator.initialize(createIsDateAnnotation(FixedDateSupplier.class));

            boolean result = validator.isValid(null, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("日期相等校验测试")
    class DateEqualTests {

        @Test
        @DisplayName("日期相等应返回true")
        void isValid_dateEquals_returnsTrue() {
            validator.initialize(createIsDateAnnotation(FixedDateSupplier.class));

            LocalDate sameDate = LocalDate.of(2025, 6, 15);

            boolean result = validator.isValid(sameDate, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("日期不等应返回false")
        void isValid_dateNotEquals_returnsFalse() {
            validator.initialize(createIsDateAnnotation(FixedDateSupplier.class));

            LocalDate differentDate = LocalDate.of(2025, 6, 16);

            boolean result = validator.isValid(differentDate, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("动态日期测试")
    class DynamicDateTests {

        @Test
        @DisplayName("当前日期校验应返回true")
        void isValid_currentDate_returnsTrue() {
            validator.initialize(createIsDateAnnotation(CurrentDateSupplier.class));

            LocalDate today = LocalDate.now();

            boolean result = validator.isValid(today, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("昨天日期校验应返回false")
        void isValid_yesterdayDate_returnsFalse() {
            validator.initialize(createIsDateAnnotation(CurrentDateSupplier.class));

            LocalDate yesterday = LocalDate.now().minusDays(1);

            boolean result = validator.isValid(yesterday, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("明天日期校验应返回false")
        void isValid_tomorrowDate_returnsFalse() {
            validator.initialize(createIsDateAnnotation(CurrentDateSupplier.class));

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            boolean result = validator.isValid(tomorrow, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("年初日期校验")
        void isValid_yearStart() {
            validator.initialize(createIsDateAnnotation(YearStartSupplier.class));

            assertTrue(validator.isValid(LocalDate.of(2025, 1, 1), context));
            assertFalse(validator.isValid(LocalDate.of(2025, 1, 2), context));
        }

        @Test
        @DisplayName("年末日期校验")
        void isValid_yearEnd() {
            validator.initialize(createIsDateAnnotation(YearEndSupplier.class));

            assertTrue(validator.isValid(LocalDate.of(2025, 12, 31), context));
            assertFalse(validator.isValid(LocalDate.of(2025, 12, 30), context));
        }

        @Test
        @DisplayName("闰年日期校验")
        void isValid_leapYear() {
            validator.initialize(createIsDateAnnotation(LeapDaySupplier.class));

            assertTrue(validator.isValid(LocalDate.of(2024, 2, 29), context));
            assertFalse(validator.isValid(LocalDate.of(2024, 2, 28), context));
        }
    }

    /**
     * 创建 IsDate 注解的 Mock 对象
     *
     * @param supplierClass 日期提供者类
     * @return Mock 注解对象
     */
    @SuppressWarnings("unchecked")
    private IsDate createIsDateAnnotation(Class<? extends Supplier<LocalDate>> supplierClass) {
        return new IsDate() {
            @Override
            public Class<? extends Supplier<LocalDate>> getDate() {
                return supplierClass;
            }

            @Override
            public String message() {
                return "日期不正确";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return IsDate.class;
            }
        };
    }
}
