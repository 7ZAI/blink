package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author binblink
 * @Date 2026/2/18
 */
@Data
public class IssueChannelTokenReq {

    @NotBlank(message = "渠道ID不能为空")
    private String appKey;

    @NotBlank(message = "渠道ID不能为空")
    private String appSecret;

    private String ip;

    private Long expiresInSeconds;

    private String scope;
}
