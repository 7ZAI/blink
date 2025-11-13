package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;


/**
 * <p>
 * UpdateBlinkChannelReqDTO 更新对接渠道请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
@Data
public class UpdateBlinkChannelReqDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 渠道ID
     */
    @NotBlank
    private String channelId;


    /**
     * 渠道名
     */
    @NotBlank
    private String channelName;



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
    @NotNull
    private Byte enable;


    /**
     * 加密开关 0 开启 1关闭
     */
    @NotNull
    private Byte encryptionSwitch;


    /**
     * 认证token过期开关 0 开启 1关闭
     */
    @NotNull
    private Byte tokenTimeoutSwitch;


    /**
     * 权限校验开关 0 开启 1关闭
     */
    @NotNull
    private Byte authoritySwitch;


    /**
     * 备注
     */
    private String remark;



}
