package com.blink.base.dto.req;

import lombok.Data;

import java.util.List;

/**
 * @Author binblink
 * @Date 2026/2/11
 */
@Data
public class QueryRolePermissionReqDTO {

    /**
     * 角色集合
     */
    private List<Integer> roleIds;

    /**
     * 权限类型
     */
    private List<Integer> acTypes;
}
