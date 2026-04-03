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
 * DeleteSysDictDataReq 删除字典数据请求参数对象
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Data
public class DeleteSysDictDataReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer deleteId;

    /**
     * 批量删除字典数据Id集合
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
        if (!Boolean.TRUE.equals(batchDelete)) {
            return true;
        }
        return idList != null && !idList.isEmpty();
    }

    /**
     * 校验非批量删除时deleteId不能为空
     */
    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isDeleteIdValid() {
        if (Boolean.TRUE.equals(batchDelete)) {
            return true;
        }
        return deleteId != null;
    }
}
