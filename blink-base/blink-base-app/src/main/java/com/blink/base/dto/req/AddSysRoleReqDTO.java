package com.blink.base.dto.req;


import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * AddSysRoleReqDTO 新增系统角色请求参数对象
 *
 * @author binblink
 * @since 2024-01-03
 */
@Data
public class AddSysRoleReqDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 角色名称
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name = "systemName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleName;


    /**
     * 角色英文名称
     */
    @DataDict(name = "systemEnName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleEnName;


    /**
     * 角色状态
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer status;


    /**
     * 角色代码
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name = "code30", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleCode;


    /**
     * 角色类型
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer roleType;

    /**
     * 角色分配的权限列表
     */
    private List<Integer> permissionIds;


    /**
     * 角色分配的菜单
     */
    private List<Integer> menuIds;


}
