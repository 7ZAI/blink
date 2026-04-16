package com.blink.framework.common.utils;

import com.blink.framework.test.annotation.UnitTest;
import com.blink.framework.test.base.BlinkUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.*;

/**
 * RSAUtils 单元测试
 * <p>
 * 测试覆盖：
 * 1. 密钥对生成
 * 2. 公钥加密私钥解密
 * 3. 密钥Base64转换
 * 4. 边界条件与异常处理
 *
 * @author binblink
 */
@UnitTest
@DisplayName("RSAUtils 加密解密工具类测试")
class RSAUtilsTest extends BlinkUnitTest {

    private static final String TEST_PLAINTEXT = "Hello, RSA Encryption!";

    @Nested
    @DisplayName("密钥对生成测试")
    class KeyPairGenerationTests {

        @Test
        @DisplayName("应该成功生成RSA密钥对")
        void shouldGenerateKeyPair() {
            // when
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // then
            assertThat(keyPair).isNotNull();
            assertThat(keyPair.getPublic()).isNotNull();
            assertThat(keyPair.getPrivate()).isNotNull();
        }

        @Test
        @DisplayName("生成的公钥应该是2048位")
        void shouldGenerate2048BitPublicKey() {
            // when
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // then
            // 2048位 = 256字节
            assertThat(keyPair.getPublic().getEncoded().length).isGreaterThan(200);
        }

        @Test
        @DisplayName("每次生成的密钥对应该不同")
        void shouldGenerateDifferentKeyPairs() {
            // when
            KeyPair keyPair1 = RSAUtils.generateKeyPair();
            KeyPair keyPair2 = RSAUtils.generateKeyPair();

            // then
            assertThat(keyPair1.getPublic().getEncoded())
                    .isNotEqualTo(keyPair2.getPublic().getEncoded());
            assertThat(keyPair1.getPrivate().getEncoded())
                    .isNotEqualTo(keyPair2.getPrivate().getEncoded());
        }
    }

    @Nested
    @DisplayName("密钥Base64转换测试")
    class KeyBase64ConversionTests {

        @Test
        @DisplayName("应该正确将公钥转换为Base64字符串")
        void shouldConvertPublicKeyToBase64() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String base64 = RSAUtils.generatePublicKeyToBase64(keyPair);

            // then
            assertThat(base64).isNotEmpty();
            assertThatCode(() -> java.util.Base64.getDecoder().decode(base64))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该正确将私钥转换为Base64字符串")
        void shouldConvertPrivateKeyToBase64() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String base64 = RSAUtils.generatePrivateKeyToBase64(keyPair);

            // then
            assertThat(base64).isNotEmpty();
            assertThatCode(() -> java.util.Base64.getDecoder().decode(base64))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该从Base64字符串恢复公钥")
        void shouldRestorePublicKeyFromBase64() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();
            String base64 = RSAUtils.publicKeyToBase64(keyPair.getPublic());

            // when
            PublicKey restoredKey = RSAUtils.base64ToPublicKey(base64);

            // then
            assertThat(restoredKey.getEncoded())
                    .isEqualTo(keyPair.getPublic().getEncoded());
        }

        @Test
        @DisplayName("应该从Base64字符串恢复私钥")
        void shouldRestorePrivateKeyFromBase64() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();
            String base64 = RSAUtils.privateKeyToBase64(keyPair.getPrivate());

            // when
            PrivateKey restoredKey = RSAUtils.base64ToPrivateKey(base64);

