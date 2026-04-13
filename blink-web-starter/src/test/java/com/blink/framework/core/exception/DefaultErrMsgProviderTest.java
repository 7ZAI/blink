package com.blink.framework.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultErrMsgProvider 单元测试
 *
 * @author binblink
 */
@DisplayName("DefaultErrMsgProvider 单元测试")
class DefaultErrMsgProviderTest {

    private final DefaultErrMsgProvider provider = new DefaultErrMsgProvider();

    @Nested
    @DisplayName("getErrMsg 方法测试")
    class GetErrMsgTest {

        @Test
        @DisplayName("中文业务错误消息 - BUSS前缀")
        void testGetErrMsg_中文业务错误消息_BUSS前缀() {
            // Arrange
            String msgCode = "BUSS0001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
            assertThat(result).contains("错误码：BUSS0001");
        }

        @Test
        @DisplayName("英文业务错误消息 - BUSS前缀")
        void testGetErrMsg_英文业务错误消息_BUSS前缀() {
            // Arrange
            String msgCode = "BUSS0001";
            String lang = "en_us";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("Operation failed");
            assertThat(result).contains("Error Code: BUSS0001");
        }

        @Test
        @DisplayName("中文业务错误消息 - INVALID前缀")
        void testGetErrMsg_中文业务错误消息_INVALID前缀() {
            // Arrange
            String msgCode = "INVALID001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
            assertThat(result).contains("错误码：INVALID001");
        }

        @Test
        @DisplayName("中文业务错误消息 - AUTH前缀")
        void testGetErrMsg_中文业务错误消息_AUTH前缀() {
            // Arrange
            String msgCode = "AUTH0001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
            assertThat(result).contains("错误码：AUTH0001");
        }

        @Test
        @DisplayName("中文业务错误消息 - FLOW前缀")
        void testGetErrMsg_中文业务错误消息_FLOW前缀() {
            // Arrange
            String msgCode = "FLOW0001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
            assertThat(result).contains("错误码：FLOW0001");
        }

        @Test
        @DisplayName("中文系统错误消息 - 其他前缀")
        void testGetErrMsg_中文系统错误消息_其他前缀() {
            // Arrange
            String msgCode = "GATE0001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("系统错误，请稍后重试");
        }

        @Test
        @DisplayName("英文系统错误消息 - 其他前缀")
        void testGetErrMsg_英文系统错误消息_其他前缀() {
            // Arrange
            String msgCode = "GATE0001";
            String lang = "en_us";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("System error, please try again later");
        }

        @Test
        @DisplayName("语言参数大小写不敏感")
        void testGetErrMsg_语言参数大小写不敏感() {
            // Arrange
            String msgCode = "BUSS0001";
            String lang = "ZH_CN";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
        }

        @Test
        @DisplayName("错误码大小写不敏感")
        void testGetErrMsg_错误码大小写不敏感() {
            // Arrange
            String msgCode = "buss0001";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
        }
    }

    @Nested
    @DisplayName("空值和边界场景测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("空错误码 - 返回系统错误消息")
        void testGetErrMsg_空错误码() {
            // Arrange
            String msgCode = "";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("系统错误，请稍后重试");
        }

        @Test
        @DisplayName("null错误码 - 返回系统错误消息")
        void testGetErrMsg_null错误码() {
            // Arrange
            String msgCode = null;
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("系统错误，请稍后重试");
        }

        @Test
        @DisplayName("空格错误码 - 返回系统错误消息")
        void testGetErrMsg_空格错误码() {
            // Arrange
            String msgCode = "   ";
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("系统错误，请稍后重试");
        }

        @Test
        @DisplayName("null语言 - 返回英文系统错误消息")
        void testGetErrMsg_null语言() {
            // Arrange
            String msgCode = "GATE0001";
            String lang = null;

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("System error, please try again later");
        }

        @Test
        @DisplayName("空语言 - 返回英文系统错误消息")
        void testGetErrMsg_空语言() {
            // Arrange
            String msgCode = "GATE0001";
            String lang = "";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("System error, please try again later");
        }
    }

    @Nested
    @DisplayName("业务错误码前缀判断测试")
    class BusinessErrorCodeTest {

        @ParameterizedTest
        @ValueSource(strings = {"BUSS0001", "INVALID001", "AUTH0001", "FLOW0001"})
        @DisplayName("业务错误码前缀应返回业务错误消息")
        void testGetErrMsg_业务错误码前缀(String msgCode) {
            // Arrange
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).contains("操作失败");
            assertThat(result).contains(msgCode);
        }

        @ParameterizedTest
        @ValueSource(strings = {"GATE0001", "PARAM0001", "SYS0001", "DB0001"})
        @DisplayName("非业务错误码前缀应返回系统错误消息")
        void testGetErrMsg_非业务错误码前缀(String msgCode) {
            // Arrange
            String lang = "zh_cn";

            // Act
            String result = provider.getErrMsg(msgCode, lang);

            // Assert
            assertThat(result).isEqualTo("系统错误，请稍后重试");
        }
    }
}
