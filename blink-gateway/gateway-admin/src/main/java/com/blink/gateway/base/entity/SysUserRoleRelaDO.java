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
 * 用户角色关系表 多对多
 *
 * @author binblink
 * @since 2023-12-15
 */
@Getter
@Setter
@TableName("sys_user_role_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "用户",
        enName = "User",
        table = "sys_user",
        field = "user_id",
        relationField = "user_id"
    ),
    endpointB = @RelationEndpoint(
        name = "角色",
        enName = "Role",
        table = "sys_role",
        field = "role_id",
        relationField = "role_id"
    )
    // 匹配类型自动推断：
    // 用户视角（关联到角色）→ CURRENT_ROLE, ROLE_LIST
    // 角色视角（关联到用户）→ CURRENT_USER, USER_LIST
)
public class SysUserRoleRelaDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableId("user_id")
    private Integer userId;

    /**
     * 角色id
     */
    @TableField("role_id")
    private Integer roleId;


}
