package com.blink.gateway.base.dto.rsp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author binblink
 * @Date 2026/2/18
 */
@Data
public class ChannelTokenRsp {
    // 签发的令牌
    private String token;
    // 过期时间（绝对时间）
    private LocalDateTime expireTime;
    // 有效期（秒），可选
    private Long expiresIn;
}
