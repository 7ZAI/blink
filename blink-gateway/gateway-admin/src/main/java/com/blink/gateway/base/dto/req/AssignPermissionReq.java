package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色授权请求DTO
 *
 * @author binblink
 */
@Data
public class AssignPermissionReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer roleId;

    /**
     * 权限ID列表
     */
    private List<Integer> permissionIds;
}
