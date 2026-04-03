package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blink.datasource.annotation.DataScopeEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织部门
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_group")
@DataScopeEntity(name = "组织部门", enName = "SysGroup")
public class SysGroupDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组id
     */
    @TableId(value = "group_id", type = IdType.AUTO)
    private Integer groupId;

    /**
     * 组编号
     */
    @TableField("group_no")
    private String groupNo;

    /**
     * 组名称
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 组英文名称
     */
    @TableField("group_en_name")
    private String groupEnName;

    /**
     * 父组id
     */
    @TableField("group_parent_id")
    private Integer groupParentId;

    /**
     * 层级
     */
    @TableField("group_level")
    private Integer groupLevel;

    /**
     * 是否叶子节点 0否 1是
     */
    @TableField("isLeaf")
    private Integer isLeaf;

    /**
     * 组领导
     */
    @TableField("group_leader")
    private String groupLeader;

    /**
     * 组地址
     */
    @TableField("group_address")
    private String groupAddress;

    /**
     * 组电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 删除标志
     */
    @TableField("delFlag")
    private Boolean delFlag;
}
