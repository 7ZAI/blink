package com.blink.framework.core.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import static com.blink.framework.common.constrant.SysConstant.*;

/**
 * LogMdcInterceptor 单元测试
 *
 * @author binblink
 */
@DisplayName("LogMdcInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
class LogMdcInterceptorTest {

    private LogMdcInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new LogMdcInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("preHandle 方法测试")
    class PreHandleTest {

        @Test
        @DisplayName("从Header获取traceId并设置到MDC")
        void testPreHandle_从Header获取traceId() {
            // Arrange
            String expectedTraceId = "trace-123-456";
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn(expectedTraceId);
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("testuser");

            // Act
            boolean result = interceptor.preHandle(request, response, null);

            // Assert
            assertThat(result).isTrue();
            assertThat(MDC.get("traceId")).isEqualTo(expectedTraceId);
        }

        @Test
        @DisplayName("Header无traceId时自动生成UUID")
        void testPreHandle_Header无traceId自动生成() {
            // Arrange
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn(null);
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("testuser");

            // Act
            boolean result = interceptor.preHandle(request, response, null);

            // Assert
            assertThat(result).isTrue();
            String generatedTraceId = MDC.get("traceId");
            assertThat(generatedTraceId).isNotNull();
            assertThat(generatedTraceId).hasSize(32); // UUID去掉横杠后32位
            assertThat(generatedTraceId).doesNotContain("-");
        }

        @Test
        @DisplayName("Header为空字符串时自动生成traceId")
        void testPreHandle_Header为空字符串自动生成() {
            // Arrange
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("testuser");

            // Act
            boolean result = interceptor.preHandle(request, response, null);

            // Assert
            assertThat(result).isTrue();
            String generatedTraceId = MDC.get("traceId");
            assertThat(generatedTraceId).isNotNull();
            assertThat(generatedTraceId).hasSize(32);
        }

        @Test
        @DisplayName("设置userName到MDC")
        void testPreHandle_设置userName到MDC() {
            // Arrange
            String expectedUserName = "admin";
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("trace-123");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn(expectedUserName);

            // Act
            interceptor.preHandle(request, response, null);

            // Assert
            assertThat(MDC.get("userName")).isEqualTo(expectedUserName);
        }

        @Test
        @DisplayName("userName为null时MDC设置为null")
        void testPreHandle_userName为null() {
            // Arrange
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("trace-123");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn(null);

            // Act
            interceptor.preHandle(request, response, null);

            // Assert
            assertThat(MDC.get("userName")).isNull();
        }
    }

    @Nested
    @DisplayName("afterCompletion 方法测试")
    class AfterCompletionTest {

        @Test
        @DisplayName("清空MDC")
        void testAfterCompletion_清空MDC() {
            // Arrange - 先设置MDC值
            MDC.put("traceId", "test-trace");
            MDC.put("userName", "testuser");
            assertThat(MDC.get("traceId")).isNotNull();

            // Act
            interceptor.afterCompletion(request, response, null, null);

            // Assert
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("userName")).isNull();
        }

        @Test
        @DisplayName("有异常时也能清空MDC")
        void testAfterCompletion_有异常时清空() {
            // Arrange
            MDC.put("traceId", "test-trace");

            // Act
            interceptor.afterCompletion(request, response, null, new RuntimeException("test"));

            // Assert
            assertThat(MDC.get("traceId")).isNull();
        }

        @Test
        @DisplayName("MDC为空时调用不抛异常")
        void testAfterCompletion_MDC为空时不抛异常() {
            // Arrange - MDC已清空
            MDC.clear();

            // Act & Assert - 不应抛出异常
            interceptor.afterCompletion(request, response, null, null);
        }

        @Test
        @DisplayName("多次调用不抛异常")
        void testAfterCompletion_多次调用() {
            // Arrange
            interceptor.afterCompletion(request, response, null, null);

            // Act & Assert - 不应抛出异常
            interceptor.afterCompletion(request, response, null, null);
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullFlowTest {

        @Test
        @DisplayName("完整请求流程 - preHandle到afterCompletion")
        void test完整请求流程() {
            // Arrange
            when(request.getHeader(X_BLINK_TRACE_ID)).thenReturn("flow-trace-id");
            when(request.getHeader(X_BLINK_LOGINNAME)).thenReturn("flowuser");

            // Act - preHandle
            boolean preHandleResult = interceptor.preHandle(request, response, null);

            // Assert - 验证MDC已设置
            assertThat(preHandleResult).isTrue();
            assertThat(MDC.get("traceId")).isEqualTo("flow-trace-id");
            assertThat(MDC.get("userName")).isEqualTo("flowuser");

            // Act - afterCompletion
            interceptor.afterCompletion(request, response, null, null);

            // Assert - 验证MDC已清空
            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("userName")).isNull();
        }
    }
}
