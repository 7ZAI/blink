package com.blink.gateway.base.dto.req;


import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
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
public class UpdateSysMenuReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    @NotNull
    @FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer menuId;

    /**
     * 菜单名称
     */
    @NotNull
    @FieldConstraint(name = "systemName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String menuName;


    /**
     * 菜单英文名称
     */
    @FieldConstraint(name = "systemEnName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String menuEnName;


    /**
     * 菜单类型 1-目录 2-页面 3-按钮
     */
    @NotNull
    @FieldConstraint(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte type;


    /**
     * 菜单图标
     */
    @FieldConstraint(name = "url", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String icon;


    /**
     * 菜单地址（type=1目录或type=2页面时必填）
     */
    @FieldConstraint(name = "url", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String url;


    /**
     * 排序序号
     */
    @FieldConstraint(name = "number", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer orderNumber;


    /**
     * 状态 0显示 1隐藏
     */
    @NotNull
    @FieldConstraint(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte status;

    /**
     * 父菜单id
     */
    @FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer parentId;


    /**
     * 菜单层级
     */
    @FieldConstraint(name = "number", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer menuLevel;


    /**
     * 组件路径
     */
    @FieldConstraint(name = "url", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String componentPath;


    /**
     * 是否有子菜单（按钮不算）
     */
    @FieldConstraint(name = "boolean", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Boolean hasChildren;

    /**
     * 关联的权限ID（仅type=2页面或type=3按钮时有效）
     */
    private Integer permId;

}
