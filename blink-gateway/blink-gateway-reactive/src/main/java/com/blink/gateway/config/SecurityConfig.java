package com.blink.gateway.config;

import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.security.BlinkAccessDeniedHandler;
import com.blink.gateway.security.BlinkAuthenticationEntryPoint;
import com.blink.gateway.security.BlinkAuthenticationFailureHandler;
import com.blink.gateway.security.BlinkAuthorizationManager;
import com.blink.gateway.security.filter.*;
import com.blink.gateway.security.jwt.JwtAuthenticationConverter;
import com.blink.gateway.security.jwt.JwtAuthenticationManager;
import com.blink.gateway.security.jwt.JwtAuthenticationSuccessHandler;
import com.blink.gateway.security.token.TokenAuthenticationConverter;
import com.blink.gateway.security.token.TokenAuthenticationManager;
import com.blink.gateway.security.token.TokenAuthenticationSuccessHandler;
import com.blink.gateway.signature.SignatureServiceFactory;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 关于 Spring Security 认证机制请看源码
 *
 * @{link org.springframework.security.web.server.authentication.AuthenticationWebFilter }
 * <p>
 * 授权机制请看
 * @{link org.springframework.security.web.server.authorization.AuthorizationWebFilter }
 * @Author binblink
 * @Date 2025/8/20
 */
@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private BlinkGatewayConfigProperties config;

    @Resource
    private GateWayCacheComponent cacheComponent;

    @Resource
    private ChannelSecretCache channelSecretCache;

    @Resource
    private SignatureServiceFactory signatureServiceFactory;


    @Resource
    private ReactiveIdGenerator reactiveIdGenerator;

    /**
     * Actuator 端点安全配置
     * 优先级最高，仅处理 /actuator/** 路径
     */
    @Bean("actuatorWebFilterChain")
    @Order(1)
    public SecurityWebFilterChain actuatorWebFilterChain(ServerHttpSecurity http) {
        return http
                // 仅匹配 actuator 端点
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
                .authorizeExchange(exchanges -> exchanges
                        // 放行健康检查和信息服务
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 保护其他 Actuator 端点，需要认证
                        .anyExchange().authenticated()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 启用 HTTP Basic 认证
                .httpBasic(withDefaults())
                .build();
    }

    /**
     * 配置内存用户（也可以从配置文件读取）
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("123456")
                .roles("ACTUATOR")
                .build();
        return new MapReactiveUserDetailsService(user);
    }

    /**
     * 业务接口安全配置
     * 处理除 actuator 外的所有请求
     */
    @Bean
    @Order(2)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                //无状态 AuthenticationWebFilter默认就是无状态的
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/**"))
                //自定义认证机制
                .authorizeExchange(exchange -> {
                            List<String> urls = config.getIgnoreInterceptUrl();
                            //根据配置文件配置 忽略url 如登入url 渠道认证url
                            if (urls != null && !urls.isEmpty()) {
                                exchange.pathMatchers(urls.toArray(new String[0])).permitAll();
                            }
                            //鉴权
                            exchange.anyExchange().access(new BlinkAuthorizationManager(cacheComponent));
                        }
                )
                //日志记录
                .addFilterBefore(new LogFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                //ip过滤
                .addFilterBefore(new IpFilter(config), SecurityWebFiltersOrder.AUTHENTICATION)
                //合法性校验
                .addFilterBefore(new RequestValidateFilter(cacheComponent, config), SecurityWebFiltersOrder.AUTHENTICATION)
                //渠道认证
                .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                //认证
                .addFilterBefore(tokenAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

                //在鉴定权结束后
                //签名
                .addFilterAfter(signatureFilter(),SecurityWebFiltersOrder.AUTHORIZATION)
                //防重放
                .addFilterAfter(replayAttackPreventionFilter(),SecurityWebFiltersOrder.AUTHORIZATION)
                //加密解密
                .addFilterAfter(cryptFilter(),SecurityWebFiltersOrder.AUTHORIZATION)
                //填装body
                .addFilterAfter(rewriteRequestBodyFilter(),SecurityWebFiltersOrder.AUTHORIZATION)

                .exceptionHandling(exceptionHandling -> exceptionHandling
                        //  处理认证异常（如未登录）
                        .authenticationEntryPoint(new BlinkAuthenticationEntryPoint())
                        //  处理授权异常（如权限不足）
                        .accessDeniedHandler(new BlinkAccessDeniedHandler()))
                .build();
    }


    /**
     * token 认证
     * 认证过滤器 用原有的认证流程 只是添加自己的扩展
     * 认证过程由 的filter执行
     *
     * TODO: 当前只实现了 tokenType == -1 (内部系统用户token认证) 和 tokenType == 1 (JWT渠道认证)
     *       tokenType == 0 (固定渠道token认证) 的认证逻辑尚未实现，需要补充 FixedTokenAuthenticationFilter
     *
     * @return tokenAuthenticationFilter
     * @{ling org.springframework.security.web.server.authentication.AuthenticationWebFilter#filter(org.springframework.web.server.ServerWebExchange, org.springframework.web.server.WebFilterChain) }
     */
    public AuthenticationWebFilter tokenAuthenticationFilter() {

        AuthenticationWebFilter authenticationFilter = new AuthenticationWebFilter(new TokenAuthenticationManager(redisClient));
        //设置token转换器
        authenticationFilter.setServerAuthenticationConverter(new TokenAuthenticationConverter());
        //设置认证成功处理器
        authenticationFilter.setAuthenticationSuccessHandler(new TokenAuthenticationSuccessHandler(redisClient));
        //设置认证失败处理器
        authenticationFilter.setAuthenticationFailureHandler(new BlinkAuthenticationFailureHandler());
        return authenticationFilter;
    }

    public AuthenticationWebFilter jwtAuthenticationFilter() {

        AuthenticationWebFilter authenticationFilter = new AuthenticationWebFilter(new JwtAuthenticationManager(channelSecretCache, cacheComponent));
        //设置token转换器
        authenticationFilter.setServerAuthenticationConverter(new JwtAuthenticationConverter());
        //设置认证成功处理器
        authenticationFilter.setAuthenticationSuccessHandler(new JwtAuthenticationSuccessHandler());
        //设置认证失败处理器
        authenticationFilter.setAuthenticationFailureHandler(new BlinkAuthenticationFailureHandler());
        return authenticationFilter;
    }


    /**
     * 签名  filter
     *
     * @return
     */
    private SignatureFilter signatureFilter() {
        return new SignatureFilter(signatureServiceFactory, cacheComponent,channelSecretCache);
    }

    /**
     * 防重放 filter
     *
     * @return
     */
    private ReplayAttackPreventionFilter replayAttackPreventionFilter() {
        return new ReplayAttackPreventionFilter(redisClient, cacheComponent);
    }

    /**
     * 加密 解密 filter
     *
     * @return
     */
    private CryptFilter cryptFilter() {
        return new CryptFilter(signatureServiceFactory, channelSecretCache);
    }

    /**
     * 元数据组装filter
     *
     * @return
     */
    private RewriteRequestBodyFilter rewriteRequestBodyFilter() {
        return new RewriteRequestBodyFilter(reactiveIdGenerator);
    }





}

