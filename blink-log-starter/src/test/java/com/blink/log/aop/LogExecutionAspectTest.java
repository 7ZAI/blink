package com.blink.log.aop;

import com.blink.log.annotation.ConsoleLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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
import static org.mockito.Mockito.*;

/**
 * LogExecutionAspect 切面测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogExecutionAspect 切面测试")
class LogExecutionAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private final LogExecutionAspect aspect = new LogExecutionAspect();

    @Nested
    @DisplayName("正常流程测试")
    class NormalFlowTest {

        @Test
        @DisplayName("默认配置 - 记录请求、响应、耗时")
        void logExecution_defaultConfig_shouldLogAll() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("只记录请求")
        void logExecution_onlyRequest_shouldLogRequest() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, false, false, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("只记录响应")
        void logExecution_onlyResponse_shouldLogResponse() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(false, true, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("只记录耗时")
        void logExecution_onlyCostTime_shouldLogCostTime() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(false, false, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }
    }

    @Nested
    @DisplayName("日志级别测试")
    class LogLevelTest {

        @Test
        @DisplayName("DEBUG级别")
        void logExecution_debugLevel_shouldUseDebugLevel() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.DEBUG);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.DEBUG));

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("INFO级别")
        void logExecution_infoLevel_shouldUseInfoLevel() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("WARN级别")
        void logExecution_warnLevel_shouldUseWarnLevel() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.WARN);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.WARN));

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("ERROR级别")
        void logExecution_errorLevel_shouldUseErrorLevel() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.ERROR);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.ERROR));

            // then
            assertThat(result).isEqualTo("result");
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("方法抛异常 - 记录异常日志")
        void logExecution_throwsException_shouldLogError() throws Throwable {
            // given
            setupMockJoinPoint(true, true, true, ConsoleLog.LogLevel.INFO);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("测试异常"));

            // when & then
            assertThatThrownBy(() -> aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.INFO)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("测试异常");
        }
    }

    @Nested
    @DisplayName("参数处理测试")
    class ArgumentHandlingTest {

        @Test
        @DisplayName("有参数")
        void logExecution_withArgs_shouldLogParams() throws Throwable {
            // given
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 123});
            when(methodSignature.getMethod()).thenReturn(TestService.class.getDeclaredMethod("methodWithArgs", String.class, int.class));
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("无参数")
        void logExecution_noArgs_shouldHandleCorrectly() throws Throwable {
            // given
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
            when(methodSignature.getMethod()).thenReturn(TestService.class.getDeclaredMethod("noArgMethod"));
            when(methodSignature.getParameterNames()).thenReturn(new String[0]);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.logExecution(joinPoint, createConsoleLog(true, true, true, ConsoleLog.LogLevel.INFO));

            // then
            assertThat(result).isEqualTo("result");
        }
    }

    // ==================== 辅助方法 ====================

    private void setupMockJoinPoint(boolean logRequest, boolean logResponse, boolean logCostTime, ConsoleLog.LogLevel level)
            throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(methodSignature.getMethod()).thenReturn(TestService.class.getDeclaredMethod("noArgMethod"));
        when(methodSignature.getParameterNames()).thenReturn(new String[0]);
    }

    private ConsoleLog createConsoleLog(boolean logRequest, boolean logResponse, boolean logCostTime, ConsoleLog.LogLevel level) {
        return new ConsoleLog() {
            @Override
            public boolean logRequest() {
                return logRequest;
            }

            @Override
            public boolean logResponse() {
                return logResponse;
            }

            @Override
            public boolean logCostTime() {
                return logCostTime;
            }

            @Override
            public LogLevel level() {
                return level;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ConsoleLog.class;
            }
        };
    }

    // 测试用 Service
    static class TestService {
        @ConsoleLog
        public void noArgMethod() {
        }

        @ConsoleLog
        public void methodWithArgs(String param1, int param2) {
        }
    }
}
