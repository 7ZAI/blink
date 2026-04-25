package com.blink.framework.common.data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 渠道配置信息（Nacos 存储）
 * 用于 Nacos 配置中心的渠道信息存储，包含版本控制字段
 *
 * @author binblink
 */
public class ChannelInfoNacosDO implements Serializable {

    @Serial
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
     * 关联用户
     */
    private String relaUserId;

    /**
     * 认证token
     */
    private String accessToken;

    /**
     * 渠道开关 0 开启 1关闭
     */
    private Byte enable;

    /**
     * 加密开关 0 开启 1关闭
     */
    private Byte encryptionSwitch;

    /**
     * 认证方式 -1不认证 0 固定token  1 jwt
     */
    private Byte tokenType;

    /**
     * 权限校验开关 0 开启 1关闭
     */
    private Byte authoritySwitch;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新时间（用于版本控制）
     */
    private LocalDateTime updatedAt;

    // ==================== Getter/Setter ====================

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

    public Byte getTokenType() {
        return tokenType;
    }

    public void setTokenType(Byte tokenType) {
        this.tokenType = tokenType;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ChannelInfoNacosDO{" +
                "channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", appKey='" + appKey + '\'' +
                ", relaUserId='" + relaUserId + '\'' +
                ", enable=" + enable +
                ", encryptionSwitch=" + encryptionSwitch +
                ", tokenType=" + tokenType +
                ", authoritySwitch=" + authoritySwitch +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
