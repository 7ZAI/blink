package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class QueryUserRolesReq implements Serializable {

    /**
     * 登录id
     */
    @NotNull
    @FieldConstraint(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer userId;


}
