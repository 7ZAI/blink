package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blink.datasource.annotation.DataScopeEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_role")
@DataScopeEntity(name = "系统角色", enName = "SysRole")
public class SysRoleDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色id
     */
    @TableId(value = "role_id", type = IdType.AUTO)
    private Integer roleId;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色英文名称
     */
    @TableField("role_en_name")
    private String roleEnName;

    /**
     * 角色状态
     */
    @TableField("status")
    private Byte status;

    /**
     * 角色代码
     */
    @TableField("role_code")
    private String roleCode;


    /**
     * 角色类型
     */
    @TableField("role_type")
    private Byte roleType;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志
     */
    @TableField("delFlag")
    private Boolean delFlag;
}
