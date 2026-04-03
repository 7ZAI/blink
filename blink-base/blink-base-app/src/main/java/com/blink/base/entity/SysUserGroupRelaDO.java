package com.blink.base.entity;

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
 * 用户组关系表 多对多
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_user_group_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "用户",
        enName = "User",
        table = "sys_user",
        field = "user_id",
        relationField = "user_id"
    ),
    endpointB = @RelationEndpoint(
        name = "部门",
        enName = "Dept",
        table = "sys_group",
        field = "group_id",
        relationField = "group_id"
    )
    // 匹配类型自动推断：
    // 用户视角（关联到部门）→ CURRENT_DEPT, DEPT_LIST
    // 部门视角（关联到用户）→ CURRENT_USER, USER_LIST
)
public class SysUserGroupRelaDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableId("user_id")
    private Integer userId;

    /**
     * 组id
     */
    @TableField("group_id")
    private Integer groupId;


}
