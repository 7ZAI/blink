package com.blink.gateway.service.model.dto;

import com.blink.gateway.service.model.vo.CaptchaVO;
import jakarta.validation.constraints.NotBlank;

public class SysLoginReqDTO {

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private CaptchaVO captchaVO;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
