package com.blink.gateway.config;

import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.filter.*;
import com.blink.gateway.security.*;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;


/**
 * 关于 Spring Security 认证机制请看源码
 * @{link org.springframework.security.web.server.authentication.AuthenticationWebFilter }
 *
 * 授权机制请看
 * @{link org.springframework.security.web.server.authorization.AuthorizationWebFilter }
 * @Author binblink
 * @Date 2025/8/20
 */
@Configuration
public class SecurityConfig {

    @Resource
    private ReactiveRedisClient redisClient;

//    @Resource
//    private WebClient webClient;


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                //无状态 AuthenticationWebFilter默认就是无状态的
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                //自定义登入机制
                .authorizeExchange(exchange -> exchange
                        // 登入请求urL 依然会经过认证管理器 到授权管理器才放行
                        .pathMatchers(GatewayConstant.LOGIN_PATH).permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().access(customReactiveAuthorizationManager())
                )
//                .addFilterBefore(requestValidateFilter(),SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(tokenAuthenticationFilter(tokenAuthenticationManager()), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        //  处理认证异常（如未登录）
                        .authenticationEntryPoint(authenticationEntryPoint())
                        //  处理授权异常（如权限不足）
                        .accessDeniedHandler(accessDeniedHandler()))
                .build();
    }


    @Bean
    public BlinkAccessDeniedHandler accessDeniedHandler(){
        return new BlinkAccessDeniedHandler();
    }
    @Bean
    public BlinkAuthenticationEntryPoint authenticationEntryPoint(){
        return new BlinkAuthenticationEntryPoint();
    }

    /**
     * 自定义授权管理
     * @return
     */
    @Bean
    public BlinkAuthorizationManager customReactiveAuthorizationManager(){
        return new BlinkAuthorizationManager(redisClient);
    }



    /**
     * 认证管理器
     * @return TokenAuthenticationManager
     */
    @Bean
    public TokenAuthenticationManager tokenAuthenticationManager(){
        return new TokenAuthenticationManager(redisClient);
    }

    /**
     * 认证过滤器 用原有的认证流程 只是添加自己的扩展
     * 认证过程由 的filter执行
     * @{ling org.springframework.security.web.server.authentication.AuthenticationWebFilter#filter(org.springframework.web.server.ServerWebExchange, org.springframework.web.server.WebFilterChain) }
     * @return tokenAuthenticationFilter
     */
    @Bean
    public AuthenticationWebFilter tokenAuthenticationFilter(TokenAuthenticationManager authenticationManager){

        AuthenticationWebFilter authenticationFilter = new AuthenticationWebFilter(authenticationManager);
        //设置token转换器
        authenticationFilter.setServerAuthenticationConverter(new TokenServerAuthenticationConverter());
        //设置认证成功处理器
        authenticationFilter.setAuthenticationSuccessHandler(new BlinkAuthenticationSuccessHandler(redisClient));
        //设置认证失败处理器
        authenticationFilter.setAuthenticationFailureHandler(new BlinkAuthenticationFailureHandler());
        return authenticationFilter;
    }







}
