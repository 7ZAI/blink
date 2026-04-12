package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.StartEndDate;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DateRangeValidator 日期范围校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class DateRangeValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private DateRangeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
    }

    /**
     * 测试用的DTO对象
     */
    @Data
    static class DateRangeDTO {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Nested
    @DisplayName("正常日期范围测试")
    class NormalDateRangeTests {

        @Test
        @DisplayName("开始日期早于结束日期应返回true")
        void isValid_startBeforeEnd_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 1, 1));
            dto.setEndDate(LocalDate.of(2025, 12, 31));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("开始日期等于结束日期应返回true")
        void isValid_startEqualsEnd_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            LocalDate sameDate = LocalDate.of(2025, 6, 15);
            dto.setStartDate(sameDate);
            dto.setEndDate(sameDate);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("开始日期晚于结束日期应返回false")
        void isValid_startAfterEnd_returnsFalse() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 12, 31));
            dto.setEndDate(LocalDate.of(2025, 1, 1));

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("null值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("开始日期为null应返回true（由@NotNull处理）")
        void isValid_startDateNull_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(null);
            dto.setEndDate(LocalDate.of(2025, 12, 31));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("结束日期为null应返回true（由@NotNull处理）")
        void isValid_endDateNull_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 1, 1));
            dto.setEndDate(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("两个日期均为null应返回true")
        void isValid_bothDatesNull_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(null);
            dto.setEndDate(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("最小日期范围（同一天）应返回true")
        void isValid_sameDay_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            LocalDate date = LocalDate.MIN;
            dto.setStartDate(date);
            dto.setEndDate(date);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("最大日期范围应返回true")
        void isValid_maxDateRange_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.MIN);
            dto.setEndDate(LocalDate.MAX);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("相邻日期应返回true")
        void isValid_adjacentDates_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 1, 1));
            dto.setEndDate(LocalDate.of(2025, 1, 2));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("跨年日期范围应返回true")
        void isValid_crossYearRange_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2024, 12, 31));
            dto.setEndDate(LocalDate.of(2025, 1, 1));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("跨月日期范围应返回true")
        void isValid_crossMonthRange_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 1, 31));
            dto.setEndDate(LocalDate.of(2025, 2, 1));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("闰年日期范围应返回true")
        void isValid_leapYearRange_returnsTrue() {
            validator.initialize(createStartEndDateAnnotation("startDate", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2024, 2, 28));
            dto.setEndDate(LocalDate.of(2024, 2, 29));

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("不存在的字段名应返回false")
        void isValid_nonExistentField_returnsFalse() {
            validator.initialize(createStartEndDateAnnotation("nonExistent", "endDate"));

            DateRangeDTO dto = new DateRangeDTO();
            dto.setStartDate(LocalDate.of(2025, 1, 1));
            dto.setEndDate(LocalDate.of(2025, 12, 31));

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    /**
     * 创建 StartEndDate 注解的 Mock 对象
     *
     * @param startDateField 开始日期字段名
     * @param endDateField   结束日期字段名
     * @return Mock 注解对象
     */
    private StartEndDate createStartEndDateAnnotation(String startDateField, String endDateField) {
        return new StartEndDate() {
            @Override
            public String startDate() {
                return startDateField;
            }

            @Override
            public String endDate() {
                return endDateField;
            }

            @Override
            public String message() {
                return "开始日期必须早于结束日期";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return StartEndDate.class;
            }
        };
    }
}
