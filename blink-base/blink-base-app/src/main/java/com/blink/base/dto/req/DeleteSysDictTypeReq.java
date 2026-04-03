package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 删除字典类型表请求参数对象
 *
 * @author blink
 * @since 2025-03-07
 */
@Data
public class DeleteSysDictTypeReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 单个删除的字典主键id
     */
    private Integer deleteId;

    /**
     * 批量删除字典主键id集合
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
