package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
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
    private boolean isBatchDelete;


}
