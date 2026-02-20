package com.blink.gateway.config;

import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.component.ChannelSecretCache;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.security.BlinkAccessDeniedHandler;
import com.blink.gateway.security.BlinkAuthenticationEntryPoint;
import com.blink.gateway.security.BlinkAuthenticationFailureHandler;
import com.blink.gateway.security.BlinkAuthorizationManager;
import com.blink.gateway.security.filter.IpFilter;
import com.blink.gateway.security.filter.LogFilter;
import com.blink.gateway.security.filter.RequestValidateFilter;
import com.blink.gateway.security.jwt.JwtAuthenticationConverter;
import com.blink.gateway.security.jwt.JwtAuthenticationManager;
import com.blink.gateway.security.jwt.JwtAuthenticationSuccessHandler;
import com.blink.gateway.security.token.TokenAuthenticationConverter;
import com.blink.gateway.security.token.TokenAuthenticationManager;
import com.blink.gateway.security.token.TokenAuthenticationSuccessHandler;
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


    @Bean("actuatorWebFilterChain")
    @Order(1)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        // 可选：放行健康检查
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 保护所有其他 Actuator 端点
                        .pathMatchers("/actuator/**").authenticated()
                        // 业务接口公开（根据需求调整）
                        .anyExchange().permitAll()
                )
                // 启用 HTTP Basic 认证
                .httpBasic(withDefaults())
                .build();
    }

    // 配置内存用户（也可以从配置文件读取）
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("secret")
                .roles("ACTUATOR")
                .build();
        return new MapReactiveUserDetailsService(user);
    }


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
                //自定义登入机制
                .authorizeExchange(exchange -> exchange
                        // 登入请求urL 依然会经过认证管理器 到授权管理器才放行
                        .pathMatchers(GatewayConstant.LOGIN_PATH).permitAll()
                        .pathMatchers("/channel/auth/**").permitAll()
                        //鉴权
                        .anyExchange().access(new BlinkAuthorizationManager(cacheComponent))
                )
                //日志记录
                .addFilterBefore(new LogFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                //ip过滤
                .addFilterBefore(new IpFilter(config), SecurityWebFiltersOrder.AUTHENTICATION)
                //合法性校验
                .addFilterBefore(new RequestValidateFilter(cacheComponent), SecurityWebFiltersOrder.AUTHENTICATION)
                //渠道认证
                .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                //认证
                .addFilterBefore(tokenAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
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


}

