package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 检查菜单角色分配请求参数
 *
 * @author binblink
 * @since 2026-03-23
 */
@Data
public class CheckMenuRoleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer menuId;

    /**
     * 新权限ID（编辑时用于比较权限是否变更）
     */
    private Integer newPermId;
}