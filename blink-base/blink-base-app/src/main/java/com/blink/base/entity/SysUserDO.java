package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户
 * </p>
 *
 * @author binblink
 * @since 2023-12-15
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    /**
     * 登录名
     */
    @TableField("login_name")
    private String loginName;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("username")
    private String username;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 性别 1男 2女 3不确定
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 上次登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 锁定状态 0 未锁定 1 管理员锁定 2 输错密码锁定
     */
    @TableField("locked")
    private Integer locked;

    /**
     * 加密盐值
     */
    @TableField("salt")
    private String salt;

    /**
     * 密码重试次数
     */
    @TableField("psw_retry")
    private Integer pswRetry;

    /**
     * 超级管理员标志 0否 1是
     */
    @TableField("superFlag")
    private Integer superFlag;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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
     * 锁定时间
     */
    @TableField("lock_time")
    private LocalDateTime lockTime;


}
