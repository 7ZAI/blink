package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员重置用户密码请求参数
 *
 * @author binblink
 */
@Data
public class ResetPasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer userId;

    /**
     * 新密码
     */
    // TODO: 生产环境应添加密码复杂度校验（要求包含大小写字母、数字和特殊字符）
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @Size(min = 6, max = 20, message = BaseErrCodeConstant.PASSWORD_FORMAT_ERR)
    private String newPassword;
}
