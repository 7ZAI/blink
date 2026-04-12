package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.MutuallyExclusive;
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
 * MutuallyExclusiveValidator 互斥字段校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class MutuallyExclusiveValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private MutuallyExclusiveValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MutuallyExclusiveValidator();
    }

    /**
     * 测试用的DTO对象
     */
    @Data
    static class MutualDTO {
        private String field1;
        private String field2;
        private Integer intValue1;
        private Integer intValue2;
    }

    @Nested
    @DisplayName("两字段均为null测试")
    class BothFieldsNullTests {

        @Test
        @DisplayName("两字段均为null应返回true")
        void isValid_bothFieldsNull_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1(null);
            dto.setField2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("单一字段有值测试")
    class SingleFieldValueTests {

        @Test
        @DisplayName("字段1有值，字段2为null应返回true")
        void isValid_field1HasValueField2Null_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("value1");
            dto.setField2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("字段1为null，字段2有值应返回true")
        void isValid_field1NullField2HasValue_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1(null);
            dto.setField2("value2");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("两字段均有值测试")
    class BothFieldsHaveValueTests {

        @Test
        @DisplayName("两字段均有值应返回false")
        void isValid_bothFieldsHaveValue_returnsFalse() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("value1");
            dto.setField2("value2");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("两字段均有相同值应返回false")
        void isValid_bothFieldsHaveSameValue_returnsFalse() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("same");
            dto.setField2("same");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("空字符串处理测试")
    class EmptyStringTests {

        @Test
        @DisplayName("字段1为空字符串，字段2为null应返回true")
        void isValid_field1EmptyField2Null_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("");
            dto.setField2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("字段1为null，字段2为空字符串应返回true")
        void isValid_field1NullField2Empty_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1(null);
            dto.setField2("");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("两字段均为空字符串应返回true")
        void isValid_bothFieldsEmpty_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("");
            dto.setField2("");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("字段1有值，字段2为空字符串应返回true")
        void isValid_field1HasValueField2Empty_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("value1");
            dto.setField2("");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("字段1为空字符串，字段2有值应返回true")
        void isValid_field1EmptyField2HasValue_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("field1", "field2"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("");
            dto.setField2("value2");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("整型字段测试")
    class IntegerFieldTests {

        @Test
        @DisplayName("整型字段1有值，字段2为null应返回true")
        void isValid_intField1HasValueField2Null_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("intValue1", "intValue2"));

            MutualDTO dto = new MutualDTO();
            dto.setIntValue1(100);
            dto.setIntValue2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("整型字段均为null应返回true")
        void isValid_bothIntFieldsNull_returnsTrue() {
            validator.initialize(createMutuallyExclusiveAnnotation("intValue1", "intValue2"));

            MutualDTO dto = new MutualDTO();
            dto.setIntValue1(null);
            dto.setIntValue2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("整型字段均有值应返回false")
        void isValid_bothIntFieldsHaveValue_returnsFalse() {
            validator.initialize(createMutuallyExclusiveAnnotation("intValue1", "intValue2"));

            MutualDTO dto = new MutualDTO();
            dto.setIntValue1(100);
            dto.setIntValue2(200);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("整型字段值为0应有值（非null）应返回false")
        void isValid_intFieldZeroHasValue_returnsFalse() {
            validator.initialize(createMutuallyExclusiveAnnotation("intValue1", "intValue2"));

            MutualDTO dto = new MutualDTO();
            dto.setIntValue1(0);
            dto.setIntValue2(100);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("不存在的字段名应返回false")
        void isValid_nonExistentField_returnsFalse() {
            validator.initialize(createMutuallyExclusiveAnnotation("nonExistent", "field1"));

            MutualDTO dto = new MutualDTO();
            dto.setField1("value1");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    /**
     * 创建 MutuallyExclusive 注解的 Mock 对象
     *
     * @param field1 第一个字段名
     * @param field2 第二个字段名
     * @return Mock 注解对象
     */
    private MutuallyExclusive createMutuallyExclusiveAnnotation(String field1, String field2) {
        return new MutuallyExclusive() {
            @Override
            public String field1() {
                return field1;
            }

            @Override
            public String field2() {
                return field2;
            }

            @Override
            public String message() {
                return field1 + "和" + field2 + "不能同时有值";
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
                return MutuallyExclusive.class;
            }
        };
    }
}
