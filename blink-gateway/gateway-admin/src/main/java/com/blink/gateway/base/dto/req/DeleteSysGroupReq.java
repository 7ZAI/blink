package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 *
 * DeleteSysGroupReqDTO 删除组请求参数对象
 *
 *
 * @author binblink
 * @since 2024-01-04
 */
@Data
public class DeleteSysGroupReq implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 分组id
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
     * 校验批量删除时idList不能为空
     */
    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isIdListValid() {
        // 非批量删除时不需要校验
        if (!Boolean.TRUE.equals(batchDelete)) {
            return true;
        }
        // 批量删除时idList不能为空
        return idList != null && !idList.isEmpty();
    }

    /**
     * 校验非批量删除时deleteId不能为空
     */
    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isDeleteIdValid() {
        // 批量删除时不需要校验
        if (Boolean.TRUE.equals(batchDelete)) {
            return true;
        }
        // 非批量删除时deleteId不能为空
        return deleteId != null;
    }

}
