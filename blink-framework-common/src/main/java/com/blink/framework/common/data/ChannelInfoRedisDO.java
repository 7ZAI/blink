package com.blink.framework.common.data;


import java.io.Serializable;

/**
 * <p>
 * 对接渠道
 * </p>
 *
 * @author binblink
 */
public class ChannelInfoRedisDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 渠道ID
     */
    private String channelId;

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
     * 系统公钥
     */
    private String systemPublickey;

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

    /**
     * 备注
     */
    private String remark;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getRelaUserId() {
        return relaUserId;
    }

    public void setRelaUserId(String relaUserId) {
        this.relaUserId = relaUserId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSystemPublickey() {
        return systemPublickey;
    }

    public void setSystemPublickey(String systemPublickey) {
        this.systemPublickey = systemPublickey;
    }

    public String getSystemPrivatekey() {
        return systemPrivatekey;
    }

    public void setSystemPrivatekey(String systemPrivatekey) {
        this.systemPrivatekey = systemPrivatekey;
    }

    public String getChannelPublickey() {
        return channelPublickey;
    }

    public void setChannelPublickey(String channelPublickey) {
        this.channelPublickey = channelPublickey;
    }

    public Byte getEnable() {
        return enable;
    }

    public void setEnable(Byte enable) {
        this.enable = enable;
    }

    public Byte getEncryptionSwitch() {
        return encryptionSwitch;
    }

    public void setEncryptionSwitch(Byte encryptionSwitch) {
        this.encryptionSwitch = encryptionSwitch;
    }

    public Byte getTokenTimeoutSwitch() {
        return tokenTimeoutSwitch;
    }

    public void setTokenTimeoutSwitch(Byte tokenTimeoutSwitch) {
        this.tokenTimeoutSwitch = tokenTimeoutSwitch;
    }

    public Byte getAuthoritySwitch() {
        return authoritySwitch;
    }

    public void setAuthoritySwitch(Byte authoritySwitch) {
        this.authoritySwitch = authoritySwitch;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ChannelInfoRedisDO{" +
                "channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", appKey='" + appKey + '\'' +
                ", appSecret='" + appSecret + '\'' +
                ", relaUserId='" + relaUserId + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", systemPublickey='" + systemPublickey + '\'' +
                ", systemPrivatekey='" + systemPrivatekey + '\'' +
                ", channelPublickey='" + channelPublickey + '\'' +
                ", enable=" + enable +
                ", encryptionSwitch=" + encryptionSwitch +
                ", tokenTimeoutSwitch=" + tokenTimeoutSwitch +
                ", authoritySwitch=" + authoritySwitch +
                ", remark='" + remark + '\'' +
                '}';
    }
}
