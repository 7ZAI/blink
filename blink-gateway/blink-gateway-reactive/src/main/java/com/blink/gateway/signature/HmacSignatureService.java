package com.blink.gateway.signature;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * HMAC 签名实现
 */
@Slf4j
public class HmacSignatureService extends AbstractSignatureService {


    public HmacSignatureService() {
        super("HmacSHA256");
    }

    public HmacSignatureService(String algorithm) {
        super(algorithm);
    }

    @Override
    public String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKeySpec);
            byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BlinkException("HMAC签名生成失败: " + e.getMessage(), e, BlinkErrorCodeEnum.SIGNATURE_EXCEPTION.getCode());
        }
    }

    @Override
    public boolean verify(String data, String secret, String signature) {
        String expectedSignature = sign(data, secret);
        return constantTimeCompare(expectedSignature, signature);
    }

    @Override
    public String sign(String data, String secret, Map<String, Object> params) {
        String stringToSign = buildStringToSign(data, params);
        log.debug("sign stringToSign :{}", stringToSign);
        return sign(stringToSign, secret);
    }

    @Override
    public boolean verify(String data, String secret, String signature, Map<String, Object> params) {
        String stringToSign = buildStringToSign(data, params);
        log.debug("verify stringToSign :{}", stringToSign);
        return verify(stringToSign, secret, signature);
    }

    /**
     * 安全地比较两个字符串，防止时序攻击
     */
    private boolean constantTimeCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}