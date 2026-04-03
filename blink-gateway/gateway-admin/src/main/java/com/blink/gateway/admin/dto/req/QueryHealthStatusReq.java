package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询网关健康状态请求参数
 *
 * @author binblink
 */
@Data
public class QueryHealthStatusReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID（可选，为空则查询所有）
     */
    private String instanceId;
}
