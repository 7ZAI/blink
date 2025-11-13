package com.blink.framework.core.crypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加密解密工具类
 *
 * @Author binblink
 * @Date 2025/8/28
 */
public class AESUtils {
    // 推荐 12 字节
    private static final int GCM_IV_LENGTH = 12;
    //aes key 长度
    private static final int AES_KEY_SIZE = 256;
    // 128 位认证标签
    private static final int GCM_TAG_LENGTH = 128;

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    public static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        // 生成 AES 密钥
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(AES_KEY_SIZE);
        return kg.generateKey();
    }


    public static byte[] generateIV() throws NoSuchAlgorithmException {
        //  生成 随机变量 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);
        return iv;
    }

    public static String encodeToBase64String(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 生成 AAD 字符串
     */
    public static byte[] buildAAD(String userId, String token, String protocol) {
        String aad = "protocol=" + protocol + ";userId=" + userId;
        if (token != null && !token.isEmpty()) {
            aad += ";token=" + token;
        }
        return aad.getBytes(StandardCharsets.UTF_8);
    }


    // AES-GCM 加密
    public static String encrypt(SecretKey aesKey, byte[] iv, String plaintext) throws Exception {

        Cipher aesCipher = Cipher.getInstance(ALGORITHM);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    // AES-GCM 解密
    public static String decrypt(String keyBase64, String ivBase64, String encryptedBase64) throws Exception {

        SecretKey aesKey = new SecretKeySpec(Base64.getDecoder().decode(keyBase64), "AES");

        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        byte[] iv = Base64.getDecoder().decode(ivBase64);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] plaintext = cipher.doFinal(encrypted);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    // 带有关联数据的加密（AAD）
    public static String encryptWithAad(SecretKey key, byte[] iv, String plaintext, byte[] aad) throws Exception {

        Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        // 设置关联数据（不加密但认证）
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    // 带有关联数据的解密
    public static String decryptWithAad(String keyBase64, String ivBase64, String encryptedBase64, byte[] aad) throws Exception {

        SecretKey aesKey = new SecretKeySpec(Base64.getDecoder().decode(keyBase64), "AES");
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        // 设置关联数据
        if (aad != null) {
            cipher.updateAAD(aad);
        }

        byte[] plaintext = cipher.doFinal(encrypted);
        return new String(plaintext, StandardCharsets.UTF_8);
    }


}