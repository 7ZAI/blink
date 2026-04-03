package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.blink.datasource.annotation.DataScopeRelation;
import com.blink.datasource.annotation.RelationEndpoint;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色关联菜单表
 *
 * @author binblink
 * @since 2026-02-14
 */
@Getter
@Setter
@TableName("sys_role_menu_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "角色",
        enName = "Role",
        table = "sys_role",
        field = "role_id",
        relationField = "role_id"
    ),
    endpointB = @RelationEndpoint(
        name = "菜单",
        enName = "Menu",
        table = "sys_menu",
        field = "menu_id",
        relationField = "menu_id"
    )
    // 匹配类型自动推断：
    // 角色视角（关联到菜单）→ 默认匹配类型（菜单暂无动态匹配）
    // 菜单视角（关联到角色）→ CURRENT_ROLE, ROLE_LIST
)
public class SysRoleMenuRelaDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色id
     */
    @TableId("role_id")
    private Integer roleId;

    /**
     * 菜单id
     */
    @TableField("menu_id")
    private Integer menuId;
}
