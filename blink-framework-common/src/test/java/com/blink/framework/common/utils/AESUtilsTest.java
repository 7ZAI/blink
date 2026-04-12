package com.blink.framework.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * AESUtils 单元测试
 * <p>
 * 测试覆盖：
 * 1. ECB模式加密解密
 * 2. CBC模式加密解密
 * 3. 密钥生成与恢复
 * 4. IV生成与恢复
 * 5. 完整加密解密流程
 * 6. CompleteEncryptionResult传输格式
 *
 * @author binblink
 */
@DisplayName("AESUtils 加密解密工具类测试")
class AESUtilsTest {

    private static final String TEST_PLAINTEXT = "Hello, Blink Framework!";
    private static final String TEST_KEY_STRING = "ThisIsASecretKey1234567890123456"; // 32字节
    private static final int KEY_SIZE_128 = 16;
    private static final int KEY_SIZE_192 = 24;
    private static final int KEY_SIZE_256 = 32;

    @Nested
    @DisplayName("ECB模式测试")
    class EcbModeTests {

        @Test
        @DisplayName("应该成功加密和解密明文")
        void shouldEncryptAndDecryptSuccessfully() throws Exception {
            // given - 使用随机生成的密钥更安全
            SecretKey secretKey = AESUtils.generateRandomAESKey();

            // when
            String encrypted = AESUtils.encrypt(secretKey, TEST_PLAINTEXT);
            String decrypted = AESUtils.decrypt(secretKey, encrypted);

            // then
            assertThat(encrypted).isNotEmpty();
            assertThat(encrypted).isNotEqualTo(TEST_PLAINTEXT);
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
        }

