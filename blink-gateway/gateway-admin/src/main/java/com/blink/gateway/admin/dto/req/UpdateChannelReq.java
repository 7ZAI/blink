package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新渠道请求参数
 *
 * @author binblink
 */
@Data
public class UpdateChannelReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道ID
     */
    @NotBlank(message = "渠道ID不能为空")
    private String channelId;

    /**
     * 渠道名
     */
    @NotBlank(message = "渠道名不能为空")
    private String channelName;

    /**
     * 应用秘钥
     */
    private String appSecret;

    /**
     * 关联用户
     */
    @NotBlank(message = "关联用户不能为空")
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
    @NotNull(message = "加密开关不能为空")
    private Byte encryptionSwitch;

    /**
     * 认证方式 0带状态的token  1 jwt
     */
    @NotNull(message = "认证方式不能为空")
    private Byte tokenType;

    /**
     * 权限校验开关 0 开启 1关闭
     */
    @NotNull(message = "权限校验开关不能为空")
    private Byte authoritySwitch;

    /**
     * 备注
     */
    private String remark;
}
