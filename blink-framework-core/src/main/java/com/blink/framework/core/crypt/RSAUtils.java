package com.blink.framework.core.crypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 工具类，推荐使用 RSA/ECB/OAEPWithSHA-256AndMGF1Padding
 *
 * @Author binblink
 * @Date 2025/8/30
 */
public class RSAUtils {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int KEY_SIZE = 2048;

    // ====== RSA 基础方法 ======

    /**
     * 生成 RSA 密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(KEY_SIZE);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成 RSA 密钥对失败", e);
        }
    }

    public static String generatePublicKeyToBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public static String generatePrivateKeyToBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /**
     * 公钥加密（Base64 输出）
     */
    public static String encryptToBase64(String plaintext, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }

    /**
     * 私钥解密（Base64 输入）
     */
    public static String decryptFromBase64(String ciphertextBase64, PrivateKey privateKey) {
        try {
            byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA 解密失败", e);
        }
    }

    // ====== 密钥导出/导入 ======

    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public static PublicKey base64ToPublicKey(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Base64 公钥解析失败", e);
        }
    }

    public static PrivateKey base64ToPrivateKey(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Base64 私钥解析失败", e);
        }
    }

}