            // then
            assertThat(restoredKey.getEncoded())
                    .isEqualTo(keyPair.getPrivate().getEncoded());
        }

        @Test
        @DisplayName("无效Base64公钥应该抛出异常")
        void shouldThrowExceptionForInvalidPublicKeyBase64() {
            // when & then
            assertThatThrownBy(() -> RSAUtils.base64ToPublicKey("invalid-base64!!!"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("公钥解析失败");
        }

        @Test
        @DisplayName("无效Base64私钥应该抛出异常")
        void shouldThrowExceptionForInvalidPrivateKeyBase64() {
            // when & then
            assertThatThrownBy(() -> RSAUtils.base64ToPrivateKey("invalid-base64!!!"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("私钥解析失败");
        }
    }

    @Nested
    @DisplayName("加密解密测试")
    class EncryptionDecryptionTests {

        @Test
        @DisplayName("应该成功使用公钥加密私钥解密")
        void shouldEncryptWithPublicKeyAndDecryptWithPrivateKey() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64(TEST_PLAINTEXT, keyPair.getPublic());
            String decrypted = RSAUtils.decryptFromBase64(encrypted, keyPair.getPrivate());

            // then
            assertThat(encrypted).isNotEmpty();
            assertThat(encrypted).isNotEqualTo(TEST_PLAINTEXT);
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
        }

        @Test
        @DisplayName("加密结果应该是Base64格式")
        void shouldReturnBase64EncodedCiphertext() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64(TEST_PLAINTEXT, keyPair.getPublic());

            // then
            assertThatCode(() -> java.util.Base64.getDecoder().decode(encrypted))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("相同明文和公钥加密结果应该一致")
        void shouldProduceSameCiphertextForSamePlaintextAndKey() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted1 = RSAUtils.encryptToBase64(TEST_PLAINTEXT, keyPair.getPublic());
            String encrypted2 = RSAUtils.encryptToBase64(TEST_PLAINTEXT, keyPair.getPublic());

            // then
            // RSA-OAEP使用随机填充，所以每次加密结果不同
            // 但解密后应该得到相同结果
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            String decrypted1 = RSAUtils.decryptFromBase64(encrypted1, keyPair.getPrivate());
            String decrypted2 = RSAUtils.decryptFromBase64(encrypted2, keyPair.getPrivate());

            assertThat(decrypted1).isEqualTo(TEST_PLAINTEXT);
            assertThat(decrypted2).isEqualTo(TEST_PLAINTEXT);
        }

        @Test
        @DisplayName("空字符串应该能正确加密解密")
        void shouldHandleEmptyString() {
            // given
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64("", keyPair.getPublic());
            String decrypted = RSAUtils.decryptFromBase64(encrypted, keyPair.getPrivate());

            // then
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("中文字符应该能正确加密解密")
        void shouldHandleChineseCharacters() {
            // given
            String chineseText = "你好，世界！这是一段中文测试。";
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64(chineseText, keyPair.getPublic());
            String decrypted = RSAUtils.decryptFromBase64(encrypted, keyPair.getPrivate());

            // then
            assertThat(decrypted).isEqualTo(chineseText);
        }

        @Test
        @DisplayName("使用错误的私钥解密应该失败")
        void shouldFailToDecryptWithWrongPrivateKey() {
            // given
            KeyPair keyPair1 = RSAUtils.generateKeyPair();
            KeyPair keyPair2 = RSAUtils.generateKeyPair();
            String encrypted = RSAUtils.encryptToBase64(TEST_PLAINTEXT, keyPair1.getPublic());

            // when & then
            assertThatThrownBy(() ->
                    RSAUtils.decryptFromBase64(encrypted, keyPair2.getPrivate()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("解密失败");
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("最大长度明文应该能正确加密解密")
        void shouldHandleMaxLengthPlaintext() {
            // RSA 2048位密钥，OAEP填充（SHA-256），最大加密长度 = (keySize/8) - 2*hashLen - 2 = 190字节
            // given
            int maxLen = 190;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxLen; i++) {
                sb.append('A');
            }
            String maxPlaintext = sb.toString();
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64(maxPlaintext, keyPair.getPublic());
            String decrypted = RSAUtils.decryptFromBase64(encrypted, keyPair.getPrivate());

            // then
            assertThat(decrypted).isEqualTo(maxPlaintext);
        }

        @Test
        @DisplayName("超过最大长度应该抛出异常")
        void shouldThrowExceptionWhenExceedingMaxLength() {
            // given - 200字节超过190字节限制
            int overLen = 200;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < overLen; i++) {
                sb.append('A');
            }
            String tooLongPlaintext = sb.toString();
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when & then
            assertThatThrownBy(() ->
                    RSAUtils.encryptToBase64(tooLongPlaintext, keyPair.getPublic()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("加密失败");
        }

        @Test
        @DisplayName("特殊字符应该能正确加密解密")
        void shouldHandleSpecialCharacters() {
            // given
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`";
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // when
            String encrypted = RSAUtils.encryptToBase64(specialChars, keyPair.getPublic());
            String decrypted = RSAUtils.decryptFromBase64(encrypted, keyPair.getPrivate());

            // then
            assertThat(decrypted).isEqualTo(specialChars);
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullProcessTests {

        @Test
        @DisplayName("应该完成密钥生成、Base64转换、加密解密完整流程")
        void shouldCompleteFullProcess() {
            // when - 生成密钥对
            KeyPair keyPair = RSAUtils.generateKeyPair();

            // and - 导出为Base64
            String publicKeyBase64 = RSAUtils.publicKeyToBase64(keyPair.getPublic());
            String privateKeyBase64 = RSAUtils.privateKeyToBase64(keyPair.getPrivate());

            // and - 从Base64恢复
            PublicKey restoredPublicKey = RSAUtils.base64ToPublicKey(publicKeyBase64);
            PrivateKey restoredPrivateKey = RSAUtils.base64ToPrivateKey(privateKeyBase64);

            // and - 使用恢复的密钥加密解密
            String encrypted = RSAUtils.encryptToBase64(TEST_PLAINTEXT, restoredPublicKey);
            String decrypted = RSAUtils.decryptFromBase64(encrypted, restoredPrivateKey);

            // then
            assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
        }
    }
}
