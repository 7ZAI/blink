package com.blink.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.component.GateWayCacheComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.blink.gateway.constant.GatewayConstant.DEFAULT_LANG_CN;

/**
 * @author binblink
 */
@Slf4j
public class GlobalExceptionHandlerFilter implements WebExceptionHandler, Ordered {

    private final GateWayCacheComponent cacheComponent;

    public GlobalExceptionHandlerFilter(GateWayCacheComponent gateWayCacheComponent) {
        this.cacheComponent = gateWayCacheComponent;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        log.error(" error happen! Gateway GlobalExceptionHandler handling", ex);

        ServerHttpResponse response = exchange.getResponse();

        // 关键：检查响应是否已提交，避免二次异常
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        String code = "";
        //未知异常
        if (!(ex instanceof BlinkException blinkException)) {
            code = BlinkErrorCodeEnum.SYS_ERROR.getCode();
        } else {
            code = blinkException.getMessage();
        }
        String local = exchange.getRequest().getHeaders().getFirst(SysConstant.X_BLINK_LOCALE);
        if (StrUtil.isBlank(local)) {
            local = DEFAULT_LANG_CN;
        }
        ResponseDTO<EmptyBody> responseDTO = ResponseDTO.newFailInstance();
        responseDTO.setMsgCode(code);


        return cacheComponent.getErrorMsgInfoFromCache(code, local)
                //返回空抛异常
                .switchIfEmpty(Mono.error(new BlinkException(BlinkErrorCodeEnum.SYS_ERROR.getCode())))
                .flatMap(s -> {
                    responseDTO.setMsgInfo(s);
                    String result = JSON.toJSONString(responseDTO);
                    response.getHeaders().set("Content-Type", "application/json");
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    return response.writeWith(Mono.just(bufferFactory.wrap(result.getBytes(StandardCharsets.UTF_8))));
                }).onErrorResume(throwable ->{
                    //兜底
                    log.error("获取错误信息失败!", throwable);
                    responseDTO.setMsgInfo("系统异常，请稍后重试");
                    responseDTO.setMsgCode(BlinkErrorCodeEnum.SYS_ERROR.getCode());
                    String finResult = JSON.toJSONString(responseDTO);
                    response.getHeaders().set("Content-Type", "application/json");
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    return response.writeWith(Mono.just(bufferFactory.wrap(finResult.getBytes(StandardCharsets.UTF_8))));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE ;
    }

}
