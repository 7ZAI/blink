package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.NonNegative;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IsNoNegativeValidator 非负校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class IsNoNegativeValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private NonNegative nonNegativeAnnotation;

    private IsNoNegativeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IsNoNegativeValidator();
    }

    @Nested
    @DisplayName("null值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("null值应返回true（由@NotNull处理）")
        void isValid_nullValue_returnsTrue() {
            // includeZero 默认为 true
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(null, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("BigDecimal类型测试")
    class BigDecimalTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_bigDecimalZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(BigDecimal.ZERO, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_bigDecimalZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(BigDecimal.ZERO, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正数应返回true")
        void isValid_bigDecimalPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new BigDecimal("123.45"), context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负数应返回false")
        void isValid_bigDecimalNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new BigDecimal("-1.23"), context);

            assertFalse(result);
        }

        @Test
        @DisplayName("边界值：-1应返回false")
        void isValid_bigDecimalNegativeOne_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new BigDecimal("-1"), context);

            assertFalse(result);
        }

        @Test
        @DisplayName("极小正数应返回true")
        void isValid_bigDecimalVerySmallPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(new BigDecimal("0.0000001"), context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("BigInteger类型测试")
    class BigIntegerTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_bigIntegerZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(BigInteger.ZERO, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_bigIntegerZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(BigInteger.ZERO, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正数应返回true")
        void isValid_bigIntegerPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new BigInteger("999999999999999999"), context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负数应返回false")
        void isValid_bigIntegerNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new BigInteger("-999999999999999999"), context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Integer类型测试")
    class IntegerTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_integerZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(0, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_integerZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(0, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正整数应返回true")
        void isValid_integerPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(100, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负整数应返回false")
        void isValid_integerNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(-1, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Integer.MAX_VALUE应返回true")
        void isValid_integerMaxValue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(Integer.MAX_VALUE, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Long类型测试")
    class LongTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_longZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(0L, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_longZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(0L, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正Long值应返回true")
        void isValid_longPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(Long.MAX_VALUE, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负Long值应返回false")
        void isValid_longNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(-1L, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Double类型测试")
    class DoubleTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_doubleZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(0.0, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_doubleZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(0.0, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正Double值应返回true")
        void isValid_doublePositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(123.456, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负Double值应返回false")
        void isValid_doubleNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(-0.0001, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Float类型测试")
    class FloatTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_floatZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(0.0f, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_floatZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid(0.0f, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正Float值应返回true")
        void isValid_floatPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(99.99f, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负Float值应返回false")
        void isValid_floatNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(-0.01f, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Short类型测试")
    class ShortTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_shortZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid((short) 0, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_shortZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid((short) 0, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正Short值应返回true")
        void isValid_shortPositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(Short.MAX_VALUE, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负Short值应返回false")
        void isValid_shortNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid((short) -1, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Byte类型测试")
    class ByteTests {

        @Test
        @DisplayName("includeZero=true时，零值应返回true")
        void isValid_byteZero_includeZeroTrue_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid((byte) 0, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("includeZero=false时，零值应返回false")
        void isValid_byteZero_includeZeroFalse_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(false));

            boolean result = validator.isValid((byte) 0, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("正Byte值应返回true")
        void isValid_bytePositive_returnsTrue() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid((byte) 127, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("负Byte值应返回false")
        void isValid_byteNegative_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid((byte) -1, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("不支持类型测试")
    class UnsupportedTypeTests {

        @Test
        @DisplayName("String类型应返回false")
        void isValid_stringType_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid("123", context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Object类型应返回false")
        void isValid_objectType_returnsFalse() {
            validator.initialize(createNonNegativeAnnotation(true));

            boolean result = validator.isValid(new Object(), context);

            assertFalse(result);
        }
    }

    /**
     * 创建 NonNegative 注解的 Mock 对象
     *
     * @param includeZero 是否包含零
     * @return Mock 注解对象
     */
    private NonNegative createNonNegativeAnnotation(boolean includeZero) {
        return new NonNegative() {
            @Override
            public boolean includeZero() {
                return includeZero;
            }

            @Override
            public String message() {
                return "值必须为非负数";
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
                return NonNegative.class;
            }
        };
    }
}
