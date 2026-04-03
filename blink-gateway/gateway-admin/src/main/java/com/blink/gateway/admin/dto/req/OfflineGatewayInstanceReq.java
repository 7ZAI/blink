package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关实例下线请求参数
 *
 * @author binblink
 */
@Data
public class OfflineGatewayInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 下线原因（可选）
     */
    private String reason;
}
