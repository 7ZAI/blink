package com.blink.gateway.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 签名服务配置
 */
@Configuration
@ConfigurationProperties(prefix = "signature")
public class SignatureConfig {

    private String defaultAlgorithm = "HmacSHA256";

    public String getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    public void setDefaultAlgorithm(String defaultAlgorithm) {
        this.defaultAlgorithm = defaultAlgorithm;
    }

    @Bean
    public SignatureServiceFactory signatureServiceFactory() {
        return new SignatureServiceFactory();
    }

    @Bean
    public SignatureService signatureService(SignatureServiceFactory factory) {
        return factory.getService(defaultAlgorithm);
    }
}