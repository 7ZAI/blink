package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 2368841350409243359L;
    /**
     * 角色id
     */
    private Integer roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色英文名称
     */
    private String roleEnName;

    /**
     * 角色状态
     */
    private Byte status;

    /**
     * 角色代码
     */
    private String roleCode;


    /**
     * 角色类型
     */
    private Byte roleType;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
