package com.blink.gateway.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统菜单
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Getter
@Setter
@TableName("sys_menu")
public class SysMenuDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Integer menuId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 菜单英文名称
     */
    @TableField("menu_en_name")
    private String menuEnName;

    /**
     * 菜单类型
     */
    @TableField("type")
    private Byte type;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 菜单地址
     */
    @TableField("url")
    private String url;

    /**
     * 排序序号
     */
    @TableField("order_number")
    private Integer orderNumber;

    /**
     * 状态 0显示 1隐藏
     */
    @TableField("status")
    private Byte status;

    /**
     * 父菜单id
     */
    @TableField("parent_id")
    private Integer parentId;

    /**
     * 菜单层级
     */
    @TableField("menu_level")
    private Integer menuLevel;

    /**
     * 组件路径
     */
    @TableField("component_path")
    private String componentPath;

    /**
     * 关联的权限ID
     */
    @TableField(value = "perm_id", updateStrategy = FieldStrategy.ALWAYS)
    private Integer permId;

    /**
     * 是否有子菜单（按钮不算）
     */
    @TableField("hasChildren")
    private Boolean hasChildren;

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

    /**
     * 删除标志
     */
    @TableField("delFlag")
    private Boolean delFlag;
}
