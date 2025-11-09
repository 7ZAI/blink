package com.blink.gateway.security;

import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.Set;


/**
 * 授权
 * @Author binblink
 * @Date 2025/8/26
 */
public class BlinkAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private ReactiveRedisClient redisClient;

    public BlinkAuthorizationManager(ReactiveRedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .flatMap(auth -> {
                    //未认证
                    if(!auth.isAuthenticated()){
                       return Mono.error(new AccessDeniedException("未认证"));
                    }
                    // 获取当前请求的URL
                    String requestPath = context.getExchange().getRequest().getPath().value();

                    UserInfoRedisDO userInfo = (UserInfoRedisDO) auth.getPrincipal();
                    // 从Redis中获取当前url对应的权限标识
                    return redisClient.get(GatewayConstant.URL_PERMISSION + requestPath)
                            .map(permittedIdentity -> {
                                // 检查当前请求URL是否在用户权限列表中
                                String perIndetity = (String) permittedIdentity;
                                // 为空说明当前url 未被纳入权限控制范围 可以通过
                                if(Strings.isBlank(perIndetity)){
                                    return new AuthorizationDecision(true);
                                }
                                //用户权限 获取用户有权访问的URL列表
                                Set<String> permittedList = userInfo.getPermissions();
                                //超级管理员 有任何权限
                                if(permittedList.contains(GatewayConstant.SUPER_ADMIN_PERMISSION)){
                                    return new AuthorizationDecision(true);
                                }

                                boolean hasPermission = permittedList.contains(perIndetity);
                                return new AuthorizationDecision(hasPermission);
                            })   // 为空说明当前url 未被纳入权限控制范围 可以执行
                            .defaultIfEmpty(new AuthorizationDecision(true));

                }).switchIfEmpty(Mono.just(new AuthorizationDecision(false)));

    }


}