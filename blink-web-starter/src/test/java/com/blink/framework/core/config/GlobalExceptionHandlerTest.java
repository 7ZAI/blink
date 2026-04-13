package com.blink.framework.core.config;

import com.blink.framework.common.constrant.ResponseMsgType;
import com.blink.framework.common.context.BlinkRequestContext;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.exception.ErrMsgProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 单元测试
 *
 * @author binblink
 */
@DisplayName("GlobalExceptionHandler 单元测试")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private ErrMsgProvider errMsgProvider;

    @BeforeEach
    void setUp() {
        BlinkRequestContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        BlinkRequestContextHolder.clearContext();
    }

    @Nested
    @DisplayName("handleBlinkException 方法测试")
    class HandleBlinkExceptionTest {

        @Test
        @DisplayName("业务异常返回正确响应")
        void testHandleBlinkException_业务异常返回正确响应() {
            // Arrange
            BlinkException exception = mock(BlinkException.class);
            when(exception.getMessage()).thenReturn("BUSS0001");
            when(exception.isBusinessException()).thenReturn(true);
            when(errMsgProvider.getErrMsg("BUSS0001", "zh_cn")).thenReturn("操作失败");

            // 设置语言
            BlinkRequestContext context = new BlinkRequestContext();
            context.setLanguage("zh_cn");
            BlinkRequestContextHolder.setContext(context);

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleBlinkException(exception);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgCode()).isEqualTo("BUSS0001");
            assertThat(result.getMsgInfo()).isEqualTo("操作失败");
            assertThat(result.getMsgType()).isEqualTo(ResponseMsgType.BUSINESS_ERR.getType());
            verify(exception, times(1)).isBusinessException();
        }

        @Test
        @DisplayName("系统异常返回正确响应")
        void testHandleBlinkException_系统异常返回正确响应() {
            // Arrange
            BlinkException exception = mock(BlinkException.class);
            when(exception.getMessage()).thenReturn("GATE0001");
            when(exception.isBusinessException()).thenReturn(false);
            when(errMsgProvider.getErrMsg("GATE0001", "en_us")).thenReturn("System error");

            BlinkRequestContext context = new BlinkRequestContext();
            context.setLanguage("en_us");
            BlinkRequestContextHolder.setContext(context);

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleBlinkException(exception);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgCode()).isEqualTo("GATE0001");
            assertThat(result.getMsgInfo()).isEqualTo("System error");
            assertThat(result.getMsgType()).isNotEqualTo(ResponseMsgType.BUSINESS_ERR.getType());
        }

        @Test
        @DisplayName("空语言返回默认中文")
        void testHandleBlinkException_空语言返回默认中文() {
            // Arrange
            BlinkException exception = mock(BlinkException.class);
            when(exception.getMessage()).thenReturn("BUSS0002");
            when(exception.isBusinessException()).thenReturn(true);
            when(errMsgProvider.getErrMsg("BUSS0002", "zh_cn")).thenReturn("中文错误");

            // 不设置语言

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleBlinkException(exception);

            // Assert
            assertThat(result.getMsgInfo()).isEqualTo("中文错误");
            verify(errMsgProvider).getErrMsg("BUSS0002", "zh_cn");
        }

        @Test
        @DisplayName("语言格式转换 - 横杠转下划线")
        void testHandleBlinkException_语言格式转换() {
            // Arrange
            BlinkException exception = mock(BlinkException.class);
            when(exception.getMessage()).thenReturn("BUSS0003");
            when(exception.isBusinessException()).thenReturn(true);
            when(errMsgProvider.getErrMsg("BUSS0003", "zh_cn")).thenReturn("转换后的语言");

            BlinkRequestContext context = new BlinkRequestContext();
            context.setLanguage("zh-cn"); // 使用横杠格式
            BlinkRequestContextHolder.setContext(context);

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleBlinkException(exception);

            // Assert
            verify(errMsgProvider).getErrMsg("BUSS0003", "zh_cn"); // 验证转换为了下划线
        }
    }

    @Nested
    @DisplayName("handleException (MethodArgumentNotValidException) 方法测试")
    class HandleMethodArgumentNotValidExceptionTest {

        @Test
        @DisplayName("参数校验异常取第一个错误")
        void testHandleException_参数校验异常取第一个错误() {
            // Arrange
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("object", "fieldName", "PARAM0001");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));
            when(errMsgProvider.getErrMsg("PARAM0001", "zh_cn")).thenReturn("参数错误");

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleException(exception);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgCode()).isEqualTo("PARAM0001");
            assertThat(result.getMsgInfo()).isEqualTo("参数错误");
            assertThat(result.getMsgType()).isEqualTo(ResponseMsgType.BUSINESS_ERR.getType());
        }

        @Test
        @DisplayName("多个参数校验异常只取第一个")
        void testHandleException_多个参数校验异常只取第一个() {
            // Arrange
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError error1 = new FieldError("object", "field1", "PARAM0001");
            FieldError error2 = new FieldError("object", "field2", "PARAM0002");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));
            when(errMsgProvider.getErrMsg("PARAM0001", "zh_cn")).thenReturn("第一个错误");

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleException(exception);

            // Assert
            assertThat(result.getMsgCode()).isEqualTo("PARAM0001");
            assertThat(result.getMsgInfo()).isEqualTo("第一个错误");
        }
    }

    @Nested
    @DisplayName("handleNoResourceFoundException 方法测试")
    class HandleNoResourceFoundExceptionTest {

        @Test
        @DisplayName("资源未找到返回404响应")
        void testHandleNoResourceFoundException_返回404响应() {
            // Arrange
            NoResourceFoundException exception = mock(NoResourceFoundException.class);
            when(exception.getResourcePath()).thenReturn("/api/not-exist");
            when(errMsgProvider.getErrMsg(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode(), "zh_cn"))
                    .thenReturn("资源未找到");

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleNoResourceFoundException(exception);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgCode()).isEqualTo(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode());
            assertThat(result.getMsgInfo()).isEqualTo("资源未找到");
        }
    }

    @Nested
    @DisplayName("handleAllOtherExceptions 方法测试")
    class HandleAllOtherExceptionsTest {

        @Test
        @DisplayName("未知异常返回默认响应")
        void testHandleAllOtherExceptions_未知异常返回默认响应() {
            // Arrange
            Exception exception = new RuntimeException("Unknown error");
            when(errMsgProvider.getErrMsg(anyString(), anyString())).thenReturn("系统繁忙");

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleAllOtherExceptions(exception);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgInfo()).isEqualTo("系统繁忙");
        }

        @Test
        @DisplayName("NullPointerException异常处理")
        void testHandleAllOtherExceptions_NullPointerException() {
            // Arrange
            NullPointerException npe = new NullPointerException("Null value");
            when(errMsgProvider.getErrMsg(anyString(), anyString())).thenReturn("系统错误");

            // Act
            ResponseDTO<EmptyBody> result = exceptionHandler.handleAllOtherExceptions(npe);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMsgInfo()).isEqualTo("系统错误");
        }
    }
}
