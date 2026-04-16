package com.blink.framework.common.exception;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * BlinkException 单元测试
 * <p>
 * 测试覆盖：
 * 1. 各种构造方法
 * 2. 静态工厂方法
 * 3. 异常属性获取
 * 4. 异常链传递
 *
 * @author binblink
 */
@UnitTest
@DisplayName("BlinkException 自定义异常类测试")
class BlinkExceptionTest extends BlinkUnitTest {

    private static final String TEST_ERROR_CODE = "TEST0001";

    // ==================== 构造方法测试 ====================

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("无参构造应该使用默认系统错误码")
        void shouldUseDefaultSystemErrorCode() {
            // when
            BlinkException exception = new BlinkException();

            // then
            assertThat(exception.getCode()).isEqualTo(BlinkErrorCodeEnum.SYS_ERROR.getCode());
            assertThat(exception.getMessage()).isEqualTo(BlinkErrorCodeEnum.SYS_ERROR.getCode());
        }

        @Test
        @DisplayName("单参数构造应该设置错误码")
        void shouldSetErrorCodeWithSingleParam() {
            // when
            BlinkException exception = new BlinkException(TEST_ERROR_CODE);

            // then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
            assertThat(exception.getMessage()).isEqualTo(TEST_ERROR_CODE);
        }

