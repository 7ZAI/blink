package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除渠道请求参数
 *
 * @author binblink
 */
@Data
public class DeleteChannelReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道ID
     */
    @NotBlank(message = "渠道ID不能为空")
    private String channelId;
}
