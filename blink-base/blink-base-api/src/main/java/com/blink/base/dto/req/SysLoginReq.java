package com.blink.base.dto.req;

import com.blink.base.dto.vo.CaptchaVO;
import jakarta.validation.constraints.NotBlank;

public class SysLoginReq {

    @NotBlank
    private String loginName;
    @NotBlank
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
