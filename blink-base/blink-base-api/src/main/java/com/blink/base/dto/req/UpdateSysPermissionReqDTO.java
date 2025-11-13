package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


/**
 * UpdateSysPermissionReqDTO 更新权限菜单请求参数对象
 *
 * @author binblink
 * @since 2024-01-13
 */
@Data
public class UpdateSysPermissionReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 权限id
     */
    @NotNull
    @DataDict(name = "systemId", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer acId;


    /**
     * 权限名称
     */
    @NotNull
    @DataDict(name = "systemName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String acName;


    /**
     * 权限英文名称
     */
    @DataDict(name = "systemEnName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String acEnName;


    /**
     * 权限标识
     */
    @NotNull
    @DataDict(name = "code30", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String acIdentity;


    /**
     * 权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限
     */
    @NotNull
    @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Byte acType;


    /**
     * 权限图标
     */
    @DataDict(name = "url", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String icon;


    /**
     * 权限地址
     */
    @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String url;


    /**
     * 状态 0启动 1禁用 2隐藏
     */
    @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Byte status;


    /**
     * 父权限id
     */
    @DataDict(name = "systemId", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer parentId;


    /**
     * 数据过滤器id
     */
    @DataDict(name = "systemId", message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer dataFilterId;


}
