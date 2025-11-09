package com.blink.gateway.signature;

import java.util.HashMap;
import java.util.Map;

/**
 * 签名服务工厂
 * 用于创建和管理不同的签名服务实例
 */
public class SignatureServiceFactory {

    private final Map<String, SignatureService> services = new HashMap<>();

    public SignatureServiceFactory() {
        // 注册默认的签名服务
        registerService("HmacSHA256", new HmacSignatureService());
        registerService("SHA256withRSA", new RsaSignatureService());
        registerService("SHA256withECDSA", new EcdsaSignatureService());
    }

    /**
     * 注册签名服务
     */
    public void registerService(String algorithm, SignatureService service) {
        services.put(algorithm, service);
    }

    /**
     * 获取签名服务
     */
    public SignatureService getService(String algorithm) {
        SignatureService service = services.get(algorithm);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported signature algorithm: " + algorithm);
        }
        return service;
    }

    /**
     * 获取默认的签名服务
     */
    public SignatureService getDefaultService() {
        return getService("HmacSHA256");
    }
}