        @Test
        @DisplayName("双参数构造应该设置消息和错误码")
        void shouldSetMessageAndErrorCode() {
            // given
            String errorMessage = "测试错误消息";

            // when
            BlinkException exception = new BlinkException(errorMessage, TEST_ERROR_CODE);

            // then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getErrMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("三参数构造应该设置消息、原因和错误码")
        void shouldSetMessageCauseAndErrorCode() {
            // given
            String errorMessage = "测试错误消息";
            Throwable cause = new RuntimeException("原始异常");

            // when
            BlinkException exception = new BlinkException(errorMessage, cause, TEST_ERROR_CODE);

            // then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getErrMessage()).isEqualTo(errorMessage);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("错误码和业务标识构造应该正确设置")
        void shouldSetErrorCodeAndBusinessFlag() {
            // when
            BlinkException exception = new BlinkException(TEST_ERROR_CODE, true);

            // then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
            assertThat(exception.isBusinessException()).isTrue();
        }

        @Test
        @DisplayName("原因和错误码构造应该正确设置")
        void shouldSetCauseAndErrorCode() {
            // given
            Throwable cause = new RuntimeException("原始异常");

            // when
            BlinkException exception = new BlinkException(cause, TEST_ERROR_CODE);

            // then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    // ==================== 静态工厂方法测试 ====================

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("throwException应该抛出带默认错误码的异常")
        void shouldThrowExceptionWithDefaultCode() {
            // when & then
            assertThatThrownBy(() -> BlinkException.throwException())
                    .isInstanceOf(BlinkException.class)
                    .hasMessage(BlinkErrorCodeEnum.SYS_ERROR.getCode());
        }

        @Test
        @DisplayName("throwException(code)应该抛出带指定错误码的异常")
        void shouldThrowExceptionWithSpecifiedCode() {
            // when & then
            assertThatThrownBy(() -> BlinkException.throwException(TEST_ERROR_CODE))
                    .isInstanceOf(BlinkException.class)
                    .satisfies(ex -> {
                        BlinkException blinkEx = (BlinkException) ex;
                        assertThat(blinkEx.getCode()).isEqualTo(TEST_ERROR_CODE);
                    });
        }

        @Test
        @DisplayName("throwBusinessException()应该抛出默认业务异常")
        void shouldThrowDefaultBusinessException() {
            // when & then
            assertThatThrownBy(() -> BlinkException.throwBusinessException())
                    .isInstanceOf(BlinkException.class)
                    .satisfies(ex -> {
                        BlinkException blinkEx = (BlinkException) ex;
                        assertThat(blinkEx.getCode()).isEqualTo(BlinkErrorCodeEnum.BUSINESS_ERROR.getCode());
                        assertThat(blinkEx.isBusinessException()).isTrue();
                    });
        }

        @Test
        @DisplayName("throwBusinessException(code)应该抛出指定错误码的业务异常")
        void shouldThrowBusinessExceptionWithSpecifiedCode() {
            // when & then
            assertThatThrownBy(() -> BlinkException.throwBusinessException(TEST_ERROR_CODE))
                    .isInstanceOf(BlinkException.class)
                    .satisfies(ex -> {
                        BlinkException blinkEx = (BlinkException) ex;
                        assertThat(blinkEx.getCode()).isEqualTo(TEST_ERROR_CODE);
                        assertThat(blinkEx.isBusinessException()).isTrue();
                    });
        }
    }

    // ==================== 属性获取测试 ====================

    @Nested
    @DisplayName("属性获取测试")
    class PropertyAccessTests {

        @Test
        @DisplayName("应该正确获取错误码")
        void shouldGetErrorCode() {
            // given
            BlinkException exception = new BlinkException(TEST_ERROR_CODE);

            // when & then
            assertThat(exception.getCode()).isEqualTo(TEST_ERROR_CODE);
        }

        @Test
        @DisplayName("应该正确获取错误消息")
        void shouldGetErrorMessage() {
            // given
            String errorMessage = "测试错误消息";
            BlinkException exception = new BlinkException(errorMessage, TEST_ERROR_CODE);

            // when & then
            assertThat(exception.getErrMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("应该正确判断是否业务异常")
        void shouldCheckBusinessExceptionFlag() {
            // given
            BlinkException businessEx = new BlinkException(TEST_ERROR_CODE, true);
            BlinkException systemEx = new BlinkException(TEST_ERROR_CODE, false);

            // when & then
            assertThat(businessEx.isBusinessException()).isTrue();
            assertThat(systemEx.isBusinessException()).isFalse();
        }

        @Test
        @DisplayName("默认构造的业务异常标识应该为false")
        void shouldHaveDefaultBusinessFlagAsFalse() {
            // given
            BlinkException exception = new BlinkException(TEST_ERROR_CODE);

            // when & then - 默认情况下业务异常标识为false
            assertThat(exception.isBusinessException()).isFalse();
        }
    }

    // ==================== 异常链测试 ====================

    @Nested
    @DisplayName("异常链测试")
    class ExceptionChainTests {

        @Test
        @DisplayName("应该正确保留原始异常")
        void shouldPreserveOriginalException() {
            // given
            Throwable originalCause = new NullPointerException("空指针异常");

            // when
            BlinkException exception = new BlinkException("包装异常", originalCause, TEST_ERROR_CODE);

            // then
            assertThat(exception.getCause()).isSameAs(originalCause);
            assertThat(exception.getCause().getMessage()).isEqualTo("空指针异常");
        }

        @Test
        @DisplayName("应该支持异常链打印")
        void shouldSupportStackTracePrinting() {
            // given
            Throwable originalCause = new NullPointerException("空指针异常");
            BlinkException exception = new BlinkException("包装异常", originalCause, TEST_ERROR_CODE);

            // when & then
            assertThat(exception.getCause()).isNotNull();
            assertThat(exception.getStackTrace()).isNotEmpty();
        }

        @Test
        @DisplayName("应该正确包装其他类型异常")
        void shouldWrapOtherExceptions() {
            // given
            RuntimeException originalException = new RuntimeException("原始运行时异常");

            // when
            BlinkException wrapped = new BlinkException(originalException, TEST_ERROR_CODE);

            // then
            assertThat(wrapped.getCause()).isSameAs(originalException);
        }
    }

    // ==================== 预定义错误码测试 ====================

    @Nested
    @DisplayName("预定义错误码测试")
    class PredefinedErrorCodeTests {

        @Test
        @DisplayName("系统错误码应该是SYS00001")
        void shouldHaveCorrectSystemErrorCode() {
            assertThat(BlinkErrorCodeEnum.SYS_ERROR.getCode()).isEqualTo("SYS00001");
        }

        @Test
        @DisplayName("业务错误码应该是BUSS00001")
        void shouldHaveCorrectBusinessErrorCode() {
            assertThat(BlinkErrorCodeEnum.BUSINESS_ERROR.getCode()).isEqualTo("BUSS00001");
        }

        @Test
        @DisplayName("无权限错误码应该是SYS00401")
        void shouldHaveCorrectNoAuthErrorCode() {
            assertThat(BlinkErrorCodeEnum.NO_AUTH_ERROR.getCode()).isEqualTo("SYS00401");
        }

        @Test
        @DisplayName("禁止操作错误码应该是SYS00403")
        void shouldHaveCorrectForbiddenErrorCode() {
            assertThat(BlinkErrorCodeEnum.FORBIDDEN_OPERATION.getCode()).isEqualTo("SYS00403");
        }

        @Test
        @DisplayName("资源未找到错误码应该是SYS00404")
        void shouldHaveCorrectNotFoundErrorCode() {
            assertThat(BlinkErrorCodeEnum.NO_HANDLER_FOUND_ERROR.getCode()).isEqualTo("SYS00404");
        }

        @Test
        @DisplayName("Token无效错误码应该是BLINK0002")
        void shouldHaveCorrectTokenInvalidErrorCode() {
            assertThat(BlinkErrorCodeEnum.BLINK_TOKEN_INVALID.getCode()).isEqualTo("BLINK0002");
        }
    }

    // ==================== 实际使用场景测试 ====================

    @Nested
    @DisplayName("实际使用场景测试")
    class UsageScenarioTests {

        @Test
        @DisplayName("业务校验失败应该抛出业务异常")
        void shouldThrowBusinessExceptionForValidationFailure() {
            // when & then
            assertThatThrownBy(() -> {
                // 模拟业务校验失败
                boolean isValid = false;
                if (!isValid) {
                    BlinkException.throwBusinessException("VALIDATE001");
                }
            })
            .isInstanceOf(BlinkException.class)
            .satisfies(ex -> {
                BlinkException blinkEx = (BlinkException) ex;
                assertThat(blinkEx.isBusinessException()).isTrue();
                assertThat(blinkEx.getCode()).isEqualTo("VALIDATE001");
            });
        }

        @Test
        @DisplayName("系统异常应该包含原始异常信息")
        void shouldIncludeOriginalExceptionForSystemError() {
            // given
            Throwable dbException = new RuntimeException("数据库连接失败");

            // when & then
            assertThatThrownBy(() -> {
                try {
                    // 模拟数据库操作
                    throw dbException;
                } catch (Exception e) {
                    throw new BlinkException("数据库操作失败", e, BlinkErrorCodeEnum.ACCESS_DATABASE_ERROR.getCode());
                }
            })
            .isInstanceOf(BlinkException.class)
            .hasMessage("数据库操作失败")
            .hasCause(dbException);
        }

        @Test
        @DisplayName("异常可以被捕获并处理")
        void canBeCaughtAndHandled() {
            // given
            String capturedCode = null;
            String capturedMessage = null;

            // when
            try {
                BlinkException.throwBusinessException(TEST_ERROR_CODE);
            } catch (BlinkException e) {
                capturedCode = e.getCode();
                capturedMessage = e.getErrMessage();
            }

            // then
            assertThat(capturedCode).isEqualTo(TEST_ERROR_CODE);
        }
    }
}
