package com.blink.log.aop;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.log.annotation.RecordLog;
import com.blink.log.config.LogProperties;
import com.blink.log.constant.LogType;
import com.blink.log.function.LogConverter;
import com.blink.log.function.LogEnabledFunction;
import com.blink.log.function.LogPersistFunction;
import com.blink.log.function.UserInfoProviderFunction;
import com.blink.log.model.OperationLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * OperationLogAspect 切面测试
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OperationLogAspect 切面测试")
class OperationLogAspectTest {

    @Mock
    private LogProperties logProperties;

    @Mock
    private LogPersistFunction<String> logPersistFunction;

    @Mock
    private LogEnabledFunction logEnabledFunction;

    @Mock
    private UserInfoProviderFunction userInfoProviderFunction;

    @Mock
    private LogConverter<String> logConverter;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    private OperationLogAspect<String> aspect;

    @BeforeEach
    void setUp() {
        // 设置默认配置
        LogProperties.LogRecord recordConfig = new LogProperties.LogRecord();
        recordConfig.setSaveRequest(true);
        recordConfig.setSaveResponse(true);
        recordConfig.setMaxRequestLength(4000);
        recordConfig.setMaxResponseLength(4000);
        recordConfig.setMaxErrorMsgLength(500);
        recordConfig.setMaxUserAgentLength(500);
        when(logProperties.getRecord()).thenReturn(recordConfig);

        aspect = new OperationLogAspect<>(
                logProperties,
                logPersistFunction,
                logEnabledFunction,
                userInfoProviderFunction,
                logConverter
        );
    }

    // ==================== 正常流程测试 ====================

    @Nested
    @DisplayName("正常流程测试")
    class NormalFlowTest {

        @Test
        @DisplayName("方法正常执行 - 日志记录成功")
        void around_normalExecution_shouldRecordLog() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");
            doNothing().when(logPersistFunction).persist(any());

            // when
            Object result = aspect.around(joinPoint);

            // then
            assertThat(result).isNotNull();
            verify(logPersistFunction).persist(any());
        }

        @Test
        @DisplayName("日志开关关闭 - 跳过日志记录")
        void around_logDisabled_shouldSkipLog() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(false);

            // when
            Object result = aspect.around(joinPoint);

            // then
            assertThat(result).isNotNull();
            verify(logPersistFunction, never()).persist(any());
        }

        @Test
        @DisplayName("LogEnabledFunction为null - 默认启用日志")
        void around_nullEnabledFunction_shouldDefaultEnable() throws Throwable {
            // given
            aspect = new OperationLogAspect<>(
                    logProperties,
                    logPersistFunction,
                    null,  // LogEnabledFunction 为 null
                    userInfoProviderFunction,
                    logConverter
            );
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            // when
            Object result = aspect.around(joinPoint);

            // then
            assertThat(result).isNotNull();
            verify(logPersistFunction).persist(any());
        }
    }

    // ==================== 用户信息测试 ====================

    @Nested
    @DisplayName("用户信息测试")
    class UserInfoTest {

        @Test
        @DisplayName("获取用户信息成功")
        void around_userInfoSuccess_shouldSetUserInfo() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(userInfoProviderFunction.getCurrentUser())
                    .thenReturn(new UserInfoProviderFunction.UserInfo(1, "admin"));
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);

            // when
            aspect.around(joinPoint);

            // then
            verify(logConverter).convert(captor.capture());
            OperationLogRecord record = captor.getValue();
            assertThat(record.getUserId()).isEqualTo(1);
            assertThat(record.getLoginName()).isEqualTo("admin");
        }

        @Test
        @DisplayName("用户信息获取失败 - 不影响主流程")
        void around_userInfoFails_shouldContinue() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(userInfoProviderFunction.getCurrentUser()).thenThrow(new RuntimeException("获取用户失败"));
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            // when
            Object result = aspect.around(joinPoint);

            // then - 不抛异常，继续执行
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("UserInfoProviderFunction为null - 跳过用户信息")
        void around_nullUserInfoProvider_shouldSkipUserInfo() throws Throwable {
            // given
            aspect = new OperationLogAspect<>(
                    logProperties,
                    logPersistFunction,
                    logEnabledFunction,
                    null,  // UserInfoProviderFunction 为 null
                    logConverter
            );
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);

            // when
            aspect.around(joinPoint);

            // then
            verify(logConverter).convert(captor.capture());
            OperationLogRecord record = captor.getValue();
            assertThat(record.getUserId()).isNull();
            assertThat(record.getLoginName()).isNull();
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("方法抛出异常 - 记录异常信息")
        void around_methodThrowsException_shouldRecordError() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("业务异常"));
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("业务异常");

            verify(logConverter).convert(captor.capture());
            OperationLogRecord record = captor.getValue();
            assertThat(record.getExecuteStatus()).isEqualTo(1); // 失败状态
            assertThat(record.getErrorMsg()).contains("业务异常");
        }

        @Test
        @DisplayName("异常信息超长 - 截断处理")
        void around_longErrorMessage_shouldTruncate() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            String longMessage = "a".repeat(1000);
            when(joinPoint.proceed()).thenThrow(new RuntimeException(longMessage));
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");

            ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint))
                    .isInstanceOf(RuntimeException.class);

            verify(logConverter).convert(captor.capture());
            OperationLogRecord record = captor.getValue();
            assertThat(record.getErrorMsg()).hasSize(500); // maxErrorMsgLength
        }

        @Test
        @DisplayName("LogPersistFunction为null - 跳过持久化")
        void around_nullPersistFunction_shouldSkipPersist() throws Throwable {
            // given
            aspect = new OperationLogAspect<>(
                    logProperties,
                    null,  // LogPersistFunction 为 null
                    logEnabledFunction,
                    userInfoProviderFunction,
                    logConverter
            );
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);

            // when
            Object result = aspect.around(joinPoint);

            // then
            assertThat(result).isNotNull();
            verify(logConverter, never()).convert(any());
        }

        @Test
        @DisplayName("LogConverter返回null - 跳过持久化")
        void around_converterReturnsNull_shouldSkipPersist() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn(null);

            // when
            Object result = aspect.around(joinPoint);

            // then
            assertThat(result).isNotNull();
            verify(logPersistFunction, never()).persist(any());
        }

        @Test
        @DisplayName("LogPersistFunction抛异常 - 不影响主流程")
        void around_persistFails_shouldNotAffectMainFlow() throws Throwable {
            // given
            setupMockJoinPoint("testMethod");
            when(joinPoint.proceed()).thenReturn(ResponseDTO.newSuccessInstance());
            when(logEnabledFunction.isEnabled(any())).thenReturn(true);
            when(logConverter.convert(any(OperationLogRecord.class))).thenReturn("logEntity");
            doThrow(new RuntimeException("持久化失败")).when(logPersistFunction).persist(any());

            // when
            Object result = aspect.around(joinPoint);

            // then - 不抛异常，返回正常结果
            assertThat(result).isNotNull();
        }
    }

    // ==================== 辅助方法 ====================

    private void setupMockJoinPoint(String methodName) throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getMethodWithAnnotation(methodName));
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.getTarget()).thenReturn(new TestController());
    }

    private Method getMethodWithAnnotation(String methodName) throws NoSuchMethodException {
        return TestController.class.getDeclaredMethod(methodName);
    }

    // 测试用 Controller
    static class TestController {
        @RecordLog(type = LogType.OPERATION, description = "测试操作")
        public void testMethod() {
        }
    }
}
