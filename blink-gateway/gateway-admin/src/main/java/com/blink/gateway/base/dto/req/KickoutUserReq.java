package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;

public class KickoutUserReq {

    @NotBlank(message = "token不能为空")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
