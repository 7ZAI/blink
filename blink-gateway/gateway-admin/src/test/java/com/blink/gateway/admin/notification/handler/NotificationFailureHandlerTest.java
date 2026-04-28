package com.blink.gateway.admin.notification.handler;

import com.blink.gateway.admin.notification.entity.NotificationFailureLogDO;
import com.blink.gateway.admin.notification.mapper.NotificationFailureLogMapper;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * NotificationFailureHandler 失败处理器测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationFailureHandler 失败处理器测试")
class NotificationFailureHandlerTest {

    @Mock
    private NotificationFailureLogMapper failureLogMapper;

    @InjectMocks
    private NotificationFailureHandler failureHandler;

    @Nested
    @DisplayName("失败处理测试")
    class HandleFailureTests {

        @Test
        @DisplayName("应该持久化失败记录")
        void shouldPersistFailureLog() {
            NotificationMessage message = NotificationMessage.builder()
                .title("告警通知")
                .content("CPU使用率超过80%")
                .notificationType("ALERT")
                .severity("WARNING")
                .recipients(List.of("admin@example.com"))
                .businessId("alert-123")
                .build();

            SendResult result = SendResult.failure(ChannelType.EMAIL, "SMTP连接失败");

            failureHandler.handleFailure(message, result);

            ArgumentCaptor<NotificationFailureLogDO> captor = ArgumentCaptor.forClass(NotificationFailureLogDO.class);
            verify(failureLogMapper, times(1)).insert(captor.capture());

            NotificationFailureLogDO log = captor.getValue();
            assertThat(log.getChannelType()).isEqualTo("email");
            assertThat(log.getNotificationType()).isEqualTo("ALERT");
            assertThat(log.getBusinessId()).isEqualTo("alert-123");
            assertThat(log.getTitle()).isEqualTo("告警通知");
            assertThat(log.getContent()).isEqualTo("CPU使用率超过80%");
            assertThat(log.getErrorMessage()).isEqualTo("SMTP连接失败");
            assertThat(log.getStatus()).isEqualTo((byte) 0); // 待重试
        }

        @Test
        @DisplayName("应该处理带错误码的失败结果")
        void shouldHandleFailureWithErrorCode() {
            NotificationMessage message = NotificationMessage.builder()
                .title("测试")
                .content("内容")
                .build();

            SendResult result = SendResult.failure(ChannelType.WEBHOOK, "TIMEOUT", "请求超时");

            failureHandler.handleFailure(message, result);

            ArgumentCaptor<NotificationFailureLogDO> captor = ArgumentCaptor.forClass(NotificationFailureLogDO.class);
            verify(failureLogMapper, times(1)).insert(captor.capture());

            NotificationFailureLogDO log = captor.getValue();
            assertThat(log.getChannelType()).isEqualTo("webhook");
            assertThat(log.getErrorCode()).isEqualTo("TIMEOUT");
            assertThat(log.getErrorMessage()).isEqualTo("请求超时");
        }
    }
}
