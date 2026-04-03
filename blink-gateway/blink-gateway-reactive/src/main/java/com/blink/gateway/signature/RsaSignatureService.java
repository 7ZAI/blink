package com.blink.gateway.signature;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.RSAUtils;

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
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            throw new BlinkException("RSA签名生成失败: " + e.getMessage(), e, BlinkErrorCodeEnum.SIGNATURE_EXCEPTION.getCode());
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
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            throw new BlinkException("RSA签名验证失败: " + e.getMessage(), e, BlinkErrorCodeEnum.SIGNATURE_EXCEPTION.getCode());
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyStr) {
        try {
            return RSAUtils.base64ToPrivateKey(privateKeyStr);
        } catch (Exception e) {
            throw new BlinkException("私钥解析失败: " + e.getMessage(), e, BlinkErrorCodeEnum.SIGNATURE_EXCEPTION.getCode());
        }
    }

    private PublicKey parsePublicKey(String publicKeyStr) {
        try {
            return RSAUtils.base64ToPublicKey(publicKeyStr);
        } catch (Exception e) {
            throw new BlinkException("公钥解析失败: " + e.getMessage(), e, BlinkErrorCodeEnum.SIGNATURE_EXCEPTION.getCode());
        }
    }
}