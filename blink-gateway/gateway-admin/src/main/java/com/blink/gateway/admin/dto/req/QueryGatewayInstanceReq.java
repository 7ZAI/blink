package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询网关实例列表请求参数
 *
 * @author binblink
 */
@Data
public class QueryGatewayInstanceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（可选，用于过滤）
     */
    private String instanceId;
}
