package com.blink.gateway.signature;

import java.security.*;
import java.util.Base64;
import java.util.Map;

/**
 * ECDSA 签名实现
 */
public class EcdsaSignatureService extends AbstractSignatureService {

    public EcdsaSignatureService() {
        super("SHA256withECDSA");
    }

    @Override
    public String sign(String data, String secret) {
        // 实现ECDSA签名逻辑
        // 类似于RSA实现，但使用ECDSA算法
        throw new UnsupportedOperationException("ECDSA signing not implemented");
    }

    @Override
    public boolean verify(String data, String secret, String signature) {
        // 实现ECDSA验证逻辑
        // 类似于RSA实现，但使用ECDSA算法
        throw new UnsupportedOperationException("ECDSA verification not implemented");
    }
}