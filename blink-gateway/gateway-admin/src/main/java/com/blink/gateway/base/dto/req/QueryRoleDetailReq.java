package com.blink.gateway.base.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询角色详情请求DTO
 *
 * @author binblink
 */
@Data
public class QueryRoleDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Integer roleId;
}
