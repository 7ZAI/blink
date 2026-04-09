package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * DeleteSysUserReqDTO 删除用户请求参数 支持批量删除
 * @author binblink
 */
@Data
public class DeleteSysUserReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -1062964850752777049L;

    /**
     * 删除的用户Id
     */
    @FieldConstraint(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer userId;

    /**
     * 批量删除用户Id集合
     */
    private List<Integer> userIdList;

    /**
     * 是否批量删除标志
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Boolean batchDelete;

    @AssertTrue(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    public boolean isValidParam() {
        if(Boolean.TRUE.equals(batchDelete)){
            return userIdList != null && !userIdList.isEmpty();
        }else{
            return userId != null;
        }
    }
}
