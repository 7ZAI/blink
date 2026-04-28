package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.notification.entity.NotificationChannelConfigDO;
import com.blink.gateway.admin.notification.mapper.NotificationChannelConfigMapper;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * WebhookNotificationChannel Webhook渠道测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookNotificationChannel Webhook渠道测试")
class WebhookNotificationChannelTest {

    @Mock
    private NotificationChannelConfigMapper configMapper;

    @InjectMocks
    private WebhookNotificationChannel webhookChannel;

    @BeforeEach
    void setUp() {
        // 使用 lenient 允许不必要的 stubbing（某些测试可能不需要调用此方法）
        lenient().when(configMapper.selectByChannelType("webhook")).thenReturn(null);
    }

    @Nested
    @DisplayName("渠道类型测试")
    class ChannelTypeTests {

        @Test
        @DisplayName("应该返回WEBHOOK渠道类型")
        void shouldReturnWebhookChannelType() {
            assertThat(webhookChannel.getChannelType()).isEqualTo(ChannelType.WEBHOOK);
            assertThat(webhookChannel.getChannelName()).isEqualTo("Webhook通知");
        }
    }

    @Nested
    @DisplayName("可用性测试")
    class AvailabilityTests {

        @Test
        @DisplayName("未配置时应该不可用")
        void shouldNotBeAvailableWhenNotConfigured() {
            assertThat(webhookChannel.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("配置后应该可用")
        void shouldBeAvailableWhenConfigured() {
            NotificationChannelConfigDO config = new NotificationChannelConfigDO();
            config.setConfigJson("{\"url\":\"https://webhook.example.com/notify\",\"method\":\"POST\",\"timeout\":5000}");
            config.setEnabled((byte) 1);
            when(configMapper.selectByChannelType("webhook")).thenReturn(config);

            webhookChannel.init();

            assertThat(webhookChannel.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("发送测试")
    class SendTests {

        @Test
        @DisplayName("渠道不可用时应该返回不可用结果")
        void shouldReturnUnavailableWhenChannelNotAvailable() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = webhookChannel.send(message);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("CHANNEL_UNAVAILABLE");
        }
    }

    @Nested
    @DisplayName("扩展参数测试")
    class ExtraParamsTests {

        @Test
        @DisplayName("应该支持扩展参数")
        void shouldSupportExtraParams() {
            NotificationChannelConfigDO config = new NotificationChannelConfigDO();
            config.setConfigJson("{\"url\":\"https://webhook.example.com/notify\",\"method\":\"POST\"}");
            config.setEnabled((byte) 1);
            when(configMapper.selectByChannelType("webhook")).thenReturn(config);

            webhookChannel.init();

            assertThat(webhookChannel.isAvailable()).isTrue();
        }
    }
}
