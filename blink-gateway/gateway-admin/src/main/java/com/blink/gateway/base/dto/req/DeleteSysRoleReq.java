package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.AssertTrue;
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
    private Boolean batchDelete;

    /**
     * 联动校验：根据 batchDelete 校验对应的字段
     * - batchDelete=true 时，idList 不能为空
     * - batchDelete=false 时，deleteId 不能为空
     *
     * @return 校验是否通过
     */
    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @SuppressWarnings("unused")
    public boolean isBatchDeleteParamValid() {
        if (Boolean.TRUE.equals(batchDelete)) {
            return idList != null && !idList.isEmpty();
        } else {
            return deleteId != null;
        }
    }
}