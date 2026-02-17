package com.blink.gateway.security.token;

import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.constant.GatewayConstant;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static com.blink.gateway.constant.RedisConstans.USER_TOKEN;

/**
 * 自定义认证器
 * 可以定制多个认证器实现不同的认证方式
 *
 * @Author binblink
 * @Date 2025/8/27
 */
public class TokenAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveRedisClient redisClient;

    public TokenAuthenticationManager(ReactiveRedisClient redisClient) {
        this.redisClient = redisClient;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        //authentication 为未认证的UsernamePasswordAuthenticationToken
//        String loginName = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        //直接返回未认证的token 错误由后面的filter抛出 登入uri会经过
        if (Strings.isBlank(token)) {
            return Mono.just(authentication);
        }

        String tokenKey = USER_TOKEN + token;

        // 根据token 拿到用户信息
        return redisClient.get(tokenKey)
                .switchIfEmpty(Mono.error(new CredentialsExpiredException("token过期")))
                .flatMap(userInfoObj -> {
                    UserInfoRedisDO userInfo = JacksonUtil.convert(userInfoObj, UserInfoRedisDO.class);
                    //拿到userId
//                    String userIdFromRedis = String.valueOf(userInfo.getLoginName());
                    //参数不合法  request header loginName参数 和redis存的loginName不一致
//                    if(!loginName.equals(userIdFromRedis)){
//                       return Mono.error(new UsernameNotFoundException("认证错误,参数非法！"));
//                    }
                    //认证Authentication
                    Authentication authenticated = UsernamePasswordAuthenticationToken
                            .authenticated(userInfo, token, userInfo.getPermissions().stream().map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList()));
                    //返回已认证的authentication
                    return Mono.just(authenticated);
                });
    }
}
