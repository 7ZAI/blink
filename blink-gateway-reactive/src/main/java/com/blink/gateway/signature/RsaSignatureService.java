package com.blink.gateway.signature;

import java.security.*;
import java.util.Base64;
import java.util.Map;

/**
 * RSA 签名实现
 */
public class RsaSignatureService extends AbstractSignatureService {

    public RsaSignatureService() {
        super("SHA256withRSA");
    }

    @Override
    public String sign(String data, String secret) {
        // 注意：这里的secret应该是私钥字符串
        try {
            PrivateKey privateKey = parsePrivateKey(secret);
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            byte[] digitalSignature = signature.sign();
            return Base64.getEncoder().encodeToString(digitalSignature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA signature", e);
        }
    }

    @Override
    public boolean verify(String data, String secret, String signature) {
        // 注意：这里的secret应该是公钥字符串
        try {
            PublicKey publicKey = parsePublicKey(secret);
            Signature verifySignature = Signature.getInstance(algorithm);
            verifySignature.initVerify(publicKey);
            verifySignature.update(data.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return verifySignature.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify RSA signature", e);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyStr) {
        // 实现私钥解析逻辑
        // 这里需要根据您的私钥格式实现
        throw new UnsupportedOperationException("Private key parsing not implemented");
    }

    private PublicKey parsePublicKey(String publicKeyStr) {
        // 实现公钥解析逻辑
        // 这里需要根据您的公钥格式实现
        throw new UnsupportedOperationException("Public key parsing not implemented");
    }
}