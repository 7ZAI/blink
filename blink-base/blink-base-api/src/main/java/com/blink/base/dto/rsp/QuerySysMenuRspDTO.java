package com.blink.base.dto.rsp;

import lombok.Data;
import java.io.Serializable;
import com.blink.framework.common.data.PageDTO;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysMenuRspDTO 新增系统菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Data
public class QuerySysMenuRspDTO extends PageDTO implements Serializable {

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
     * 是否有子菜单（按钮不算）
     */
    private Boolean hasChildren;


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


    /**
     * 删除标志
     */
    private Boolean delFlag;


}
