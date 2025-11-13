package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 * UpdateSysRoleReqDTO 更新系统角色请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
@Data
public class UpdateSysRoleReqDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 角色id
     */
    @NotNull
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer roleId;


    /**
     * 角色名称
     */
    @NotNull
    @DataDict(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleName;


    /**
     * 角色英文名称
     */
    @DataDict(name="systemEnName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleEnName;


    /**
     * 角色状态
     */
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte status;


    /**
     * 角色代码
     */
    @DataDict(name="code30",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleCode;


    /**
     * 角色类型
     */
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte roleType;



}
