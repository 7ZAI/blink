package com.blink.log.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogSensitiveUtils 工具类测试
 *
 * @author binblink
 */
@DisplayName("LogSensitiveUtils 工具类测试")
class LogSensitiveUtilsTest {

    // ==================== toSensitiveString() 无长度限制测试 ====================

    @Nested
    @DisplayName("toSensitiveString() 无长度限制测试")
    class ToSensitiveStringBasicTest {

        @Test
        @DisplayName("null对象 - 返回null字符串")
        void toSensitiveString_null_shouldReturnNullString() {
            // when
            String result = LogSensitiveUtils.toSensitiveString(null);

            // then
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("简单字符串对象 - 直接序列化")
        void toSensitiveString_string_shouldReturnJson() {
            // given
            String str = "hello";

            // when - String无法转为Map，直接序列化
            String result = LogSensitiveUtils.toSensitiveString(str);

            // then
            assertThat(result).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("数字对象 - 直接序列化")
        void toSensitiveString_number_shouldReturnJson() {
            // given
            Integer num = 123;

            // when - Integer无法转为Map，直接序列化
            String result = LogSensitiveUtils.toSensitiveString(num);

            // then
            assertThat(result).isEqualTo("123");
        }

        @Test
        @DisplayName("Map对象 - 自动脱敏敏感字段")
        void toSensitiveString_map_shouldMaskSensitiveFields() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "张三");
            map.put("password", "secret123");
            map.put("phone", "13812345678");

            // when
            String result = LogSensitiveUtils.toSensitiveString(map);

            // then
            assertThat(result).contains("\"name\":\"张三\"");
            assertThat(result).contains("\"password\":\"******\"");
            assertThat(result).contains("\"phone\":\"138****5678\"");
        }
    }

    // ==================== toSensitiveString(obj, maxLength) 带截断测试 ====================

    @Nested
    @DisplayName("toSensitiveString(obj, maxLength) 带截断测试")
    class ToSensitiveStringWithLimitTest {

