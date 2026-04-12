package com.blink.log.aop;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.log.annotation.ConsoleLog;
import com.blink.log.config.LogProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BlinkControllerLogAspect 切面测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BlinkControllerLogAspect 切面测试")
class BlinkControllerLogAspectTest {

    @Mock
    private LogProperties logProperties;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private BlinkControllerLogAspect aspect;

    @BeforeEach
    void setUp() {
        LogProperties.LogConsole consoleConfig = new LogProperties.LogConsole();
        consoleConfig.setEnableControllerLog(true);
        consoleConfig.setUpperLimit(1000);
        consoleConfig.setAutoSkip(false);
        consoleConfig.setEnableSensitive(false);
        when(logProperties.getConsole()).thenReturn(consoleConfig);

        aspect = new BlinkControllerLogAspect(logProperties);
    }

    @Nested
    @DisplayName("正常流程测试")
    class NormalFlowTest {

        @Test
        @DisplayName("方法正常执行 - 日志输出")
        void aroundControllerMethod_normalExecution_shouldLog() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundControllerMethod(joinPoint);

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("方法带@ConsoleLog注解 - 跳过切面逻辑")
        void aroundControllerMethod_withConsoleLog_shouldSkip() throws Throwable {
            // given
            setupMockJoinPoint("annotatedMethod", true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundControllerMethod(joinPoint);

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("方法抛异常 - 记录异常日志")
        void aroundControllerMethod_throwsException_shouldLogError() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("测试异常"));

            // when & then
            assertThatThrownBy(() -> aspect.aroundControllerMethod(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("测试异常");
        }
    }

    @Nested
    @DisplayName("参数处理测试")
    class ArgumentHandlingTest {

        @Test
        @DisplayName("无参数方法")
        void aroundControllerMethod_noArgs_shouldLogNoParams() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("null参数")
        void aroundControllerMethod_nullArg_shouldLogNull() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.getArgs()).thenReturn(new Object[]{null});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("简单类型参数")
        void aroundControllerMethod_simpleArgs_shouldLogDirectly() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"string", 123, true});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("RequestDTO参数")
        void aroundControllerMethod_requestDtoArg_shouldLogAsString() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            RequestDTO<String> requestDTO = new RequestDTO<>();
            requestDTO.setBody("testBody");
            when(joinPoint.getArgs()).thenReturn(new Object[]{requestDTO});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("截断测试")
    class TruncationTest {

        @Test
        @DisplayName("autoSkip=true - 超长参数截断")
        void aroundControllerMethod_autoSkipEnabled_shouldTruncate() throws Throwable {
            // given
            LogProperties.LogConsole consoleConfig = new LogProperties.LogConsole();
            consoleConfig.setEnableControllerLog(true);
            consoleConfig.setUpperLimit(100);
            consoleConfig.setAutoSkip(true);
            consoleConfig.setEnableSensitive(false);
            when(logProperties.getConsole()).thenReturn(consoleConfig);

            aspect = new BlinkControllerLogAspect(logProperties);

            setupMockJoinPoint("normalMethod", false);
            String longString = "a".repeat(200);
            when(joinPoint.getArgs()).thenReturn(new Object[]{longString});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("autoSkip=false - 不截断")
        void aroundControllerMethod_autoSkipDisabled_shouldNotTruncate() throws Throwable {
            // given
            LogProperties.LogConsole consoleConfig = new LogProperties.LogConsole();
            consoleConfig.setEnableControllerLog(true);
            consoleConfig.setUpperLimit(100);
            consoleConfig.setAutoSkip(false);
            consoleConfig.setEnableSensitive(false);
            when(logProperties.getConsole()).thenReturn(consoleConfig);

            aspect = new BlinkControllerLogAspect(logProperties);

            setupMockJoinPoint("normalMethod", false);
            String longString = "a".repeat(200);
            when(joinPoint.getArgs()).thenReturn(new Object[]{longString});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundControllerMethod(joinPoint);

            // then
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("返回值处理测试")
    class ReturnValueTest {

        @Test
        @DisplayName("null返回值")
        void aroundControllerMethod_nullResult_shouldLogNull() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            when(joinPoint.proceed()).thenReturn(null);

            // when
            Object result = aspect.aroundControllerMethod(joinPoint);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ResponseDTO返回值")
        void aroundControllerMethod_responseDtoResult_shouldLogBody() throws Throwable {
            // given
            setupMockJoinPoint("normalMethod", false);
            ResponseDTO<String> responseDTO = ResponseDTO.newSuccessInstance("testBody");
            when(joinPoint.proceed()).thenReturn(responseDTO);

            // when
            Object result = aspect.aroundControllerMethod(joinPoint);

            // then
            assertThat(result).isEqualTo(responseDTO);
        }
    }

    // ==================== 辅助方法 ====================

    private void setupMockJoinPoint(String methodName, boolean hasConsoleLog) throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getTarget()).thenReturn(new TestController());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        Method method = hasConsoleLog
                ? TestController.class.getDeclaredMethod(methodName)
                : TestController.class.getDeclaredMethod(methodName);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    // 测试用 Controller
    static class TestController {
        public void normalMethod() {
        }

        @ConsoleLog
        public void annotatedMethod() {
        }
    }
}
