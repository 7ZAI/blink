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
 * 角色权限关系表
 *
 * @author binblink
 * @since 2024-01-14
 */
@Getter
@Setter
@TableName("sys_role_perm_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "角色",
        enName = "Role",
        table = "sys_role",
        field = "role_id",
        relationField = "role_id"
    ),
    endpointB = @RelationEndpoint(
        name = "权限",
        enName = "Permission",
        table = "sys_permission",
        field = "ac_id",
        relationField = "ac_id"
    )
    // 匹配类型自动推断：
    // 角色视角（关联到权限）→ 默认匹配类型（权限暂无动态匹配）
    // 权限视角（关联到角色）→ CURRENT_ROLE, ROLE_LIST
)
public class SysRolePermRelaDO implements Serializable {

    @Serial
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
