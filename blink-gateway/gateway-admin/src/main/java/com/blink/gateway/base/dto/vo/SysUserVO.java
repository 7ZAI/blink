package com.blink.gateway.base.dto.vo;

import com.blink.log.annotation.SensitiveField;
import com.blink.log.sensitive.SensitiveType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author binblink
 */
@Data
public class SysUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 昵称
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 头像样式(DiceBear样式)
     */
    private String avatarStyle;

    /**
     * 性别 1男 2女 3不确定
     */
    private Integer sex;

    /**
     * 电话
     */
    @SensitiveField(type = SensitiveType.PHONE)
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 上次登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 锁定状态 0 未锁定 1 管理员锁定 2 输错密码锁定
     */
    private Integer locked;

    /**
     * 超级管理员标志 0否 1是
     */
    private Integer superFlag;

    /**
     * 密码重试次数
     */
    private Integer pswRetry;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;

    /**
     * 备注
     */
    private String remark;


    /**
     * 角色信息
     */
    private List<SysRoleVO> roles;
}
