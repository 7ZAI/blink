package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 对接渠道
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
@Getter
@Setter
@TableName("blink_channel")
public class BlinkChannelDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 渠道ID
     */
    @TableId("channel_id")
    private String channelId;

    /**
     * 渠道名
     */
    @TableField("channel_name")
    private String channelName;

    /**
     * 应用key值
     */
    @TableField("app_key")
    private String appKey;

    /**
     * 应用秘钥
     */
    @TableField("app_secret")
    private String appSecret;

    /**
     * 关联用户
     */
    @TableField("rela_user_id")
    private String relaUserId;

    /**
     * 认证token
     */
    @TableField("access_token")
    private String accessToken;

    /**
     * 系统公钥
     */
    @TableField("system_publickey")
    private String systemPublickey;

    /**
     * 系统私钥
     */
    @TableField("system_privatekey")
    private String systemPrivatekey;

    /**
     * 渠道公钥
     */
    @TableField("channel_publickey")
    private String channelPublickey;

    /**
     * 渠道私钥
     */
    @TableField("channel_privatekey")
    private String channelPrivatekey;

    /**
     * 渠道开关 0 开启 1关闭
     */
    @TableField("enable")
    private Byte enable;

    /**
     * 加密开关 0 开启 1关闭
     */
    @TableField("encryption_switch")
    private Byte encryptionSwitch;

    /**
     * 认证token过期开关 0 开启 1关闭
     */
    @TableField("token_timeout_switch")
    private Byte tokenTimeoutSwitch;

    /**
     * 权限校验开关 0 开启 1关闭
     */
    @TableField("authority_switch")
    private Byte authoritySwitch;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
