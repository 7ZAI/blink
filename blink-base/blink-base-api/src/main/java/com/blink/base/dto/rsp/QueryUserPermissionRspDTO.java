package com.blink.base.dto.rsp;

import lombok.Data;

import java.util.Set;

/**
 * 根据userid获取用户权限 响应
 *
 * @Author binblink
 * @Date 2026/2/14
 */
@Data
public class QueryUserPermissionRspDTO {

    /**
     * 权限标识
     */
    private Set<String> permissions;
}
