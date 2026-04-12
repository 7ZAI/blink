package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.ConditionalRequired;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConditionalRequiredValidator 条件必填校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class ConditionalRequiredValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private ConditionalRequiredValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConditionalRequiredValidator();
    }

    /**
     * 测试用的DTO对象
     */
    @Data
    static class ConditionalDTO {
        private String status;
        private String reason;
        private Integer type;
        private String description;
    }

    @Nested
    @DisplayName("条件匹配场景测试")
    class ConditionMatchedTests {

        @Test
        @DisplayName("条件匹配且必填字段有值应返回true")
        void isValid_conditionMatchedAndRequiredFieldHasValue_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");
            dto.setReason("测试原因");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("条件匹配但必填字段为null应返回false")
        void isValid_conditionMatchedAndRequiredFieldNull_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");
            dto.setReason(null);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("条件匹配且必填字段为空字符串应返回false")
        void isValid_conditionMatchedAndRequiredFieldEmptyString_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");
            dto.setReason("");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("条件匹配且必填字段为空白字符串应返回false")
        void isValid_conditionMatchedAndRequiredFieldBlankString_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");
            dto.setReason("   ");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("条件不匹配场景测试")
    class ConditionNotMatchedTests {

        @Test
        @DisplayName("条件不匹配应返回true（无需校验必填）")
        void isValid_conditionNotMatched_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("rejected");
            dto.setReason(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("条件不匹配且必填字段为空应返回true")
        void isValid_conditionNotMatchedAndRequiredFieldEmpty_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("pending");
            dto.setReason("");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("条件字段为null测试")
    class ConditionFieldNullTests {

        @Test
        @DisplayName("条件字段为null应返回true（不匹配任何条件值）")
        void isValid_conditionFieldNull_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus(null);
            dto.setReason(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("不同类型字段测试")
    class DifferentTypeFieldTests {

        @Test
        @DisplayName("整型条件字段匹配应返回true")
        void isValid_integerConditionMatched_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("type", "1", "description"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setType(1);
            dto.setDescription("描述信息");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("整型条件字段匹配但必填字段为null应返回false")
        void isValid_integerConditionMatchedButRequiredNull_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("type", "1", "description"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setType(1);
            dto.setDescription(null);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("整型条件字段不匹配应返回true")
        void isValid_integerConditionNotMatched_returnsTrue() {
            validator.initialize(createConditionalRequiredAnnotation("type", "1", "description"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setType(2);
            dto.setDescription(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("条件值为空字符串应正确匹配")
        void isValid_emptyConditionValue_matchesCorrectly() {
            validator.initialize(createConditionalRequiredAnnotation("status", "", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("");
            dto.setReason("原因");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("条件值为空字符串匹配但必填为null应返回false")
        void isValid_emptyConditionValueMatchedButRequiredNull_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("status", "", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("");
            dto.setReason(null);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("条件值为特殊字符应正确匹配")
        void isValid_specialCharConditionValue_matchesCorrectly() {
            validator.initialize(createConditionalRequiredAnnotation("status", "special!@#", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("special!@#");
            dto.setReason("原因");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("条件值为中文应正确匹配")
        void isValid_chineseConditionValue_matchesCorrectly() {
            validator.initialize(createConditionalRequiredAnnotation("status", "通过", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("通过");
            dto.setReason("测试原因");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("不存在的条件字段应返回false")
        void isValid_nonExistentConditionField_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("nonExistent", "approved", "reason"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");
            dto.setReason("测试");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("不存在的必填字段应返回false")
        void isValid_nonExistentRequiredField_returnsFalse() {
            validator.initialize(createConditionalRequiredAnnotation("status", "approved", "nonExistent"));

            ConditionalDTO dto = new ConditionalDTO();
            dto.setStatus("approved");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    /**
     * 创建 ConditionalRequired 注解的 Mock 对象
     *
     * @param conditionField 条件字段名
     * @param conditionValue 条件值
     * @param requiredField  必填字段名
     * @return Mock 注解对象
     */
    private ConditionalRequired createConditionalRequiredAnnotation(
            String conditionField, String conditionValue, String requiredField) {
        return new ConditionalRequired() {
            @Override
            public String conditionField() {
                return conditionField;
            }

            @Override
            public String conditionValue() {
                return conditionValue;
            }

            @Override
            public String requiredField() {
                return requiredField;
            }

            @Override
            public String message() {
                return "当" + conditionField + "为" + conditionValue + "时，" + requiredField + "必须填写";
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
                return ConditionalRequired.class;
            }
        };
    }
}
