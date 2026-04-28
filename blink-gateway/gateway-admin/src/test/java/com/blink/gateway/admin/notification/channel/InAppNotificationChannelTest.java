package com.blink.gateway.admin.notification.channel;

import com.blink.gateway.admin.service.NotificationPublishService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InAppNotificationChannel 站内通知渠道测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InAppNotificationChannel 站内通知渠道测试")
class InAppNotificationChannelTest {

    @Mock
    private NotificationPublishService notificationPublishService;

    @InjectMocks
    private InAppNotificationChannel inAppChannel;

    @Nested
    @DisplayName("渠道类型测试")
    class ChannelTypeTests {

        @Test
        @DisplayName("应该返回IN_APP渠道类型")
        void shouldReturnInAppChannelType() {
            assertThat(inAppChannel.getChannelType()).isEqualTo(ChannelType.IN_APP);
            assertThat(inAppChannel.getChannelName()).isEqualTo("站内通知");
        }
    }

    @Nested
    @DisplayName("可用性测试")
    class AvailabilityTests {

        @Test
        @DisplayName("站内通知渠道始终可用")
        void shouldBeAlwaysAvailable() {
            assertThat(inAppChannel.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("发送测试")
    class SendTests {

        @Test
        @DisplayName("应该成功发送站内通知")
        void shouldSendInAppNotificationSuccessfully() {
            NotificationMessage message = NotificationMessage.builder()
                .title("告警通知")
                .content("CPU使用率超过80%")
                .severity("WARNING")
                .build();

            SendResult result = inAppChannel.send(message);

            assertThat(result.isSuccess()).isTrue();
            verify(notificationPublishService, times(1)).sendAlert(
                eq("告警通知"),
                eq("CPU使用率超过80%"),
                eq("WARNING")
            );
        }

        @Test
        @DisplayName("应该处理空严重级别")
        void shouldHandleNullSeverity() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = inAppChannel.send(message);

            assertThat(result.isSuccess()).isTrue();
            verify(notificationPublishService, times(1)).sendAlert(
                anyString(),
                anyString(),
                anyString()
            );
        }
    }
}
