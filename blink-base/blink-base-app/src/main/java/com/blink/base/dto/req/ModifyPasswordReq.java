package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码请求参数
 *
 * @author binblink
 */
@Data
public class ModifyPasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 旧密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String oldPassword;

    /**
     * 新密码
     */
    // TODO: 生产环境应添加密码复杂度校验（要求包含大小写字母、数字和特殊字符）
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @Size(min = 6, max = 20, message = BaseErrCodeConstant.PASSWORD_FORMAT_ERR)
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String confirmPassword;
}
