package com.blink.base.dto.req;


import com.blink.base.dto.constant.BaseAppConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 * UpdateSysMenuReqDTO 更新系统菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Data
public class UpdateSysMenuReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    @NotNull
    @DataDict(name = "systemId", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer menuId;

    /**
     * 菜单名称
     */
    @NotNull
    @DataDict(name = "systemName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String menuName;


    /**
     * 菜单英文名称
     */
    @DataDict(name = "systemEnName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String menuEnName;


    /**
     * 菜单类型
     */
    @NotNull
    @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer type;


    /**
     * 菜单图标
     */
    @DataDict(name = "url", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String icon;


    /**
     * 菜单地址
     */
    @NotNull
    @DataDict(name = "url", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String url;


    /**
     * 排序序号
     */
    @DataDict(name = "number", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer orderNumber;


    /**
     * 状态 0显示 1隐藏
     */
    @NotNull
    @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Byte status;

    /**
     * 父菜单id
     */
    @DataDict(name = "systemId", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer parentId;


    /**
     * 菜单层级
     */
    @DataDict(name = "number", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer menuLevel;


    /**
     * 组件路径
     */
    @DataDict(name = "url", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String componentPath;


    /**
     * 是否有子菜单（按钮不算）
     */
    @DataDict(name = "boolean", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Boolean hasChildren;


}
