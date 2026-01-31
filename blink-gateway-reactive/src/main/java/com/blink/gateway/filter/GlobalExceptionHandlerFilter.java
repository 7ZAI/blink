package com.blink.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.trafficControl.RateLimitExceededException;
import com.blink.gateway.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.blink.gateway.constant.GatewayConstant.DEFAULT_LANG_CN;

/**
 * 全局异常处理
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

        //兜底 检查响应是否已提交，避免二次异常，
        // 按照系统设计无异常正常返回,有异常发生 则response在这里提交返回 不会有第三个地方于和全局异常处理争抢response的提交
        if (response.isCommitted()) {
            log.error("unexpected response committed happen!", ex);
            return Mono.error(ex);
        }
        String code = "";


         if (ex instanceof RateLimitExceededException rle ) {
             //限流
            code = rle.getMessage();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        } else if (ex instanceof AccessDeniedException) {
             //权限不足 拒绝访问
             code = BlinkErrorCodeEnum.FORBIDDEN_OPERATION.getCode();
             response.setStatusCode(HttpStatus.FORBIDDEN);
         } else if (ex instanceof AuthenticationException) {
             // 认证失败
             code = BlinkErrorCodeEnum.NO_AUTH_ERROR.getCode();
             response.setStatusCode(HttpStatus.UNAUTHORIZED);
         } else if (ex instanceof BlinkException blinkException) {
             //BlinkException
             code = blinkException.getMessage();
             response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
         } else {
             //未知异常
             code = BlinkErrorCodeEnum.SYS_ERROR.getCode();
             response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
         }


        //获取语言环境
        String local = exchange.getRequest().getHeaders().getFirst(SysConstant.X_BLINK_LOCALE);
        if (StrUtil.isBlank(local)) {
            local = DEFAULT_LANG_CN;
        }

        ResponseDTO<EmptyBody> responseDTO = ResponseDTO.newFailInstance();
        responseDTO.setMsgCode(code);

        //设置contentType
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBufferFactory bufferFactory = response.bufferFactory();
                //缓存获取 友好的错误提示语
        return cacheComponent.getErrorMsgInfoFromCache(code, local)
                //返回空抛异常
                .switchIfEmpty(Mono.error(new BlinkException(BlinkErrorCodeEnum.SYS_ERROR.getCode())))
                .flatMap(s -> {
                    responseDTO.setMsgInfo(s);
                    String result = JacksonUtil.toJson(responseDTO);
                    return response.writeWith(Mono.just(bufferFactory.wrap(result.getBytes(StandardCharsets.UTF_8))));
                }).onErrorResume(throwable -> {
                    //兜底
                    log.error("获取错误信息失败!", throwable);
                    responseDTO.setMsgInfo("系统异常，请稍后重试");
                    responseDTO.setMsgCode(BlinkErrorCodeEnum.SYS_ERROR.getCode());
                    String finResult = JacksonUtil.toJson(responseDTO);
                    return response.writeWith(Mono.just(bufferFactory.wrap(finResult.getBytes(StandardCharsets.UTF_8))));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
