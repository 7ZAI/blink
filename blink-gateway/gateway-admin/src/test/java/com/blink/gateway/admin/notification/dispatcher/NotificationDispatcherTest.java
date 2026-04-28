package com.blink.gateway.admin.notification.dispatcher;

import com.blink.gateway.admin.notification.channel.NotificationChannel;
import com.blink.gateway.admin.notification.factory.NotificationChannelFactory;
import com.blink.gateway.admin.notification.handler.NotificationFailureHandler;
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
import static org.mockito.Mockito.*;

/**
 * NotificationDispatcher 统一分发器测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDispatcher 统一分发器测试")
class NotificationDispatcherTest {

    @Mock
    private NotificationChannelFactory channelFactory;

    @Mock
    private NotificationFailureHandler failureHandler;

    @Mock
    private NotificationChannel emailChannel;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Nested
    @DisplayName("同步发送测试")
    class SyncDispatchTests {

        @Test
        @DisplayName("应该成功发送通知")
        void shouldDispatchSuccessfully() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            when(channelFactory.getChannel(ChannelType.EMAIL)).thenReturn(emailChannel);
            when(emailChannel.send(message)).thenReturn(SendResult.success(ChannelType.EMAIL));

            SendResult result = dispatcher.dispatch(message, ChannelType.EMAIL);

            assertThat(result.isSuccess()).isTrue();
            verify(emailChannel, times(1)).send(message);
        }

        @Test
        @DisplayName("渠道未注册时应该返回失败")
        void shouldReturnFailureWhenChannelNotRegistered() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            when(channelFactory.getChannel(ChannelType.SMS)).thenReturn(null);

            SendResult result = dispatcher.dispatch(message, ChannelType.SMS);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("渠道未注册");
        }

        @Test
        @DisplayName("发送失败时应该调用失败处理器")
        void shouldCallFailureHandlerOnFailure() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult failureResult = SendResult.failure(ChannelType.EMAIL, "发送失败");

            when(channelFactory.getChannel(ChannelType.EMAIL)).thenReturn(emailChannel);
            when(emailChannel.send(message)).thenReturn(failureResult);

            SendResult result = dispatcher.dispatch(message, ChannelType.EMAIL);

            assertThat(result.isSuccess()).isFalse();
            verify(failureHandler, times(1)).handleFailure(message, failureResult);
        }
    }

    @Nested
    @DisplayName("多渠道发送测试")
    class MultiChannelTests {

        @Test
        @DisplayName("应该发送到多个渠道")
        void shouldDispatchToMultipleChannels() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            NotificationChannel webhookChannel = mock(NotificationChannel.class);

            when(channelFactory.getChannel(ChannelType.EMAIL)).thenReturn(emailChannel);
            when(channelFactory.getChannel(ChannelType.WEBHOOK)).thenReturn(webhookChannel);
            when(emailChannel.send(message)).thenReturn(SendResult.success(ChannelType.EMAIL));
            when(webhookChannel.send(message)).thenReturn(SendResult.success(ChannelType.WEBHOOK));

            List<SendResult> results = dispatcher.dispatch(message, List.of(ChannelType.EMAIL, ChannelType.WEBHOOK));

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(SendResult::isSuccess);
            verify(emailChannel, times(1)).send(message);
            verify(webhookChannel, times(1)).send(message);
        }
    }
}
