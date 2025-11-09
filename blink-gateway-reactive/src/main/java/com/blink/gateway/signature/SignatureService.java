package com.blink.gateway.signature;

import java.util.Map;

/**
 * 签名接口
 * 用于抽象不同的签名算法实现
 */
public interface SignatureService {

    /**
     * 获取签名算法名称
     */
    String getAlgorithmName();

    /**
     * 生成签名
     *
     * @param data      待签名数据
     * @param secret    密钥
     * @return 签名结果
     */
    String sign(String data, String secret);

    /**
     * 验证签名
     *
     * @param data      原始数据
     * @param secret    密钥
     * @param signature 待验证的签名
     * @return 验证结果
     */
    boolean verify(String data, String secret, String signature);

    /**
     * 生成签名（支持额外参数）
     *
     * @param data      待签名数据
     * @param secret    密钥
     * @param params    额外参数（如时间戳、随机数等）
     * @return 签名结果
     */
    default String sign(String data, String secret, Map<String, Object> params) {
        // 默认实现忽略额外参数，子类可重写
        return sign(data, secret);
    }

    /**
     * 验证签名（支持额外参数）
     *
     * @param data      原始数据
     * @param secret    密钥
     * @param signature 待验证的签名
     * @param params    额外参数（如时间戳、随机数等）
     * @return 验证结果
     */
    default boolean verify(String data, String secret, String signature, Map<String, Object> params) {
        // 默认实现忽略额外参数，子类可重写
        return verify(data, secret, signature);
    }
}