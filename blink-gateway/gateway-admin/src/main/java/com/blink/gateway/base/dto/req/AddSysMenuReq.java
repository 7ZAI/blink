package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * AddSysMenuReqDTO 新增系统菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Data
public class AddSysMenuReq implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 菜单名称
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String menuName;


    /**
     * 菜单英文名称
     */
    @DataDict(name="systemEnName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String menuEnName;


    /**
     * 菜单类型
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer type;


    /**
     * 菜单图标
     */
    @DataDict(name="url",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String icon;


    /**
     * 菜单地址（type=1目录或type=2页面时必填）
     */
    @DataDict(name="url",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String url;


    /**
     * 排序序号
     */
    @DataDict(name="number",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer orderNumber;


    /**
     * 状态 0显示 1隐藏
     */
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte status;


    /**
     * 父菜单id
     */
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer parentId;


    /**
     * 菜单层级
     */
    @DataDict(name="number",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer menuLevel;


    /**
     * 组件路径
     */
    @DataDict(name="url",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String componentPath;


    /**
     * 是否有子菜单（按钮不算）
     */
    @DataDict(name="boolean",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Boolean hasChildren;

    /**
     * 关联的权限ID（仅type=2页面或type=3按钮时有效）
     */
    private Integer permId;

}
