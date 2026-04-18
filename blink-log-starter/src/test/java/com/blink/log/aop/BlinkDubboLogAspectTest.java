package com.blink.log.aop;

import com.blink.log.config.LogProperties;
import com.blink.log.util.SensitiveUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BlinkDubboLogAspect 单元测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlinkDubboLogAspect 单元测试")
class BlinkDubboLogAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private LogProperties logProperties;
    private BlinkDubboLogAspect aspect;

    @BeforeEach
    void setUp() {
        logProperties = new LogProperties();
        aspect = new BlinkDubboLogAspect(logProperties);
    }

    @Nested
    @DisplayName("aroundDubboMethod 测试")
    class AroundDubboMethodTests {

        @Test
        @DisplayName("应该记录正常执行的日志")
        void shouldLogNormalExecution() throws Throwable {
            // given
            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{"testParam"});
            when(joinPoint.proceed()).thenReturn("testResult");

            // when
            Object result = aspect.aroundDubboMethod(joinPoint);

            // then
            assertThat(result).isEqualTo("testResult");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("应该记录异常执行的日志")
        void shouldLogExceptionExecution() throws Throwable {
            // given
            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{"testParam"});
            RuntimeException testException = new RuntimeException("测试异常");
            when(joinPoint.proceed()).thenThrow(testException);

            // when & then
            assertThatThrownBy(() -> aspect.aroundDubboMethod(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("测试异常");
        }

        @Test
        @DisplayName("应该正确处理空参数")
        void shouldHandleNullArgs() throws Throwable {
            // given
            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(null);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundDubboMethod(joinPoint);

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("应该正确处理空返回值")
        void shouldHandleNullResult() throws Throwable {
            // given
            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{"testParam"});
            when(joinPoint.proceed()).thenReturn(null);

            // when
            Object result = aspect.aroundDubboMethod(joinPoint);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("应该跳过标记了 @ConsoleLog 的方法")
        void shouldSkipConsoleLogAnnotatedMethod() throws Throwable {
            // given
            Method method = TestService.class.getMethod("consoleLogMethod");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundDubboMethod(joinPoint);

            // then
            assertThat(result).isEqualTo("result");
            // 不应该获取参数（因为跳过了日志记录逻辑）
            verify(joinPoint, never()).getArgs();
        }
    }

    @Nested
    @DisplayName("buildArgString 测试")
    class BuildArgStringTests {

        @Test
        @DisplayName("应该正确构建简单类型参数字符串")
        void shouldBuildSimpleTypeArgString() throws Throwable {
            // given
            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{"simpleString"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDubboMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("应该正确处理复杂对象参数")
        void shouldHandleComplexObjectArgs() throws Throwable {
            // given
            Method method = TestService.class.getMethod("complexMethod", TestDTO.class);
            TestDTO dto = new TestDTO();
            dto.setName("testName");
            dto.setPassword("secretPassword");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDubboMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("truncateIfNeeded 测试")
    class TruncateIfNeededTests {

        @Test
        @DisplayName("应该在开启 autoSkip 时截断超长字符串")
        void shouldTruncateWhenAutoSkipEnabled() throws Throwable {
            // given
            logProperties.getConsole().setAutoSkip(true);
            logProperties.getConsole().setUpperLimit(10);

            StringBuilder longParam = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longParam.append("a");
            }

            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{longParam.toString()});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDubboMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("应该在关闭 autoSkip 时不截断字符串")
        void shouldNotTruncateWhenAutoSkipDisabled() throws Throwable {
            // given
            logProperties.getConsole().setAutoSkip(false);

            StringBuilder longParam = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longParam.append("a");
            }

            Method method = TestService.class.getMethod("normalMethod", String.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{longParam.toString()});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDubboMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }
    }

    /**
     * 测试服务类
     */
    public static class TestService {
        public String normalMethod(String param) {
            return "result";
        }

        @com.blink.log.annotation.ConsoleLog
        public String consoleLogMethod() {
            return "result";
        }

        public String complexMethod(TestDTO dto) {
            return "result";
        }
    }

    /**
     * 测试 DTO
     */
    @lombok.Data
    public static class TestDTO {
        private String name;
        private String password;
    }
}
