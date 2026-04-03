package com.blink.gateway.service;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.service.model.dto.SysLoginReqDTO;
import com.blink.gateway.service.model.dto.SysLoginRspDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

/**
 * //TODO 未验证 目前使用webclient
 * HttpExchange 远程调用登入认证服务
 * @author binblink
 */
@HttpExchange
public interface AuthService {

    /**
     * 用户登入
     * @param requestDTO
     * @return
     */
    @PostExchange(url = "/system/login",contentType= MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<ResponseDTO<SysLoginRspDTO>>> login(RequestDTO<SysLoginReqDTO> requestDTO);


    /**
     * 用户登出
     * @param requestDTO
     * @return
     */
    @PostExchange(url = "",contentType= MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<ResponseDTO>> logout(RequestDTO requestDTO);
}
