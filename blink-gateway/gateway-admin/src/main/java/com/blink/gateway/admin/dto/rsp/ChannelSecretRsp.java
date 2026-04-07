package com.blink.gateway.admin.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 渠道密钥信息响应
 *
 * @author binblink
 */
@Data
public class ChannelSecretRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 密钥值
     */
    private String secretValue;
}