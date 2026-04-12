package com.blink.framework.validate.validator;

import com.blink.framework.common.data.FieldConstraintCacheDO;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * FieldConstraintValidator 字段约束校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class FieldConstraintValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private CacheComponent cacheComponent;

    private FieldConstraintValidator validator;
    private MockedStatic<ApplicationContextUtil> applicationContextUtilMock;

    @BeforeEach
    void setUp() {
        validator = new FieldConstraintValidator();
        applicationContextUtilMock = mockStatic(ApplicationContextUtil.class);
        applicationContextUtilMock.when(() -> ApplicationContextUtil.getBean(CacheComponent.class))
                .thenReturn(cacheComponent);
    }

    @AfterEach
    void tearDown() {
        applicationContextUtilMock.close();
    }

    @Nested
    @DisplayName("null和空值处理测试")
    class NullAndEmptyTests {

        @Test
        @DisplayName("null值应返回true")
        void isValid_nullValue_returnsTrue() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            boolean result = validator.isValid(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("空字符串应返回true")
        void isValid_emptyString_returnsTrue() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            boolean result = validator.isValid("", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("空白字符串应返回true")
        void isValid_blankString_returnsTrue() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            boolean result = validator.isValid("   ", context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("缓存不存在测试")
    class CacheNotExistTests {

        @Test
        @DisplayName("缓存不存在应返回true（跳过校验）")
        void isValid_cacheNotExist_returnsTrue() {
            validator.initialize(createFieldConstraintAnnotation("nonExistentConstraint"));
            when(cacheComponent.getFromAllCache(anyString())).thenReturn(null);

            boolean result = validator.isValid("any value", context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("缓存存在校验测试")
    class CacheExistTests {

        @Test
        @DisplayName("缓存存在且校验通过应返回true")
        void isValid_cacheExistAndPass_returnsTrue() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            // 设置约束规则
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(10);
            constraint.setDataType("S");

            when(cacheComponent.getFromAllCache(anyString())).thenReturn(constraint);

            boolean result = validator.isValid("hello", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("缓存存在但校验失败应返回false")
        void isValid_cacheExistButFail_returnsFalse() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            // 设置约束规则
            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(3);
            constraint.setDataType("S");

            when(cacheComponent.getFromAllCache(anyString())).thenReturn(constraint);

            boolean result = validator.isValid("hello world", context);

            assertFalse(result);
        }

        @Test
        @DisplayName("小数类型校验应正确处理")
        void isValid_decimalType_returnsCorrect() {
            validator.initialize(createFieldConstraintAnnotation("decimalConstraint"));

            FieldConstraintCacheDO constraint = new FieldConstraintCacheDO();
            constraint.setMaxLength(20);
            constraint.setDataPrecision(2);
            constraint.setDataType("D");

            when(cacheComponent.getFromAllCache(anyString())).thenReturn(constraint);

            // 使用 Map 模拟缓存返回的对象
            java.math.BigDecimal validValue = new java.math.BigDecimal("123.45");

            boolean result = validator.isValid(validValue, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("缓存组件获取异常应返回false")
        void isValid_cacheComponentException_returnsFalse() {
            validator.initialize(createFieldConstraintAnnotation("testConstraint"));

            // 模拟异常
            when(cacheComponent.getFromAllCache(anyString())).thenThrow(new RuntimeException("Cache error"));

            boolean result = validator.isValid("test", context);

            assertFalse(result);
        }
    }

    /**
     * 创建 FieldConstraint 注解的 Mock 对象
     *
     * @param constraintName 约束名称
     * @return Mock 注解对象
     */
    private FieldConstraint createFieldConstraintAnnotation(String constraintName) {
        return new FieldConstraint() {
            @Override
            public String name() {
                return constraintName;
            }

            @Override
            public String message() {
                return "字段约束校验失败";
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
                return FieldConstraint.class;
            }
        };
    }
}
