package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * DeleteSysRoleReqDTO删除系统角色请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
@Data
public class DeleteSysRoleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色id
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
