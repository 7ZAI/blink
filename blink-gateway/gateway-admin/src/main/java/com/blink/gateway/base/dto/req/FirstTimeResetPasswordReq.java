package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首次登录重置密码请求参数
 *
 * @author binblink
 */
@Data
public class FirstTimeResetPasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @Size(min = 6, max = 20, message = BaseErrCodeConstant.PASSWORD_FORMAT_ERR)
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String confirmPassword;
}