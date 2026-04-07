package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取渠道密钥请求参数
 *
 * @author binblink
 */
@Data
public class GetChannelSecretReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道ID
     */
    @NotBlank(message = "渠道ID不能为空")
    private String channelId;

    /**
     * 密钥字段类型
     * appSecret, systemPublickey, systemPrivatekey, channelPublickey, channelPrivatekey
     */
    @NotBlank(message = "密钥字段类型不能为空")
    private String secretField;
}