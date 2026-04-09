package com.blink.gateway.base.dto.req;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.util.List;


/**
 * UpdateSysPermissionReqDTO 更新权限菜单请求参数对象
 *
 * @author binblink
 * @since 2024-01-13
 */
@Data
public class UpdateSysPermissionReq implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 权限id
     */
    @NotNull
    @FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer acId;


    /**
     * 权限名称
     */
    @NotNull
    @FieldConstraint(name = "systemName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String acName;


    /**
     * 权限英文名称
     */
    @FieldConstraint(name = "systemEnName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String acEnName;


    /**
     * 权限标识
     */
    @NotNull
    @FieldConstraint(name = "code30", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String acIdentity;


    /**
     * 权限类型 1接口权限 2数据权限
     */
    @NotNull
    @Range(min = 1, max = 2, message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte acType;


    /**
     * 权限地址
     */
    @FieldConstraint(name = "url", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String url;


    /**
     * 状态 0启动 1禁用 2隐藏
     */
    @FieldConstraint(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Byte status;


    /**
     * 父权限id
     */
    @FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer parentId;


    /**
     * 数据过滤器id
     */
    @FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer dataFilterId;

    /**
     * 关联菜单ID列表（仅接口权限ac_type=1时有效）
     */
    private List<Integer> menuIds;

    /**
     * 校验接口权限时URL必填
     */
    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isApiTypeRequired() {
        if (CommonConstants.PERMISSION_API_TYPE.equals(acType)) {
            return !StrUtil.isBlank(url);
        }
        return true;
    }

}
