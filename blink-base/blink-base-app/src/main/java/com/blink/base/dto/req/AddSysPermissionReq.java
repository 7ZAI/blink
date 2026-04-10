package com.blink.base.dto.req;

import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.CommonConstants;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * AddSysPermissionReqDTO 新增权限菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
@Data
public class AddSysPermissionReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 权限名称
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
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
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @FieldConstraint(name = "code30", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String acIdentity;


    /**
     * 权限类型 2数据权限  1接口权限
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @Range(min=1, max=2, message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
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

    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isApiTypeRequired() {

        if(CommonConstants.PERMISSION_API_TYPE.equals(acType)){
            return !StrUtil.isBlank(url);
        }
        return true;

    }

    /**
     * 关联菜单ID列表（仅接口权限ac_type=1时有效）
     */
    private List<Integer> menuIds;

}
