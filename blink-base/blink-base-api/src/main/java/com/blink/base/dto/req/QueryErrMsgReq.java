package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取错误消息入参
 * @Author binblink
 */
@Data
public class QueryErrMsgReq {
    /**
     * 错误码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String code;

    /**
     * 语言
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String local;
}
