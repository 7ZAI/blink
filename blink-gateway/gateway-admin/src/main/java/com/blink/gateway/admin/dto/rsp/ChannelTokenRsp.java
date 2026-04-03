package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 签发渠道Token响应
 *
 * @author binblink
 */
@Data
public class ChannelTokenRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 签发的令牌
     */
    private String token;

    /**
     * 过期时间（绝对时间）
     */
    private LocalDateTime expireTime;

    /**
     * 有效期（秒）
     */
    private Long expiresIn;
}