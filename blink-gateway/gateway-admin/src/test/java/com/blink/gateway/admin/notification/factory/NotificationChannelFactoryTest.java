package com.blink.gateway.admin.notification.factory;

import com.blink.gateway.admin.notification.channel.NotificationChannel;
import com.blink.gateway.admin.notification.model.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NotificationChannelFactory 渠道工厂测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@DisplayName("NotificationChannelFactory 渠道工厂测试")
class NotificationChannelFactoryTest {

    private NotificationChannelFactory factory;

    @BeforeEach
    void setUp() {
        // 创建模拟渠道
        NotificationChannel emailChannel = mock(NotificationChannel.class);
        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);
        when(emailChannel.isAvailable()).thenReturn(true);

        NotificationChannel webhookChannel = mock(NotificationChannel.class);
        when(webhookChannel.getChannelType()).thenReturn(ChannelType.WEBHOOK);
        when(webhookChannel.isAvailable()).thenReturn(true);

        NotificationChannel inAppChannel = mock(NotificationChannel.class);
        when(inAppChannel.getChannelType()).thenReturn(ChannelType.IN_APP);
        when(inAppChannel.isAvailable()).thenReturn(false); // 不可用

        factory = new NotificationChannelFactory(List.of(emailChannel, webhookChannel, inAppChannel));
    }

    @Nested
    @DisplayName("渠道获取测试")
    class GetChannelTests {

        @Test
        @DisplayName("应该根据类型获取渠道")
        void shouldGetChannelByType() {
            NotificationChannel channel = factory.getChannel(ChannelType.EMAIL);

            assertThat(channel).isNotNull();
            assertThat(channel.getChannelType()).isEqualTo(ChannelType.EMAIL);
        }

        @Test
        @DisplayName("未注册的渠道应该返回null")
        void shouldReturnNullForUnregisteredChannel() {
            NotificationChannel channel = factory.getChannel(ChannelType.SMS);

            assertThat(channel).isNull();
        }
    }

    @Nested
    @DisplayName("可用渠道测试")
    class AvailableChannelsTests {

        @Test
        @DisplayName("应该获取所有可用渠道")
        void shouldGetAllAvailableChannels() {
            List<NotificationChannel> availableChannels = factory.getAvailableChannels();

            assertThat(availableChannels).hasSize(2);
            assertThat(availableChannels)
                .extracting(NotificationChannel::getChannelType)
                .containsExactlyInAnyOrder(ChannelType.EMAIL, ChannelType.WEBHOOK);
        }
    }

    @Nested
    @DisplayName("已注册渠道类型测试")
    class RegisteredTypesTests {

        @Test
        @DisplayName("应该获取所有已注册渠道类型")
        void shouldGetAllRegisteredChannelTypes() {
            List<ChannelType> types = factory.getRegisteredChannelTypes();

            assertThat(types).hasSize(3);
            assertThat(types).containsExactlyInAnyOrder(
                ChannelType.EMAIL,
                ChannelType.WEBHOOK,
                ChannelType.IN_APP
            );
        }
    }

    @Nested
    @DisplayName("空渠道列表测试")
    class EmptyChannelsTests {

        @Test
        @DisplayName("空渠道列表应该正常处理")
        void shouldHandleEmptyChannelsList() {
            NotificationChannelFactory emptyFactory = new NotificationChannelFactory(List.of());

            assertThat(emptyFactory.getRegisteredChannelTypes()).isEmpty();
            assertThat(emptyFactory.getAvailableChannels()).isEmpty();
            assertThat(emptyFactory.getChannel(ChannelType.EMAIL)).isNull();
        }
    }
}
