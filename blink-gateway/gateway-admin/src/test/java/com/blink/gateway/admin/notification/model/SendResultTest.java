package com.blink.gateway.admin.notification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SendResult 模型单元测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@DisplayName("SendResult 模型测试")
class SendResultTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建成功结果")
        void shouldCreateSuccessResult() {
            SendResult result = SendResult.success(ChannelType.EMAIL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.EMAIL);
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("应该创建失败结果")
        void shouldCreateFailureResult() {
            SendResult result = SendResult.failure(ChannelType.EMAIL, "邮件发送失败");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.EMAIL);
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isEqualTo("邮件发送失败");
        }

        @Test
        @DisplayName("应该创建不可用结果")
        void shouldCreateUnavailableResult() {
            SendResult result = SendResult.unavailable(ChannelType.WEBHOOK);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.WEBHOOK);
            assertThat(result.getErrorCode()).isEqualTo("CHANNEL_UNAVAILABLE");
            assertThat(result.getErrorMessage()).isEqualTo("渠道未配置或不可用");
        }

        @Test
        @DisplayName("应该创建带错误码的失败结果")
        void shouldCreateFailureResultWithErrorCode() {
            SendResult result = SendResult.failure(ChannelType.SMS, "RATE_LIMIT", "发送频率超限");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.SMS);
            assertThat(result.getErrorCode()).isEqualTo("RATE_LIMIT");
            assertThat(result.getErrorMessage()).isEqualTo("发送频率超限");
        }
    }

    @Nested
    @DisplayName("Builder模式测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用Builder构建结果")
        void shouldBuildResultWithBuilder() {
            SendResult result = SendResult.builder()
                .success(true)
                .channelType(ChannelType.EMAIL)
                .externalMessageId("msg-123")
                .build();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.EMAIL);
            assertThat(result.getExternalMessageId()).isEqualTo("msg-123");
        }
    }
}
