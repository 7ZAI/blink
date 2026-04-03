package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新渠道密钥请求参数
 *
 * @author binblink
 */
@Data
public class RefreshChannelKeyReq implements Serializable {

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
    private String channelName;

    /**
     * 应用key值
     */
    private String appKey;
}
