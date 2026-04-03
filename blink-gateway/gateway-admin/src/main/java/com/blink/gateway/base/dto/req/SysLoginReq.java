package com.blink.gateway.base.dto.req;

import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.dto.vo.CaptchaVO;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author binblink
 */
public class SysLoginReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = BaseErrCodeConstant.LOGIN_NAME_NOT_NULL)
    private String loginName;

    @NotBlank(message = BaseErrCodeConstant.PASSWORD_NOT_NULL)
    private String password;

    private CaptchaVO captchaVO;

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CaptchaVO getCaptchaVO() {
        return captchaVO;
    }

    public void setCaptchaVO(CaptchaVO captchaVO) {
        this.captchaVO = captchaVO;
    }
}