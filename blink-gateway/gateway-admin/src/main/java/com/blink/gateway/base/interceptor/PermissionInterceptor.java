package com.blink.gateway.base.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.constants.RedisKeyConstants;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.gateway.base.entity.SysPermissionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.gateway.base.mapper.SysPermissionMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 全局接口权限拦截器
 * 基于 Sa-Token 实现，通过权限标识校验接口访问权限
 *
 * @author binblink
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String SESSION_USER_INFO_KEY = "userInfo";

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 未登录的请求由 SaInterceptor 处理，这里只处理已登录请求
        if (!StpUtil.isLogin()) {
            return true;
        }

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 只拦截 POST 请求（项目规范：只使用 POST）
        if (!"POST".equalsIgnoreCase(method)) {
            return true;
        }

        // 从 Sa-Token Session 获取用户信息
        UserInfoRedisDO userInfo = (UserInfoRedisDO) StpUtil.getSession().get(SESSION_USER_INFO_KEY);
        if (userInfo == null) {
            log.warn("[Permission] Session 中无用户信息 | path: {}", requestPath);
            BlinkException.throwBusinessException(BaseErrCodeConstant.TOKEN_EXPIRED);
            return false;
        }

        // 获取用户权限集合
        Set<String> userPermissions = userInfo.getPermissions();

        // 超级管理员拥有所有权限，直接放行
        if (CollUtil.isNotEmpty(userPermissions) && userPermissions.contains(CommonConstants.SUPER_ADMIN_PERMISSION)) {
            log.debug("[Permission] 超级管理员放行 | userId: {}, path: {}", userInfo.getUserId(), requestPath);
            return true;
        }

        // 根据请求 URL 获取所需权限标识
        String requiredPermission = getPermissionByUrl(requestPath);

        // 如果该 URL 未配置权限，则放行（未纳入权限控制的接口）
        if (StrUtil.isBlank(requiredPermission)) {
            log.debug("[Permission] 未配置权限的接口放行 | path: {}", requestPath);
            return true;
        }

        // 检查用户是否拥有该权限
        if (CollUtil.isEmpty(userPermissions) || !userPermissions.contains(requiredPermission)) {
            log.warn("[Permission] 权限拒绝 | userId: {}, loginName: {}, path: {}, requiredPerm: {}, userPerms: {}",
                    userInfo.getUserId(), userInfo.getLoginName(), requestPath, requiredPermission, userPermissions);
            BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_DENIED);

            return false;
        }

        log.debug("[Permission] 权限通过 | userId: {}, path: {}, perm: {}", userInfo.getUserId(), requestPath, requiredPermission);
        return true;
    }

    /**
     * 根据URL获取权限标识
     * 优先从缓存获取，缓存未命中则查询数据库
     *
     * @param url 请求URL
     * @return 权限标识，未配置则返回空字符串
     */
    private String getPermissionByUrl(String url) {
        String permission = (String) cacheComponent.getFromCacheOrDB(
                RedisKeyConstants.URL_PERMISSION + url,
                () -> {
                    SysPermissionDO perm = sysPermissionMapper.selectOne(
                            new LambdaQueryWrapper<SysPermissionDO>()
                                    .eq(SysPermissionDO::getUrl, url)
                                    .eq(SysPermissionDO::getAcType, CommonConstants.PERMISSION_API_TYPE)
                    );
                    return perm != null ? perm.getAcIdentity() : "";
                }
        );
        return permission != null ? permission : "";
    }
}