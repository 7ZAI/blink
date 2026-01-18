package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 用户组关系表 多对多
 * </p>
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_user_group_rela")
public class SysUserGroupRelaDO implements Serializable {

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
