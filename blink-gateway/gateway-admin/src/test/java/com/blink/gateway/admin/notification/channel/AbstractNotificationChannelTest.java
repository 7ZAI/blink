package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AbstractNotificationChannel 抽象基类测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@DisplayName("AbstractNotificationChannel 抽象基类测试")
class AbstractNotificationChannelTest {

    private TestNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new TestNotificationChannel();
    }

    @Nested
    @DisplayName("渠道可用性检查测试")
    class AvailabilityTests {

        @Test
        @DisplayName("渠道不可用时应该返回不可用结果")
        void shouldReturnUnavailableWhenChannelNotAvailable() {
            channel.setAvailable(false);

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = channel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("CHANNEL_UNAVAILABLE");
            assertThat(result.getErrorMessage()).isEqualTo("渠道未配置或不可用");
        }

        @Test
        @DisplayName("渠道可用时应该执行发送")
        void shouldExecuteSendWhenChannelAvailable() {
            channel.setAvailable(true);
            channel.setSendSuccess(true);

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = channel.send(message);

            assertThat(result.isSuccess()).isTrue();
            assertThat(channel.isDoSendCalled()).isTrue();
        }
    }

    @Nested
    @DisplayName("参数校验测试")
    class ValidationTests {

        @Test
        @DisplayName("校验失败时应该返回失败结果")
        void shouldReturnFailureWhenValidationFails() {
            channel.setAvailable(true);
            channel.setValidationFail(true);

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = channel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("校验失败");
            assertThat(channel.isDoSendCalled()).isFalse();
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("发送异常时应该返回失败结果")
        void shouldReturnFailureOnException() {
            channel.setAvailable(true);
            channel.setThrowException(true);

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = channel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("模拟发送异常");
        }
    }

    @Nested
    @DisplayName("渠道名称测试")
    class ChannelNameTests {

        @Test
        @DisplayName("应该返回正确的渠道名称")
        void shouldReturnCorrectChannelName() {
            assertThat(channel.getChannelName()).isEqualTo("邮件通知");
            assertThat(channel.getChannelType()).isEqualTo(ChannelType.EMAIL);
        }
    }

    /**
     * 测试用渠道实现
     */
    private static class TestNotificationChannel extends AbstractNotificationChannel {

        private boolean available = true;
        private boolean sendSuccess = true;
        private boolean validationFail = false;
        private boolean throwException = false;
        private boolean doSendCalled = false;

        @Override
        public ChannelType getChannelType() {
            return ChannelType.EMAIL;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        protected SendResult validate(NotificationMessage message) {
            if (validationFail) {
                return SendResult.failure(getChannelType(), "校验失败");
            }
            return SendResult.success(getChannelType());
        }

        @Override
        protected SendResult doSend(NotificationMessage message) {
            doSendCalled = true;
            if (throwException) {
                throw new RuntimeException("模拟发送异常");
            }
            if (sendSuccess) {
                return SendResult.success(getChannelType());
            }
            return SendResult.failure(getChannelType(), "发送失败");
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void setSendSuccess(boolean sendSuccess) {
            this.sendSuccess = sendSuccess;
        }

        public void setValidationFail(boolean validationFail) {
            this.validationFail = validationFail;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        public boolean isDoSendCalled() {
            return doSendCalled;
        }
    }
}
