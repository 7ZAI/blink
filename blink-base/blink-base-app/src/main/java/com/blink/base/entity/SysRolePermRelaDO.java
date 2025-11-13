package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 角色权限关系表
 * </p>
 *
 * @author binblink
 * @since 2024-01-14
 */
@Getter
@Setter
@TableName("sys_role_perm_rela")
public class SysRolePermRelaDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色id
     */
    @TableId("role_id")
    private Integer roleId;

    /**
     * 权限id
     */
    @TableField("ac_id")
    private Integer acId;
}
