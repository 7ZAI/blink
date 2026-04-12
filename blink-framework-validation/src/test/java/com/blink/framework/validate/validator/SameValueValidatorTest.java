package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.SameValue;
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
 * SameValueValidator 多字段相等校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class SameValueValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private SameValueValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SameValueValidator();
    }

    /**
     * 测试用的DTO对象 - 两个字段
     */
    @Data
    static class TwoFieldsDTO {
        private String field1;
        private String field2;
    }

    /**
     * 测试用的DTO对象 - 三个字段
     */
    @Data
    static class ThreeFieldsDTO {
        private String field1;
        private String field2;
        private String field3;
    }

    /**
     * 测试用的DTO对象 - 整型字段
     */
    @Data
    static class IntegerFieldsDTO {
        private Integer value1;
        private Integer value2;
    }

    @Nested
    @DisplayName("两字段相等校验测试")
    class TwoFieldsEqualTests {

        @Test
        @DisplayName("两个字段值相等应返回true")
        void isValid_twoFieldsEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("test");
            dto.setField2("test");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("两个字段值不等应返回false")
        void isValid_twoFieldsNotEqual_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("test1");
            dto.setField2("test2");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("第一个字段为null应返回false")
        void isValid_firstFieldNull_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1(null);
            dto.setField2("test");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("第二个字段为null应返回false")
        void isValid_secondFieldNull_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("test");
            dto.setField2(null);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("两个字段均为null应返回true（由@NotNull处理）")
        void isValid_bothFieldsNull_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1(null);
            dto.setField2(null);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("多字段相等校验测试")
    class MultipleFieldsEqualTests {

        @Test
        @DisplayName("三个字段值全部相等应返回true")
        void isValid_threeFieldsAllEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2", "field3"));

            ThreeFieldsDTO dto = new ThreeFieldsDTO();
            dto.setField1("same");
            dto.setField2("same");
            dto.setField3("same");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("三个字段中有一个不等应返回false")
        void isValid_threeFieldsOneDifferent_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2", "field3"));

            ThreeFieldsDTO dto = new ThreeFieldsDTO();
            dto.setField1("same");
            dto.setField2("same");
            dto.setField3("different");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("三个字段中有null应返回false")
        void isValid_threeFieldsWithNull_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2", "field3"));

            ThreeFieldsDTO dto = new ThreeFieldsDTO();
            dto.setField1("same");
            dto.setField2(null);
            dto.setField3("same");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("不同类型字段测试")
    class DifferentTypeFieldsTests {

        @Test
        @DisplayName("整型字段相等应返回true")
        void isValid_integerFieldsEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("value1", "value2"));

            IntegerFieldsDTO dto = new IntegerFieldsDTO();
            dto.setValue1(100);
            dto.setValue2(100);

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("整型字段不等应返回false")
        void isValid_integerFieldsNotEqual_returnsFalse() {
            validator.initialize(createSameValueAnnotation("value1", "value2"));

            IntegerFieldsDTO dto = new IntegerFieldsDTO();
            dto.setValue1(100);
            dto.setValue2(200);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("整型字段null应返回false")
        void isValid_integerFieldsNull_returnsFalse() {
            validator.initialize(createSameValueAnnotation("value1", "value2"));

            IntegerFieldsDTO dto = new IntegerFieldsDTO();
            dto.setValue1(100);
            dto.setValue2(null);

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("空字符串字段相等应返回true")
        void isValid_emptyStringEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("");
            dto.setField2("");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("空字符串与非空字符串不等应返回false")
        void isValid_emptyAndNonEmptyString_returnsFalse() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("");
            dto.setField2("test");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("特殊字符字段相等应返回true")
        void isValid_specialCharactersEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("!@#$%^&*()");
            dto.setField2("!@#$%^&*()");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("中文字符字段相等应返回true")
        void isValid_chineseCharactersEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("中文测试");
            dto.setField2("中文测试");

            boolean result = validator.isValid(dto, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("长字符串相等应返回true")
        void isValid_longStringEqual_returnsTrue() {
            validator.initialize(createSameValueAnnotation("field1", "field2"));

            String longString = "a".repeat(10000);

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1(longString);
            dto.setField2(longString);

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
            validator.initialize(createSameValueAnnotation("nonExistent", "field1"));

            TwoFieldsDTO dto = new TwoFieldsDTO();
            dto.setField1("test");
            dto.setField2("test");

            boolean result = validator.isValid(dto, context);

            assertFalse(result);
        }
    }

    /**
     * 创建 SameValue 注解的 Mock 对象
     *
     * @param fields 字段名数组
     * @return Mock 注解对象
     */
    private SameValue createSameValueAnnotation(String... fields) {
        return new SameValue() {
            @Override
            public String[] fields() {
                return fields;
            }

            @Override
            public String message() {
                return "值不一致";
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
                return SameValue.class;
            }
        };
    }
}