        @Test
        @DisplayName("加密结果应该是Base64格式")
        void shouldReturnBase64EncodedString() throws Exception {
            // given
            SecretKey secretKey = AESUtils.generateRandomAESKey();

            // when
            String encrypted = AESUtils.encrypt(secretKey, TEST_PLAINTEXT);

            // then
            assertThatCode(() -> Base64.getDecoder().decode(encrypted))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ECB模式下相同明文和密钥应该产生相同密文")
        void shouldProduceConsistentEncryptionInEcbMode() throws Exception {
            // given
            SecretKey secretKey = AESUtils.generateRandomAESKey();

            // ECB模式是确定性的，相同明文+相同密钥应该产生相同密文
            String encrypted1 = AESUtils.encrypt(secretKey, TEST_PLAINTEXT);
            String encrypted2 = AESUtils.encrypt(secretKey, TEST_PLAINTEXT);

            assertThat(encrypted1).isEqualTo(encrypted2);
        }

        @Test
        @DisplayName("空字符串应该能正确加密解密")
        void shouldHandleEmptyString() throws Exception {
            // given
            SecretKey secretKey = AESUtils.generateRandomAESKey();

            // when
            String encrypted = AESUtils.encrypt(secretKey, "");
            String decrypted = AESUtils.decrypt(secretKey, encrypted);

            // then
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("中文字符应该能正确加密解密")
        void shouldHandleChineseCharacters() throws Exception {
            // given
            SecretKey secretKey = AESUtils.generateRandomAESKey();
            String chineseText = "你好，世界！这是一段中文测试文本。";

            // when
            String encrypted = AESUtils.encrypt(secretKey, chineseText);
            String decrypted = AESUtils.decrypt(secretKey, encrypted);

            // then
            assertThat(decrypted).isEqualTo(chineseText);
        }
    }

    @Nested
    @DisplayName("CBC模式测试")
    class CbcModeTests {

        private SecretKey secretKey;
        private byte[] iv;

        @BeforeEach
        void setUp() throws Exception {
            secretKey = AESUtils.generateRandomAESKey();
            iv = AESUtils.generateIV();
        }

        @Test
        @DisplayName("应该成功加密和解密明文")
        void shouldEncryptAndDecryptSuccessfully() throws Exception {
            // when
            String encrypted = AESUtils.encrypt(secretKey, iv, TEST_PLAINTEXT);
            String decrypted = AESUtils.decrypt(secretKey, iv, encrypted);

            // then
            assertThat(encrypted).isNotEmpty();
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
        }

        @Test
        @DisplayName("不同IV应该产生不同密文")
        void shouldProduceDifferentCiphertextWithDifferentIv() throws Exception {
            // given
            byte[] iv1 = AESUtils.generateIV();
            byte[] iv2 = AESUtils.generateIV();

            // when
            String encrypted1 = AESUtils.encrypt(secretKey, iv1, TEST_PLAINTEXT);
            String encrypted2 = AESUtils.encrypt(secretKey, iv2, TEST_PLAINTEXT);

            // then
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("相同IV应该产生相同密文")
        void shouldProduceSameCiphertextWithSameIv() throws Exception {
            // when
            String encrypted1 = AESUtils.encrypt(secretKey, iv, TEST_PLAINTEXT);
            String encrypted2 = AESUtils.encrypt(secretKey, iv, TEST_PLAINTEXT);

            // then
            assertThat(encrypted1).isEqualTo(encrypted2);
        }

        @Test
        @DisplayName("使用错误的IV解密应该得到不同结果")
        void shouldFailToDecryptWithWrongIv() throws Exception {
            // given
            String encrypted = AESUtils.encrypt(secretKey, iv, TEST_PLAINTEXT);
            byte[] wrongIv = AESUtils.generateIV();

            // when & then
            String decrypted = AESUtils.decrypt(secretKey, wrongIv, encrypted);
            assertThat(decrypted).isNotEqualTo(TEST_PLAINTEXT);
        }
    }

    @Nested
    @DisplayName("密钥生成测试")
    class KeyGenerationTests {

        @Test
        @DisplayName("应该从字符串生成指定长度密钥")
        void shouldGenerateKeyFromString() throws Exception {
            // when
            SecretKey key128 = AESUtils.generateKey(TEST_KEY_STRING, KEY_SIZE_128);
            SecretKey key192 = AESUtils.generateKey(TEST_KEY_STRING, KEY_SIZE_192);
            SecretKey key256 = AESUtils.generateKey(TEST_KEY_STRING, KEY_SIZE_256);

            // then
            assertThat(key128.getEncoded()).hasSize(KEY_SIZE_128);
            assertThat(key192.getEncoded()).hasSize(KEY_SIZE_192);
            assertThat(key256.getEncoded()).hasSize(KEY_SIZE_256);
        }

        @Test
        @DisplayName("短密钥字符串应该被填充到指定长度")
        void shouldPadShortKeyString() throws Exception {
            // given
            String shortKey = "short";

            // when
            SecretKey key = AESUtils.generateKey(shortKey, KEY_SIZE_256);

            // then
            assertThat(key.getEncoded().length).isEqualTo(KEY_SIZE_256);
        }

        @Test
        @DisplayName("应该生成随机AES密钥")
        void shouldGenerateRandomAesKey() throws Exception {
            // when
            SecretKey key1 = AESUtils.generateRandomAESKey();
            SecretKey key2 = AESUtils.generateRandomAESKey();

            // then
            assertThat(key1.getEncoded().length).isEqualTo(KEY_SIZE_256);
            assertThat(key2.getEncoded().length).isEqualTo(KEY_SIZE_256);
            // 两次生成的密钥应该不同
            assertThat(key1.getEncoded()).isNotEqualTo(key2.getEncoded());
        }

        @Test
        @DisplayName("应该生成指定长度的随机密钥")
        void shouldGenerateRandomKeyWithSpecifiedSize() throws Exception {
            // when
            SecretKey key128 = AESUtils.generateRandomKey(128);
            SecretKey key192 = AESUtils.generateRandomKey(192);
            SecretKey key256 = AESUtils.generateRandomKey(256);

            // then
            assertThat(key128.getEncoded().length).isEqualTo(16);
            assertThat(key192.getEncoded().length).isEqualTo(24);
            assertThat(key256.getEncoded().length).isEqualTo(32);
        }
    }

    @Nested
    @DisplayName("IV生成测试")
    class IvGenerationTests {

        @Test
        @DisplayName("应该生成16字节IV")
        void shouldGenerate16ByteIv() {
            // when
            byte[] iv = AESUtils.generateIV();

            // then
            assertThat(iv.length).isEqualTo(16);
        }

        @Test
        @DisplayName("每次应该生成不同的IV")
        void shouldGenerateDifferentIvEachTime() {
            // when
            byte[] iv1 = AESUtils.generateIV();
            byte[] iv2 = AESUtils.generateIV();

            // then
            assertThat(iv1).isNotEqualTo(iv2);
        }
    }

    @Nested
    @DisplayName("Base64转换测试")
    class Base64ConversionTests {

        @Test
        @DisplayName("应该正确从Base64恢复密钥")
        void shouldRestoreKeyFromBase64() throws Exception {
            // given
            SecretKey originalKey = AESUtils.generateRandomAESKey();
            String keyBase64 = AESUtils.encodeToBase64(originalKey.getEncoded());

            // when
            SecretKey restoredKey = AESUtils.keyFromBase64(keyBase64);

            // then
            assertThat(restoredKey.getEncoded()).isEqualTo(originalKey.getEncoded());
        }

        @Test
        @DisplayName("应该正确从Base64恢复IV")
        void shouldRestoreIvFromBase64() {
            // given
            byte[] originalIv = AESUtils.generateIV();
            String ivBase64 = AESUtils.encodeToBase64(originalIv);

            // when
            byte[] restoredIv = AESUtils.ivFromBase64(ivBase64);

            // then
            assertThat(restoredIv).isEqualTo(originalIv);
        }

        @Test
        @DisplayName("应该正确编码字节数组为Base64")
        void shouldEncodeBytesToBase64() {
            // given
            byte[] bytes = {1, 2, 3, 4, 5};

            // when
            String base64 = AESUtils.encodeToBase64(bytes);

            // then
            assertThat(base64).isNotEmpty();
            assertThat(Base64.getDecoder().decode(base64)).isEqualTo(bytes);
        }
    }

    @Nested
    @DisplayName("完整加密测试")
    class CompleteEncryptionTests {

        @Test
        @DisplayName("应该完成完整的加密解密流程")
        void shouldCompleteFullEncryptionCycle() throws Exception {
            // when
            AESUtils.CompleteEncryptionResult result = AESUtils.encryptComplete(TEST_PLAINTEXT);
            String decrypted = AESUtils.decryptComplete(result);

            // then
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
            assertThat(result.getEncryptedData()).isNotEmpty();
            assertThat(result.getIv()).isNotEmpty();
            assertThat(result.getKey()).isNotEmpty();
        }

        @Test
        @DisplayName("应该使用Base64参数解密")
        void shouldDecryptWithBase64Params() throws Exception {
            // given
            AESUtils.CompleteEncryptionResult result = AESUtils.encryptComplete(TEST_PLAINTEXT);

            // when
            String decrypted = AESUtils.decryptWithBase64(
                    result.getKey(),
                    result.getIv(),
                    result.getEncryptedData()
            );

            // then
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
        }
    }

    @Nested
    @DisplayName("CompleteEncryptionResult测试")
    class CompleteEncryptionResultTests {

        @Test
        @DisplayName("应该正确转换为传输字符串并解析")
        void shouldConvertToTransportStringAndParse() throws Exception {
            // given
            AESUtils.CompleteEncryptionResult original = AESUtils.encryptComplete(TEST_PLAINTEXT);

            // when
            String transportString = original.toTransportString();
            AESUtils.CompleteEncryptionResult parsed =
                    AESUtils.CompleteEncryptionResult.fromTransportString(transportString);

            // then
            assertThat(parsed.getEncryptedData()).isEqualTo(original.getEncryptedData());
            assertThat(parsed.getIv()).isEqualTo(original.getIv());
            assertThat(parsed.getKey()).isEqualTo(original.getKey());
        }

        @Test
        @DisplayName("无效传输格式应该抛出异常")
        void shouldThrowExceptionForInvalidTransportString() {
            // given
            String invalidString = "invalid::format"; // 缺少第三部分

            // when & then
            assertThatThrownBy(() ->
                    AESUtils.CompleteEncryptionResult.fromTransportString(invalidString))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的传输格式");
        }

        @Test
        @DisplayName("应该正确转换为JSON格式")
        void shouldConvertToJson() throws Exception {
            // given
            AESUtils.CompleteEncryptionResult result = AESUtils.encryptComplete(TEST_PLAINTEXT);

            // when
            String json = result.toJson();

            // then
            assertThat(json).contains("\"iv\"");
            assertThat(json).contains("\"data\"");
            assertThat(json).contains("\"key\"");
        }

        @Test
        @DisplayName("传输字符串各部分顺序应该正确")
        void shouldMaintainCorrectOrderInTransportString() throws Exception {
            // given
            AESUtils.CompleteEncryptionResult result = AESUtils.encryptComplete(TEST_PLAINTEXT);

            // when
            String transportString = result.toTransportString();

            // then
            // 格式：iv::encryptedData::key
            String[] parts = transportString.split("::");
            assertThat(parts).hasSize(3);
            assertThat(parts[0]).isEqualTo(result.getIv());
            assertThat(parts[1]).isEqualTo(result.getEncryptedData());
            assertThat(parts[2]).isEqualTo(result.getKey());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("长文本应该能正确加密解密")
        void shouldHandleLongText() throws Exception {
            // given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("这是一段很长的文本。");
            }
            String longText = sb.toString();

            SecretKey key = AESUtils.generateRandomAESKey();
            byte[] iv = AESUtils.generateIV();

            // when
            String encrypted = AESUtils.encrypt(key, iv, longText);
            String decrypted = AESUtils.decrypt(key, iv, encrypted);

            // then
            assertThat(decrypted).isEqualTo(longText);
        }

        @Test
        @DisplayName("特殊字符应该能正确加密解密")
        void shouldHandleSpecialCharacters() throws Exception {
            // given
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`";

            SecretKey key = AESUtils.generateRandomAESKey();
            byte[] iv = AESUtils.generateIV();

            // when
            String encrypted = AESUtils.encrypt(key, iv, specialChars);
            String decrypted = AESUtils.decrypt(key, iv, encrypted);

            // then
            assertThat(decrypted).isEqualTo(specialChars);
        }

        @Test
        @DisplayName("使用错误的密钥解密应该失败")
        void shouldFailToDecryptWithWrongKey() throws Exception {
            // given
            SecretKey correctKey = AESUtils.generateRandomAESKey();
            SecretKey wrongKey = AESUtils.generateRandomAESKey();
            byte[] iv = AESUtils.generateIV();

            String encrypted = AESUtils.encrypt(correctKey, iv, TEST_PLAINTEXT);

            // when & then
            assertThatThrownBy(() -> AESUtils.decrypt(wrongKey, iv, encrypted))
                    .isInstanceOf(Exception.class);
        }
    }
}