        @Test
        @DisplayName("null对象 - 返回null字符串")
        void toSensitiveString_null_shouldReturnNullString() {
            // when
            String result = LogSensitiveUtils.toSensitiveString(null, 100);

            // then
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("长度未超过限制 - 返回原JSON")
        void toSensitiveString_underLimit_shouldReturnOriginal() {
            // given
            String str = "hello";

            // when
            String result = LogSensitiveUtils.toSensitiveString(str, 100);

            // then
            assertThat(result).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("长度超过限制 - 返回截断提示对象")
        void toSensitiveString_exceedsLimit_shouldReturnTruncatedInfo() {
            // given
            String longStr = "a".repeat(200);

            // when
            String result = LogSensitiveUtils.toSensitiveString(longStr, 100);

            // then - 应包含截断信息
            assertThat(result).contains("\"_truncated\":true");
            assertThat(result).contains("\"_originalLength\":");
            assertThat(result).contains("\"_maxLength\":100");
        }

        @Test
        @DisplayName("maxLength为0或负数 - 不限制长度")
        void toSensitiveString_zeroOrNegativeLimit_shouldNotTruncate() {
            // given
            String str = "hello";

            // when
            String result1 = LogSensitiveUtils.toSensitiveString(str, 0);
            String result2 = LogSensitiveUtils.toSensitiveString(str, -1);

            // then
            assertThat(result1).isEqualTo("\"hello\"");
            assertThat(result2).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("截断提示对象包含预览内容")
        void toSensitiveString_truncated_shouldContainPreview() {
            // given
            String longStr = "a".repeat(500);

            // when
            String result = LogSensitiveUtils.toSensitiveString(longStr, 100);

            // then
            assertThat(result).contains("\"_preview\":");
        }
    }

    // ==================== maskPhone() 测试 ====================

    @Nested
    @DisplayName("maskPhone() 方法测试")
    class MaskPhoneTest {

        @Test
        @DisplayName("标准11位手机号 - 调用SensitiveUtils")
        void maskPhone_standard11Digits_shouldMaskCorrectly() {
            // given
            String phone = "13812345678";

            // when - 使用SensitiveUtils.maskPhone（前3后4）
            String result = LogSensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("短手机号（长度<7）")
        void maskPhone_shortNumber_shouldReturnOriginal() {
            // given
            String phone = "123456";

            // when
            String result = LogSensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo("123456");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskPhone_blankOrNull_shouldReturnOriginal(String phone) {
            // when
            String result = LogSensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo(phone);
        }
    }

    // ==================== maskEmail() 测试 ====================

    @Nested
    @DisplayName("maskEmail() 方法测试")
    class MaskEmailTest {

        @Test
        @DisplayName("标准邮箱 - 调用SensitiveUtils")
        void maskEmail_standardEmail_shouldMaskCorrectly() {
            // given
            String email = "zhangsan@qq.com";

            // when - 使用SensitiveUtils.maskEmail
            String result = LogSensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("zha*****@qq.com");
        }

        @Test
        @DisplayName("短前缀邮箱（<=3字符）")
        void maskEmail_shortPrefix_shouldKeep1Char() {
            // given
            String email = "ab@qq.com";

            // when
            String result = LogSensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("a*@qq.com");
        }

        @Test
        @DisplayName("无@符号 - 返回原值")
        void maskEmail_noAtSymbol_shouldReturnOriginal() {
            // given
            String email = "noemail";

            // when
            String result = LogSensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("noemail");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskEmail_blankOrNull_shouldReturnOriginal(String email) {
            // when
            String result = LogSensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo(email);
        }
    }

    // ==================== maskIdCard() 测试 ====================

    @Nested
    @DisplayName("maskIdCard() 方法测试")
    class MaskIdCardTest {

        @Test
        @DisplayName("标准18位身份证 - 使用SensitiveUtils（前6后4）")
        void maskIdCard_standard18Digits_shouldMaskCorrectly() {
            // given
            String idCard = "110101199001011234";

            // when - 使用SensitiveUtils.maskIdCard（前6后4）
            String result = LogSensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("短身份证号（长度<10）")
        void maskIdCard_shortNumber_shouldReturnOriginal() {
            // given - ID_CARD规则前6后4=10，长度不足10返回原值
            String idCard = "123456789"; // 9字符

            // when
            String result = LogSensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo("123456789");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskIdCard_blankOrNull_shouldReturnOriginal(String idCard) {
            // when
            String result = LogSensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo(idCard);
        }
    }

    // ==================== mask() 通用脱敏测试 ====================

    @Nested
    @DisplayName("mask() 通用脱敏方法测试")
    class MaskTest {

        @Test
        @DisplayName("正常脱敏 - 调用SensitiveUtils")
        void mask_normalCase_shouldMaskCorrectly() {
            // given
            String source = "abcdefghij"; // 10字符

            // when - 前3后4=7，脱敏10-7=3个
            String result = LogSensitiveUtils.mask(source, 3, 4, '*');

            // then
            assertThat(result).isEqualTo("abc***ghij");
        }

        @Test
        @DisplayName("自定义脱敏字符")
        void mask_customChar_shouldUseCustomChar() {
            // given
            String source = "abcdefghij"; // 10字符

            // when - 前3后4=7，脱敏3个
            String result = LogSensitiveUtils.mask(source, 3, 4, '#');

            // then
            assertThat(result).isEqualTo("abc###ghij");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白 - 返回原值")
        void mask_blankOrNull_shouldReturnOriginal(String source) {
            // when
            String result = LogSensitiveUtils.mask(source, 3, 4, '*');

            // then
            assertThat(result).isEqualTo(source);
        }

        @Test
        @DisplayName("前后缀保留长度超过字符串长度 - 返回原值")
        void mask_keepExceedsLength_shouldReturnOriginal() {
            // given
            String source = "abc";

            // when
            String result = LogSensitiveUtils.mask(source, 2, 2, '*');

            // then - 2+2=4 > 3，返回原值
            assertThat(result).isEqualTo("abc");
        }
    }
}
