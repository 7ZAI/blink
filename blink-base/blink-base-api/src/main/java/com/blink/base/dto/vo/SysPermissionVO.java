package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author binblink
 */
@Data
public class SysPermissionVO implements Serializable {

    /**
     * 权限id
     */
    private Integer acId;

    /**
     * 权限名称
     */
    private String acName;

    /**
     * 权限英文名称
     */
    private String acEnName;

    /**
     * 权限标识
     */
    private String acIdentity;

    /**
     * 权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限
     */
    private Byte acType;

    /**
     * 权限图标
     */
    private String icon;

    /**
     * 权限地址
     */
    private String url;

    /**
     * 状态 0启动 1禁用 2隐藏
     */
    private Byte status;





}
