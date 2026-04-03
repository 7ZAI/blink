package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 菜单权限关系表
 * </p>
 *
 * @author binblink
 * @since 2026-02-11
 */
@Getter
@Setter
@TableName("sys_menu_perm_rela")
public class SysMenuPermRelaDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    @TableId("menu_id")
    private Integer menuId;

    /**
     * 权限id
     */
    @TableField("ac_id")
    private Integer acId;
}
