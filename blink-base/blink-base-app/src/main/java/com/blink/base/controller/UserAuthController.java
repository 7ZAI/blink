package com.blink.base.controller;

import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.req.SysLogoutReqDTO;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.base.service.UserAuthService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 登入 API
 *
 * @author binblink
 * @module blink
 * @since 2024-01-04
 */
@RestController
@RequestMapping("/auth")
public class UserAuthController {

    @Resource
    private UserAuthService userAuthService;

    /**
     * 登入
     *
     * @param requestDTO
     * @return SysLoginRspDTO
     * @throws BlinkException
     */
    @RequestMapping("/login")
    public ResponseDTO<SysLoginRspDTO> login(@Validated @RequestBody RequestDTO<SysLoginReqDTO> requestDTO) throws BlinkException {
        return ResponseDTO.newSuccessInstance(userAuthService.login(requestDTO.getBody()));
    }

    /**
     * 登出
     * @param requestDTO
     * @return
     * @throws BlinkException
     */
    @RequestMapping("/logout")
    public ResponseDTO<EmptyBody> logout(@Validated @RequestBody RequestDTO<SysLogoutReqDTO> requestDTO) throws BlinkException {
        userAuthService.logout(requestDTO.getBody());
        return ResponseDTO.newSuccessInstance() ;
    }


}
