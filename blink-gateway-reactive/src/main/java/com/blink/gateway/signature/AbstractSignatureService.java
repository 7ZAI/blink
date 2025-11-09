package com.blink.gateway.signature;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 签名服务抽象基类
 * 提供一些通用功能
 */
public abstract class AbstractSignatureService implements SignatureService {

    protected final String algorithm;

    protected AbstractSignatureService(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithmName() {
        return algorithm;
    }

    /**
     * 构建待签名字符串
     * 子类可重写此方法以实现自定义的字符串构建逻辑
     */
    protected String buildStringToSign(String data, Map<String, Object> params) {

        StringJoiner joiner = new StringJoiner("&");
        if (params == null || params.isEmpty()) {
            return data;
        }

        if(StrUtil.isNotBlank(data)){
            joiner.add(data);
        }
        // 默认实现：将参数按key排序后拼接到数据后面
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    joiner.add(entry.getKey() + "=" + entry.getValue());
                });

        return joiner.toString();
    }

}