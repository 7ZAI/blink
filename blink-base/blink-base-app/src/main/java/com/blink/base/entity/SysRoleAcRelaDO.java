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
 * @since 2023-12-15
 */
@Getter
@Setter
@TableName("sys_role_ac_rela")
public class SysRoleAcRelaDO implements Serializable {

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
