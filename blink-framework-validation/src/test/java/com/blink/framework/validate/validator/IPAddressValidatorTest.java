package com.blink.framework.validate.validator;

import com.blink.framework.validate.annotation.ValidIPAddress;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IPAddressValidator IP地址校验器单元测试
 *
 * @author binblink
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class IPAddressValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private IPAddressValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IPAddressValidator();
    }

    @Nested
    @DisplayName("null值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("null值应返回true（由@NotNull处理）")
        void isValid_nullValue_returnsTrue() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid(null, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("IPv4格式校验测试")
    class IPv4Tests {

        @BeforeEach
        void setUp() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.IPV4, ValidIPAddress.TargetType.INDIVIDUAL));
        }

        @Test
        @DisplayName("合法IPv4地址应返回true")
        void isValid_validIPv4_returnsTrue() {
            assertTrue(validator.isValid("192.168.1.1", context));
            assertTrue(validator.isValid("0.0.0.0", context));
            assertTrue(validator.isValid("255.255.255.255", context));
            assertTrue(validator.isValid("10.0.0.1", context));
            assertTrue(validator.isValid("127.0.0.1", context));
        }

        @Test
        @DisplayName("非法IPv4地址应返回false")
        void isValid_invalidIPv4_returnsFalse() {
            // 超出范围
            assertFalse(validator.isValid("256.1.1.1", context));
            assertFalse(validator.isValid("1.256.1.1", context));

            // 格式错误
            assertFalse(validator.isValid("192.168.1", context));
            assertFalse(validator.isValid("192.168.1.1.1", context));
            assertFalse(validator.isValid("192.168.1.1a", context));

            // 空字符串
            assertFalse(validator.isValid("", context));

            // 包含字母
            assertFalse(validator.isValid("192.168.1.a", context));

            // 负数
            assertFalse(validator.isValid("192.168.-1.1", context));
        }

        @Test
        @DisplayName("IPv6地址在IPv4模式下应返回false")
        void isValid_ipv6InIpv4Mode_returnsFalse() {
            assertFalse(validator.isValid("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context));
            assertFalse(validator.isValid("::1", context));
        }
    }

    @Nested
    @DisplayName("IPv6格式校验测试")
    class IPv6Tests {

        @BeforeEach
        void setUp() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.IPV6, ValidIPAddress.TargetType.INDIVIDUAL));
        }

        @Test
        @DisplayName("合法IPv6地址应返回true")
        void isValid_validIPv6_returnsTrue() {
            // 完整格式
            assertTrue(validator.isValid("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context));
            // 压缩格式
            assertTrue(validator.isValid("2001:db8:85a3::8a2e:370:7334", context));
            // 本地回环
            assertTrue(validator.isValid("::1", context));
            // 全零压缩
            assertTrue(validator.isValid("::", context));
            // IPv4映射的IPv6地址
            assertTrue(validator.isValid("::ffff:192.168.1.1", context));
        }

        @Test
        @DisplayName("非法IPv6地址应返回false")
        void isValid_invalidIPv6_returnsFalse() {
            // 超出范围
            assertFalse(validator.isValid("gggg::1", context));
            // 格式错误
            assertFalse(validator.isValid("2001:db8:85a3::8a2e::7334", context));
            // 空字符串
            assertFalse(validator.isValid("", context));
            // 端口格式
            assertFalse(validator.isValid("[::1]", context));
        }

        @Test
        @DisplayName("IPv4地址在IPv6模式下应返回false")
        void isValid_ipv4InIpv6Mode_returnsFalse() {
            assertFalse(validator.isValid("192.168.1.1", context));
        }
    }

    @Nested
    @DisplayName("ALL类型校验测试（IPv4或IPv6均可）")
    class AllTypeTests {

        @BeforeEach
        void setUp() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.INDIVIDUAL));
        }

        @Test
        @DisplayName("合法IPv4地址应返回true")
        void isValid_validIPv4InAllMode_returnsTrue() {
            assertTrue(validator.isValid("192.168.1.1", context));
            assertTrue(validator.isValid("10.0.0.1", context));
        }

        @Test
        @DisplayName("合法IPv6地址应返回true")
        void isValid_validIPv6InAllMode_returnsTrue() {
            assertTrue(validator.isValid("2001:db8::1", context));
            assertTrue(validator.isValid("::1", context));
        }

        @Test
        @DisplayName("非法IP地址应返回false")
        void isValid_invalidIPInAllMode_returnsFalse() {
            assertFalse(validator.isValid("256.1.1.1", context));
            assertFalse(validator.isValid("gggg::1", context));
            assertFalse(validator.isValid("not-an-ip", context));
        }
    }

    @Nested
    @DisplayName("集合模式校验测试")
    class MultipleTargetTests {

        @BeforeEach
        void setUp() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.MULTIPLE));
        }

        @Test
        @DisplayName("所有IP均合法应返回true")
        void isValid_allValidIPs_returnsTrue() {
            List<String> ips = Arrays.asList(
                    "192.168.1.1",
                    "10.0.0.1",
                    "2001:db8::1",
                    "::1"
            );

            boolean result = validator.isValid(ips, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("存在一个非法IP应返回false")
        void isValid_oneInvalidIP_returnsFalse() {
            List<String> ips = Arrays.asList(
                    "192.168.1.1",
                    "256.1.1.1",
                    "10.0.0.1"
            );

            boolean result = validator.isValid(ips, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("空集合应返回true")
        void isValid_emptyCollection_returnsTrue() {
            boolean result = validator.isValid(Collections.emptyList(), context);

            assertTrue(result);
        }

        @Test
        @DisplayName("集合中包含非String元素应返回false")
        void isValid_collectionWithNonStringElements_returnsFalse() {
            List<Object> ips = Arrays.asList("192.168.1.1", 123);

            boolean result = validator.isValid(ips, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("类型错误处理测试")
    class TypeMismatchTests {

        @Test
        @DisplayName("单个模式下非String类型应返回false")
        void isValid_nonStringInIndividualMode_returnsFalse() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid(123, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("多个模式下非Collection类型应返回false")
        void isValid_nonCollectionInMultipleMode_returnsFalse() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.MULTIPLE));

            boolean result = validator.isValid("192.168.1.1", context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("空字符串应返回false")
        void isValid_emptyString_returnsFalse() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid("", context);

            assertFalse(result);
        }

        @Test
        @DisplayName("纯空白字符串应返回false")
        void isValid_blankString_returnsFalse() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.ALL, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid("   ", context);

            assertFalse(result);
        }

        @Test
        @DisplayName("IPv4边界值：0.0.0.0应返回true")
        void isValid_ipv4MinBoundary_returnsTrue() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.IPV4, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid("0.0.0.0", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("IPv4边界值：255.255.255.255应返回true")
        void isValid_ipv4MaxBoundary_returnsTrue() {
            validator.initialize(createValidIPAddressAnnotation(
                    ValidIPAddress.IPType.IPV4, ValidIPAddress.TargetType.INDIVIDUAL));

            boolean result = validator.isValid("255.255.255.255", context);

            assertTrue(result);
        }
    }

    /**
     * 创建 ValidIPAddress 注解的 Mock 对象
     *
     * @param ipType     IP类型
     * @param targetType 目标类型
     * @return Mock 注解对象
     */
    private ValidIPAddress createValidIPAddressAnnotation(
            ValidIPAddress.IPType ipType, ValidIPAddress.TargetType targetType) {
        return new ValidIPAddress() {
            @Override
            public IPType type() {
                return ipType;
            }

            @Override
            public TargetType targetType() {
                return targetType;
            }

            @Override
            public String message() {
                return "无效的IP地址";
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
                return ValidIPAddress.class;
            }
        };
    }
}
