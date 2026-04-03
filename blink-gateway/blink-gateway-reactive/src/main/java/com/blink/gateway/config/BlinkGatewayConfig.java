package com.blink.gateway.config;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.filter.*;
import com.blink.gateway.security.filter.CryptFilter;
import com.blink.gateway.security.filter.ReplayAttackPreventionFilter;
import com.blink.gateway.security.filter.RewriteRequestBodyFilter;
import com.blink.gateway.security.filter.SignatureFilter;
import com.blink.gateway.signature.SignatureServiceFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * filter 执行顺序
 * <p>
 * RequestValidateFilter(合法性校验) --> SignatureFilter(签名) --> RequestBodyTransformFliter（转换报文）
 * <p>
 * <p>
 *
 * @author binblink
 */
@Slf4j
@Configuration
public class BlinkGatewayConfig {

    @Resource
    private ReactiveRedisClient redisClient;



    @Resource
    private GateWayCacheComponent cacheComponent;




    /**
     * 全局异常处理
     * 全局异常GlobalExceptionHandlerFilter
     *
     * @return
     */
    @Bean
    @Primary
    public GlobalExceptionHandlerFilter globalExceptionHandlerFilter() {
        return new GlobalExceptionHandlerFilter(cacheComponent);
    }




}
