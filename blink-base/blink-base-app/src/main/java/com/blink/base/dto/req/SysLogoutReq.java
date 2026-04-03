package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author binblink
 * @Date 2025/8/28
 */
@Data
public class SysLogoutReq {

    @NotBlank
    @DataDict(name="sysToken",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String token;

    @NotBlank
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String userId;
}
