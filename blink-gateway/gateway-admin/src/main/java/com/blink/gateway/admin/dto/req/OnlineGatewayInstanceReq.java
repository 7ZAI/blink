package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关实例上线请求参数
 *
 * @author binblink
 */
@Data
public class OnlineGatewayInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例 ID
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;
}
