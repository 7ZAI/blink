package com.blink.gateway.config;


import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.filter.*;
import com.blink.gateway.signature.SignatureServiceFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * filter 执行顺序
 * <p>
 * RequestValidateFilter(合法性校验) --> SignatureFilter(签名) --> RequestBodyTransformFliter（转换报文）
 * <p>
 * <p>
 * @author binblink
 */
@Slf4j
@Configuration
public class BlinkGatewayConfig {

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private ReactiveIdGenerator reactiveIdGenerator;

    @Resource
    private GateWayCacheComponent cacheComponent;

    @Resource
    private SignatureServiceFactory signatureServiceFactory;


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

    /**
     * 日志记录过滤器 执行filter执行顺序 正是按照当前filter的从上到下的书写顺序
     * @return
     */
    @Bean
    public GatewayLogFilter gatewayLogFilter() {
        return new GatewayLogFilter();
    }

    /**
     * ip 过滤
     * @param config 属性配置
     * @return
     */
    @Bean
    public IpFilter ipFilter(BlinkGatewayConfigProperties config) {
        return new IpFilter(config);
    }

    /**
     * 合法性校验filter
     * @return
     */
    @Bean
    public RequestValidateFilter requestValidateFilter() {
        return new RequestValidateFilter(cacheComponent);
    }

    /**
     * 签名 防重放 filter
     * @return
     */
    @Bean
    public SignatureFilter signatureFilter() {
        return new SignatureFilter(redisClient, signatureServiceFactory, cacheComponent);
    }

    /**
     * 加密 解密 filter
     * @return
     */
    @Bean
    public CryptFilter cryptFilter() {
        return new CryptFilter(signatureServiceFactory);
    }

    /**
     * 元数据组装filter
     * @return
     */
    @Bean
    public RewriteRequestBodyFilter rewriteRequestBodyFilter() {
        return new RewriteRequestBodyFilter(reactiveIdGenerator);
    }

}
