package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 签发渠道Token请求参数
 *
 * @author binblink
 */
@Data
public class IssueChannelTokenReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道标识
     */
    @NotBlank(message = "渠道appKey不能为空")
    private String appKey;

    /**
     * 渠道密钥
     */
    @NotBlank(message = "渠道appSecret不能为空")
    private String appSecret;

    /**
     * 客户端IP地址
     */
    private String ip;

    /**
     * Token有效期（秒）
     */
    private Long expiresInSeconds;

    /**
     * 权限范围
     */
    private String scope;
}