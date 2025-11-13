package com.blink.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 权限菜单
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
@Getter
@Setter
@TableName("sys_permission")
public class SysPermissionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限id
     */
    @TableId(value = "ac_id", type = IdType.AUTO)
    private Integer acId;

    /**
     * 权限名称
     */
    @TableField("ac_name")
    private String acName;

    /**
     * 权限英文名称
     */
    @TableField("ac_en_name")
    private String acEnName;

    /**
     * 权限标识
     */
    @TableField("ac_identity")
    private String acIdentity;

    /**
     * 权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限
     */
    @TableField("ac_type")
    private Byte acType;

    /**
     * 权限图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 权限地址
     */
    @TableField("url")
    private String url;

    /**
     * 状态 0启动 1禁用 2隐藏
     */
    @TableField("status")
    private Byte status;

    /**
     * 父权限id
     */
    @TableField("parent_id")
    private Integer parentId;

    /**
     * 数据过滤器id
     */
    @TableField("data_filter_id")
    private Integer dataFilterId;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
