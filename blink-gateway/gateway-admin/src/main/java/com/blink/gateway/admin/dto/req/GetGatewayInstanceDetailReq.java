package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 获取网关实例详情请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class GetGatewayInstanceDetailReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;
}