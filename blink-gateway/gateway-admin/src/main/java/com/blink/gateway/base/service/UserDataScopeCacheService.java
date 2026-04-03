package com.blink.gateway.base.service;

import com.blink.datasource.data.UserDataScopeInfo;

/**
 * 用户数据范围权限缓存服务
 * 在用户登录时生成并缓存 UserDataScopeInfo
 *
 * @author binblink
 */
public interface UserDataScopeCacheService {

    /**
     * 生成并缓存用户数据范围权限信息
     *
     * @param userId 用户ID
     * @param token  用户token（可选，用于获取用户信息）
     * @return 生成的 UserDataScopeInfo
     */
    UserDataScopeInfo buildAndCache(Integer userId, String token);

    /**
     * 生成并缓存用户数据范围权限信息（兼容旧调用）
     *
     * @param userId 用户ID
     * @return 生成的 UserDataScopeInfo
     */
    default UserDataScopeInfo buildAndCache(Integer userId) {
        return buildAndCache(userId, null);
    }

    /**
     * 从缓存获取用户数据范围权限信息
     *
     * @param userId 用户ID
     * @return UserDataScopeInfo，不存在返回 null
     */
    UserDataScopeInfo getFromCache(Integer userId);

    /**
     * 清除用户数据范围权限缓存
     *
     * @param userId 用户ID
     */
    void clearCache(Integer userId);

    /**
     * 刷新用户数据范围权限缓存
     *
     * @param userId 用户ID
     * @return 刷新后的 UserDataScopeInfo
     */
    UserDataScopeInfo refreshCache(Integer userId);
}