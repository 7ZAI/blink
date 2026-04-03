package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户偏好设置
 * </p>
 *
 * @author binblink
 */
@Getter
@Setter
@TableName("sys_user_preference")
public class SysUserPreferenceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 偏好ID
     */
    @TableId(value = "preference_id", type = IdType.AUTO)
    private Integer preferenceId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 主题: light/dark/auto
     */
    @TableField("theme")
    private String theme;

    /**
     * 语言: zh_cn/en_us
     */
    @TableField("language")
    private String language;

    /**
     * 侧边栏收起: 0否 1是
     */
    @TableField("sidebar_collapsed")
    private Boolean sidebarCollapsed;

    /**
     * 字体大小
     */
    @TableField("font_size")
    private Integer fontSize;

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

}
