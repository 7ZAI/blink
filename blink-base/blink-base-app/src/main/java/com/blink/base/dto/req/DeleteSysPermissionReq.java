package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * DeleteSysPermissionReqDTO删除权限菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
@Data
public class DeleteSysPermissionReq implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 权限id
     */
    private Integer deleteId;

    /**
     * 批量删除用户Id集合
     */
    private List<Integer> idList;


    /**
     * 是否批量删除标志
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private boolean isBatchDelete;


}
