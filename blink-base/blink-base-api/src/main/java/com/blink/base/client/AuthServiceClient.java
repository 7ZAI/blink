package com.blink.base.client;

import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 认证服务
 */
@FeignClient("base-app")
public interface AuthServiceClient {

    @PostMapping("/system/login")
    ResponseDTO<SysLoginRspDTO> login(@Validated @RequestBody RequestDTO<SysLoginReqDTO> requestDTO) throws BlinkException;

}
