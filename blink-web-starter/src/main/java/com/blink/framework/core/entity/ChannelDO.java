package com.blink.framework.core.entity;


import lombok.Data;


/**
 * @Author binblink
 * @Date 2025/8/28
 */
@Data
public class ChannelDO {

    /**
     * 渠道名
     */
    private String channelName;

    /**
     * 应用key值
     */
    private String appKey;

    /**
     * 应用秘钥
     */
    private String appSecret;

    /**
     * 关联用户
     */
    private String relaUserId;

    /**
     * 认证token
     */
    private String accessToken;

    /**
     * 系统私钥
     */
    private String systemPrivatekey;

    /**
     * 渠道公钥
     */
    private String channelPublickey;


    /**
     * 渠道开关 0 开启 1关闭
     */
    private Byte enable;

    /**
     * 加密开关 0 开启 1关闭
     */
    private Byte encryptionSwitch;

    /**
     * 认证token过期开关 0 开启 1关闭
     */
    private Byte tokenTimeoutSwitch;

    /**
     * 权限校验开关 0 开启 1关闭
     */
    private Byte authoritySwitch;




}
