package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.notification.entity.NotificationChannelConfigDO;
import com.blink.gateway.admin.notification.mapper.NotificationChannelConfigMapper;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * EmailNotificationChannel 邮件渠道测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationChannel 邮件渠道测试")
class EmailNotificationChannelTest {

    @Mock
    private NotificationChannelConfigMapper configMapper;

    @InjectMocks
    private EmailNotificationChannel emailChannel;

    @Nested
    @DisplayName("渠道类型测试")
    class ChannelTypeTests {

        @Test
        @DisplayName("应该返回EMAIL渠道类型")
        void shouldReturnEmailChannelType() {
            assertThat(emailChannel.getChannelType()).isEqualTo(ChannelType.EMAIL);
            assertThat(emailChannel.getChannelName()).isEqualTo("邮件通知");
        }
    }

    @Nested
    @DisplayName("可用性测试")
    class AvailabilityTests {

        @Test
        @DisplayName("未配置时应该不可用")
        void shouldNotBeAvailableWhenNotConfigured() {
            // 默认情况下（未调用 init），mailSender 为 null
            assertThat(emailChannel.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("配置后应该可用")
        void shouldBeAvailableWhenConfigured() {
            NotificationChannelConfigDO config = new NotificationChannelConfigDO();
            config.setConfigJson("{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"test\",\"password\":\"pwd\",\"fromAddress\":\"test@example.com\"}");
            config.setEnabled((byte) 1);
            when(configMapper.selectByChannelType("email")).thenReturn(config);

            // 重新初始化
            emailChannel.init();

            assertThat(emailChannel.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("参数校验测试")
    class ValidationTests {

        @Test
        @DisplayName("接收人为空时应该返回失败")
        void shouldReturnFailureWhenRecipientsEmpty() {
            // 先配置渠道可用
            NotificationChannelConfigDO config = new NotificationChannelConfigDO();
            config.setConfigJson("{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"test\",\"password\":\"pwd\",\"fromAddress\":\"test@example.com\"}");
            config.setEnabled((byte) 1);
            when(configMapper.selectByChannelType("email")).thenReturn(config);
            emailChannel.init();

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .recipients(List.of())
                .build();

            SendResult result = emailChannel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("收件人");
        }

        @Test
        @DisplayName("接收人为null时应该返回失败")
        void shouldReturnFailureWhenRecipientsNull() {
            // 先配置渠道可用
            NotificationChannelConfigDO config = new NotificationChannelConfigDO();
            config.setConfigJson("{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"test\",\"password\":\"pwd\",\"fromAddress\":\"test@example.com\"}");
            config.setEnabled((byte) 1);
            when(configMapper.selectByChannelType("email")).thenReturn(config);
            emailChannel.init();

            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = emailChannel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("收件人");
        }
    }

    @Nested
    @DisplayName("发送测试")
    class SendTests {

        @Test
        @DisplayName("渠道不可用时应该返回不可用结果")
        void shouldReturnUnavailableWhenChannelNotAvailable() {
            // 不配置，渠道不可用
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .recipients(List.of("test@example.com"))
                .build();

            SendResult result = emailChannel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("CHANNEL_UNAVAILABLE");
        }
    }
}
