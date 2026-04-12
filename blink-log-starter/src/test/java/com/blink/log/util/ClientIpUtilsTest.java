package com.blink.log.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ClientIpUtils 工具类测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientIpUtils 工具类测试")
class ClientIpUtilsTest {

    private static final String UNKNOWN = "unknown";

    @Mock
    private HttpServletRequest request;

    // ==================== getClientIp() 测试 ====================

    @Nested
    @DisplayName("getClientIp() 方法测试")
    class GetClientIpTest {

        @Test
        @DisplayName("request为null - 返回空字符串")
        void getClientIp_nullRequest_shouldReturnEmpty() {
            // when
            String result = ClientIpUtils.getClientIp(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("X-Forwarded-For 头获取IP")
        void getClientIp_xForwardedFor_shouldReturnIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("X-Forwarded-For 包含多个IP - 取第一个")
        void getClientIp_xForwardedForMultiple_shouldReturnFirst() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 192.168.1.101, 192.168.1.102");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("X-Forwarded-For 为 unknown - 继续检查下一个头")
        void getClientIp_xForwardedForUnknown_shouldCheckNextHeader() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(UNKNOWN);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.101");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.101");
        }

        @Test
        @DisplayName("X-Forwarded-For 为空 - 继续检查下一个头")
        void getClientIp_xForwardedForEmpty_shouldCheckNextHeader() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.101");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.101");
        }

        @Test
        @DisplayName("Proxy-Client-IP 头获取IP")
        void getClientIp_proxyClientIp_shouldReturnIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.102");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.102");
        }

        @Test
        @DisplayName("WL-Proxy-Client-IP 头获取IP")
        void getClientIp_wlProxyClientIp_shouldReturnIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("192.168.1.103");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.103");
        }

        @Test
        @DisplayName("HTTP_CLIENT_IP 头获取IP")
        void getClientIp_httpClientIp_shouldReturnIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("192.168.1.104");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.104");
        }

        @Test
        @DisplayName("HTTP_X_FORWARDED_FOR 头获取IP")
        void getClientIp_httpXForwardedFor_shouldReturnIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("192.168.1.105");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.105");
        }

        @Test
        @DisplayName("所有头都无效 - 使用 getRemoteAddr()")
        void getClientIp_allHeadersInvalid_shouldUseRemoteAddr() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.200");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.200");
        }

        @Test
        @DisplayName("所有头都为 unknown - 使用 getRemoteAddr()")
        void getClientIp_allHeadersUnknown_shouldUseRemoteAddr() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(UNKNOWN);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(UNKNOWN);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(UNKNOWN);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(UNKNOWN);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(UNKNOWN);
            when(request.getRemoteAddr()).thenReturn("192.168.1.201");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.201");
        }

        @Test
        @DisplayName("IPv6地址")
        void getClientIp_ipv6Address_shouldReturnCorrectly() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("fe80::1");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("fe80::1");
        }

        @Test
        @DisplayName("X-Forwarded-For 多IP带空格")
        void getClientIp_multipleIpsWithSpaces_shouldTrimCorrectly() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("  192.168.1.100 , 192.168.1.101  ");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("本地回环地址")
        void getClientIp_loopbackAddress_shouldReturnCorrectly() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("0:0:0:0:0:0:0:1 IPv6回环地址")
        void getClientIp_ipv6LoopbackAddress_shouldReturnCorrectly() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

            // when
            String result = ClientIpUtils.getClientIp(request);

            // then
            assertThat(result).isEqualTo("0:0:0:0:0:0:0:1");
        }
    }
}
