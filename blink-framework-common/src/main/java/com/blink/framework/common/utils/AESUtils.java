package com.blink.framework.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
/**
 * AES 加密解密工具类
 *
 * @Author binblink
 * @Date 2025/8/28
 */
public class AESUtils {


    // CBC 配置
    private static final int CBC_IV_LENGTH = 16;
    private static final int AES_KEY_SIZE = 256;
    private static final String ALGORITHM_CBC = "AES/CBC/PKCS5Padding";

    /**
     * 生成随机 AES 密钥
     */
    public static SecretKey generateRandomAESKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(AES_KEY_SIZE);
        return kg.generateKey();
    }

    /**
     * 生成随机 IV
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[CBC_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    /**
     * 从 Base64 字符串恢复密钥
     */
    public static SecretKey keyFromBase64(String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 从 Base64 字符串恢复 IV
     */
    public static byte[] ivFromBase64(String ivBase64) {
        return Base64.getDecoder().decode(ivBase64);
    }

    /**
     * Base64 编码
     */
    public static String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    // ==================== 核心加密解密方法 ====================

    /**
     * AES-CBC 加密
     */
    public static String encrypt(SecretKey aesKey, byte[] iv, String plaintext) throws Exception {

        Cipher cipher = Cipher.getInstance(ALGORITHM_CBC);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return encodeToBase64(ciphertext);
    }

    /**
     * AES-CBC 解密
     */
    public static String decrypt(SecretKey aesKey, byte[] iv, String encryptedBase64) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        Cipher cipher = Cipher.getInstance(ALGORITHM_CBC);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

        byte[] plaintext = cipher.doFinal(encrypted);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    // ==================== 便捷方法 ====================

    /**
     * 一键加密 - 生成随机密钥和IV并加密
     */
    public static CompleteEncryptionResult encryptComplete(String plaintext) throws Exception {
        SecretKey key = generateRandomAESKey();
        byte[] iv = generateIV();

        String encryptedData = encrypt(key, iv, plaintext);
        String keyBase64 = encodeToBase64(key.getEncoded());
        String ivBase64 = encodeToBase64(iv);

        return new CompleteEncryptionResult(encryptedData, ivBase64, keyBase64);
    }

    /**
     * 一键解密
     */
    public static String decryptComplete(CompleteEncryptionResult result) throws Exception {
        SecretKey key = keyFromBase64(result.getKey());
        byte[] iv = ivFromBase64(result.getIv());
        return decrypt(key, iv, result.getEncryptedData());
    }

    /**
     * 使用Base64字符串参数解密
     */
    public static String decryptWithBase64(String keyBase64, String ivBase64, String encryptedBase64) throws Exception {
        SecretKey key = keyFromBase64(keyBase64);
        byte[] iv = ivFromBase64(ivBase64);
        return decrypt(key, iv, encryptedBase64);
    }

    // ==================== 数据封装类 ====================

    /**
     * 完整加密结果
     */
    public static class CompleteEncryptionResult {
        private final String encryptedData;
        private final String iv;        // Base64编码的IV
        private final String key;       // Base64编码的密钥

        public CompleteEncryptionResult(String encryptedData, String iv, String key) {
            this.encryptedData = encryptedData;
            this.iv = iv;
            this.key = key;
        }

        public String getEncryptedData() { return encryptedData; }
        public String getIv() { return iv; }
        public String getKey() { return key; }

        /**
         * 转换为传输格式
         */
        public String toTransportString() {
            return iv + "::" + encryptedData + "::" + key;
        }

        /**
         * 从传输格式解析
         */
        public static CompleteEncryptionResult fromTransportString(String transportString) {
            String[] parts = transportString.split("::");
            if (parts.length != 3) {
                throw new IllegalArgumentException("无效的传输格式");
            }
            return new CompleteEncryptionResult(parts[1], parts[0], parts[2]);
        }

        /**
         * 转换为JSON格式（便于API传输）
         */
        public String toJson() {
            return String.format(
                    "{\"iv\":\"%s\",\"data\":\"%s\",\"key\":\"%s\"}",
                    iv, encryptedData, key
            );
        }
    }





    // ==================== 使用示例 ====================

    public static void main(String[] args) {
        try {
            String originalText = "这是需要加密的敏感数据";

            System.out.println("=== 随机密钥 + 随机IV AES-CBC 加密测试 ===");
            System.out.println("原始文本: " + originalText);

            // 方法1: 一键加密
            CompleteEncryptionResult result = encryptComplete(originalText);
            System.out.println("加密结果: " + result.getEncryptedData());
            System.out.println("随机IV: " + result.getIv());
            System.out.println("随机密钥: " + result.getKey());

            // 传输测试
            String transportString = result.toTransportString();
            System.out.println("传输格式: " + transportString);

            // 解密
            CompleteEncryptionResult receivedResult = CompleteEncryptionResult.fromTransportString(transportString);
            String decryptedText = decryptComplete(receivedResult);
            System.out.println("解密结果: " + decryptedText);
            System.out.println("验证成功: " + originalText.equals(decryptedText));

            // 方法2: 分别使用密钥和IV
            System.out.println("\n=== 分别使用密钥和IV加密 ===");
            SecretKey key = generateRandomAESKey();
            byte[] iv = generateIV();

            String encrypted = encrypt(key, iv, originalText);
            String decrypted = decrypt(key, iv, encrypted);
            System.out.println("分别加解密验证: " + originalText.equals(decrypted));

            // 性能测试
            System.out.println("\n=== 性能测试 ===");
            long startTime = System.currentTimeMillis();
            int testRounds = 1000;
            for (int i = 0; i < testRounds; i++) {
                CompleteEncryptionResult testResult = encryptComplete("测试数据" + i);
                decryptComplete(testResult);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(testRounds + "次加解密耗时: " + (endTime - startTime) + "ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}