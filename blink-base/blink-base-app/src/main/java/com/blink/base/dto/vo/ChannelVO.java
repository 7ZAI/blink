package com.blink.base.dto.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author binblink
 * @Date 2025/10/16
 */
@NoArgsConstructor
@Data
public class ChannelVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 3351759846279291793L;

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
     * 渠道私钥
     */
    private String channelPrivatekey;

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

}
