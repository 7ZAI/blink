package com.blink.framework.core.interceptor;

import com.blink.framework.common.context.BlinkRequestContext;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import static com.blink.framework.common.constrant.SysConstant.*;

/**
 * BlinkRequestContextInterceptor 单元测试
 *
 * @author binblink
 */
@DisplayName("BlinkRequestContextInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
class BlinkRequestContextInterceptorTest {

    @InjectMocks
    private BlinkRequestContextInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        BlinkRequestContextHolder.clearContext();
        ReflectionTestUtils.setField(interceptor, "appName", "test-app");
    }

    @AfterEach
    void tearDown() {
        BlinkRequestContextHolder.clearContext();
    }

    @Nested
    @DisplayName("preHandle 方法测试")
    class PreHandleTest {

        @Test
        @DisplayName("正常设置完整上下文")
        void testPreHandle_设置完整上下文() throws Exception {
            // Arrange
            when(request.getHeader(X_BLINK_REQUEST_ID)).thenReturn("req-123");
            when(request.getHeader(X_BLINK_CLIENTIP)).thenReturn("192.168.1.1");
            when(request.getHeader(X_BLINK_CHANNEL)).thenReturn("web");
            when(request.getHeader(X_BLINK_USRID)).thenReturn("user-001");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("testuser");
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("trace-456");
            when(request.getHeader(X_BLINK_LOCALE)).thenReturn("zh_cn");

            // Act
            boolean result = interceptor.preHandle(request, response, null);

            // Assert
            assertThat(result).isTrue();

            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context).isNotNull();
            assertThat(context.getRequestId()).isEqualTo("req-123");
            assertThat(context.getClientIp()).isEqualTo("192.168.1.1");
            assertThat(context.getChannel()).isEqualTo("web");
            assertThat(context.getUserId()).isEqualTo("user-001");
            assertThat(context.getLoginName()).isEqualTo("testuser");
            assertThat(context.getTraceId()).isEqualTo("trace-456");
            assertThat(context.getLanguage()).isEqualTo("zh_cn");
            assertThat(context.getAppName()).isEqualTo("test-app");
            assertThat(context.getRequestDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Header为空时上下文字段为null")
        void testPreHandle_Header为空() throws Exception {
            // Arrange
            lenient().when(request.getHeader(anyString())).thenReturn(null);

            // Act
            boolean result = interceptor.preHandle(request, response, null);

            // Assert
            assertThat(result).isTrue();

            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context).isNotNull();
            assertThat(context.getRequestId()).isNull();
            assertThat(context.getClientIp()).isNull();
            assertThat(context.getChannel()).isNull();
            assertThat(context.getUserId()).isNull();
            assertThat(context.getLoginName()).isNull();
            assertThat(context.getTraceId()).isNull();
            assertThat(context.getLanguage()).isNull();
            assertThat(context.getAppName()).isEqualTo("test-app");
        }

        @Test
        @DisplayName("从Header正确读取各字段")
        void testPreHandle_从Header读取各字段() throws Exception {
            // Arrange
            when(request.getHeader(X_BLINK_REQUEST_ID)).thenReturn("request-id-value");
            when(request.getHeader(X_BLINK_CLIENTIP)).thenReturn("10.0.0.1");
            when(request.getHeader(X_BLINK_CHANNEL)).thenReturn("mobile");
            when(request.getHeader(X_BLINK_USRID)).thenReturn("usr-999");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("admin");
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("trace-id-value");
            when(request.getHeader(X_BLINK_LOCALE)).thenReturn("en_us");

            // Act
            interceptor.preHandle(request, response, null);

            // Assert
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context.getRequestId()).isEqualTo("request-id-value");
            assertThat(context.getClientIp()).isEqualTo("10.0.0.1");
            assertThat(context.getChannel()).isEqualTo("mobile");
            assertThat(context.getUserId()).isEqualTo("usr-999");
            assertThat(context.getLoginName()).isEqualTo("admin");
            assertThat(context.getTraceId()).isEqualTo("trace-id-value");
            assertThat(context.getLanguage()).isEqualTo("en_us");
        }

        @Test
        @DisplayName("AppName从配置注入")
        void testPreHandle_AppName注入() throws Exception {
            // Arrange
            ReflectionTestUtils.setField(interceptor, "appName", "my-service");
            lenient().when(request.getHeader(anyString())).thenReturn(null);

            // Act
            interceptor.preHandle(request, response, null);

            // Assert
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context.getAppName()).isEqualTo("my-service");
        }

        @Test
        @DisplayName("RequestDate设置为当前日期")
        void testPreHandle_RequestDate设置() throws Exception {
            // Arrange
            lenient().when(request.getHeader(anyString())).thenReturn(null);

            // Act
            interceptor.preHandle(request, response, null);

            // Assert
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context.getRequestDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("afterCompletion 方法测试")
    class AfterCompletionTest {

        @Test
        @DisplayName("请求完成后清理上下文")
        void testAfterCompletion_清理上下文() throws Exception {
            // Arrange - 先设置上下文
            when(request.getHeader(X_BLINK_REQUEST_ID)).thenReturn("req-123");
            interceptor.preHandle(request, response, null);
            assertThat(BlinkRequestContextHolder.getContext().getRequestId()).isEqualTo("req-123");

            // Act
            interceptor.afterCompletion(request, response, null, null);

            // Assert - 验证上下文已清理（获取到的是新空上下文）
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context.getRequestId()).isNull();
        }

        @Test
        @DisplayName("有异常时也能清理上下文")
        void testAfterCompletion_有异常时清理() throws Exception {
            // Arrange
            when(request.getHeader(X_BLINK_REQUEST_ID)).thenReturn("req-456");
            interceptor.preHandle(request, response, null);

            // Act
            interceptor.afterCompletion(request, response, null, new RuntimeException("test exception"));

            // Assert
            BlinkRequestContext context = BlinkRequestContextHolder.getContext();
            assertThat(context.getRequestId()).isNull();
        }

        @Test
        @DisplayName("多次调用afterCompletion不抛异常")
        void testAfterCompletion_多次调用() throws Exception {
            // Arrange
            interceptor.afterCompletion(request, response, null, null);

            // Act & Assert - 不应抛出异常
            interceptor.afterCompletion(request, response, null, null);
        }
    }
}
