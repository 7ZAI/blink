package com.blink.gateway.security;

import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.gateway.component.GateWayCacheComponent;
import com.blink.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;


/**
 * 授权管理
 *
 * @Author binblink
 */
@Slf4j
public class BlinkAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {


    private final GateWayCacheComponent cacheComponent;

    public BlinkAuthorizationManager(GateWayCacheComponent cacheComponent) {
        this.cacheComponent = cacheComponent;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .flatMap(auth -> {
                    //未认证
                    if (!auth.isAuthenticated()) {
                        return Mono.error(new AccessDeniedException("未认证"));
                    }
                    // 获取当前请求的URL
                    String requestPath = context.getExchange().getRequest().getPath().value();

                    UserInfoRedisDO userInfo = (UserInfoRedisDO) auth.getPrincipal();

                    return cacheComponent.getPermissionsByRequestPath(requestPath)
                            .map(permittedIdentity -> {
                                //用户接口权限集合
                                Set<String> permittedList = userInfo.getPermissions();

                                //超级管理员 有任何权限
                                if (permittedList.contains(GatewayConstant.SUPER_ADMIN_PERMISSION)) {
                                    log.debug("[Authorization] 超级管理员访问 | userId: {}, path: {}", userInfo.getUserId(), requestPath);
                                    return new AuthorizationDecision(true);
                                }

                                boolean hasPermission = permittedList.contains(permittedIdentity);
                                // 授权审计日志
                                if (!hasPermission) {
                                    log.warn("[Authorization] 权限拒绝 | userId: {}, loginName: {}, path: {}, requiredPerm: {}, userPerms: {}",
                                            userInfo.getUserId(), userInfo.getLoginName(), requestPath, permittedIdentity, permittedList);
                                } else {
                                    log.debug("[Authorization] 权限通过 | userId: {}, path: {}, perm: {}", userInfo.getUserId(), requestPath, permittedIdentity);
                                }
                                return new AuthorizationDecision(hasPermission);
                                // 为空说明当前url 未被纳入权限控制范围 可以执行
                            }).defaultIfEmpty(new AuthorizationDecision(true));
                });

    }
}