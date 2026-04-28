package com.blink.gateway.admin.notification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChannelType 枚举单元测试
 *
 * @author binblink
 * @since 2026-04-28
 */
@DisplayName("ChannelType 枚举测试")
class ChannelTypeTest {

    @Test
    @DisplayName("应该包含所有预定义渠道类型")
    void shouldContainAllPredefinedChannelTypes() {
        assertThat(ChannelType.values())
            .hasSize(7)
            .containsExactlyInAnyOrder(
                ChannelType.IN_APP,
                ChannelType.EMAIL,
                ChannelType.WEBHOOK,
                ChannelType.SMS,
                ChannelType.WECHAT,
                ChannelType.DINGTALK,
                ChannelType.FEISHU
            );
    }

    @Test
    @DisplayName("应该正确获取渠道名称和编码")
    void shouldGetNameAndCodeCorrectly() {
        assertThat(ChannelType.EMAIL.getName()).isEqualTo("邮件通知");
        assertThat(ChannelType.EMAIL.getCode()).isEqualTo("email");
        assertThat(ChannelType.WEBHOOK.getName()).isEqualTo("Webhook通知");
        assertThat(ChannelType.WEBHOOK.getCode()).isEqualTo("webhook");
        assertThat(ChannelType.IN_APP.getName()).isEqualTo("站内通知");
        assertThat(ChannelType.IN_APP.getCode()).isEqualTo("in_app");
    }

    @Test
    @DisplayName("应该根据code查找对应的渠道类型")
    void shouldFindChannelTypeByCode() {
        assertThat(ChannelType.fromCode("email")).isEqualTo(ChannelType.EMAIL);
        assertThat(ChannelType.fromCode("webhook")).isEqualTo(ChannelType.WEBHOOK);
        assertThat(ChannelType.fromCode("in_app")).isEqualTo(ChannelType.IN_APP);
        assertThat(ChannelType.fromCode("sms")).isEqualTo(ChannelType.SMS);
    }

    @Test
    @DisplayName("应该对无效code返回null")
    void shouldReturnNullForInvalidCode() {
        assertThat(ChannelType.fromCode("invalid")).isNull();
        assertThat(ChannelType.fromCode(null)).isNull();
    }

    @Test
    @DisplayName("应该对空字符串code返回null")
    void shouldReturnNullForEmptyCode() {
        assertThat(ChannelType.fromCode("")).isNull();
        assertThat(ChannelType.fromCode("  ")).isNull();
    }
}
