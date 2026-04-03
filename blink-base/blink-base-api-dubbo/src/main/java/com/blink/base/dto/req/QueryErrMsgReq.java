package com.blink.base.dto.req;

import com.blink.base.constans.DubboErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 获取错误消息入参
 * @Author binblink
 */
@Data
public class QueryErrMsgReq implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    @NotBlank(message = DubboErrCodeConstant.PARAMETER_NOT_NULL)
    private String code;

    /**
     * 语言
     */
    @NotBlank(message = DubboErrCodeConstant.PARAMETER_NOT_NULL)
    private String local;
}
