package com.blink.log.sensitive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveType 枚举类测试
 *
 * @author binblink
 */
@DisplayName("SensitiveType 枚举类测试")
class SensitiveTypeTest {

    @Test
    @DisplayName("PHONE 类型配置验证")
    void phoneType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.PHONE;

        // then
        assertThat(type.getDescription()).isEqualTo("手机号");
        assertThat(type.getPrefixKeep()).isEqualTo(3);
        assertThat(type.getSuffixKeep()).isEqualTo(4);
    }

    @Test
    @DisplayName("ID_CARD 类型配置验证")
    void idCardType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.ID_CARD;

        // then
        assertThat(type.getDescription()).isEqualTo("身份证号");
        assertThat(type.getPrefixKeep()).isEqualTo(6);
        assertThat(type.getSuffixKeep()).isEqualTo(4);
    }

    @Test
    @DisplayName("BANK_CARD 类型配置验证")
    void bankCardType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.BANK_CARD;

        // then
        assertThat(type.getDescription()).isEqualTo("银行卡号");
        assertThat(type.getPrefixKeep()).isEqualTo(4);
        assertThat(type.getSuffixKeep()).isEqualTo(4);
    }

    @Test
    @DisplayName("EMAIL 类型配置验证")
    void emailType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.EMAIL;

        // then
        assertThat(type.getDescription()).isEqualTo("邮箱");
        assertThat(type.getPrefixKeep()).isEqualTo(3);
        assertThat(type.getSuffixKeep()).isEqualTo(4);
    }

    @Test
    @DisplayName("NAME 类型配置验证")
    void nameType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.NAME;

        // then
        assertThat(type.getDescription()).isEqualTo("姓名");
        assertThat(type.getPrefixKeep()).isEqualTo(1);
        assertThat(type.getSuffixKeep()).isEqualTo(1);
    }

    @Test
    @DisplayName("PASSWORD 类型配置验证")
    void passwordType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.PASSWORD;

        // then
        assertThat(type.getDescription()).isEqualTo("密码");
        assertThat(type.getPrefixKeep()).isEqualTo(0);
        assertThat(type.getSuffixKeep()).isEqualTo(0);
    }

    @Test
    @DisplayName("ADDRESS 类型配置验证")
    void addressType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.ADDRESS;

        // then
        assertThat(type.getDescription()).isEqualTo("地址");
        assertThat(type.getPrefixKeep()).isEqualTo(6);
        assertThat(type.getSuffixKeep()).isEqualTo(0);
    }

    @Test
    @DisplayName("CUSTOM 类型配置验证")
    void customType_shouldHaveCorrectConfig() {
        // when
        SensitiveType type = SensitiveType.CUSTOM;

        // then
        assertThat(type.getDescription()).isEqualTo("自定义");
        assertThat(type.getPrefixKeep()).isEqualTo(0);
        assertThat(type.getSuffixKeep()).isEqualTo(0);
    }

    @Test
    @DisplayName("枚举数量验证")
    void enumCount_shouldBeEight() {
        // when
        SensitiveType[] values = SensitiveType.values();

        // then
        assertThat(values).hasSize(8);
    }

    @Test
    @DisplayName("所有枚举值都有描述")
    void allValues_shouldHaveDescription() {
        // when & then
        for (SensitiveType type : SensitiveType.values()) {
            assertThat(type.getDescription()).isNotBlank();
        }
    }
}
