package com.blink.gateway.base.dto.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 根据userid获取用户权限 响应
 *
 * @Author binblink
 * @Date 2026/2/14
 */
@Data
public class QueryUserPermissionRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限标识
     */
    private Set<String> permissions;
}
