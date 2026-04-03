package com.blink.gateway.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author binblink
 */
@Data
public class SysMenuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    private Integer menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单英文名称
     */
    private String menuEnName;

    /**
     * 菜单类型
     */
    private Byte type;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单地址
     */
    private String url;

    /**
     * 排序序号
     */
    private Integer orderNumber;

    /**
     * 状态 0显示 1隐藏
     */
    private Byte status;

    /**
     * 父菜单id
     */
    private Integer parentId;

    /**
     * 菜单层级
     */
    private Integer menuLevel;

    /**
     * 组件路径
     */
    private String componentPath;

    /**
     * 关联的权限ID
     */
    private Integer permId;

    /**
     * 关联的权限标识（如 sysUser:add）
     */
    private String permIdentity;

    /**
     * 关联的权限名称
     */
    private String permName;

    /**
     * 是否有子菜单（按钮不算）
     */
    private Boolean hasChildren;

    /**
     * 子菜单列表
     */
    private List<SysMenuVO> children;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
