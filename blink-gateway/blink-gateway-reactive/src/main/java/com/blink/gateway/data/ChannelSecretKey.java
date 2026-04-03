package com.blink.gateway.data;

import lombok.Data;

/**
 * 渠道密钥
 * @Author binblink
 * @Date 2026/2/6
 */
@Data
public class ChannelSecretKey {

    /**
     * 渠道appKey
     */
    private String appKey;

    /**
     * 渠道appSecret
     */
    private String appSecret;

    /**
     * 渠道名称
     */
    private String channelName;

    /**
     * 系统公钥
     */
    private String systemPublickey;

    /**
     * 系统私钥
     */
    private String systemPrivatekey;

    /**
     * token密钥(jwt生成密钥)
     */
    private String tokenSecret;


}
