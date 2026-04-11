package com.blink.gateway.admin.service;

import com.blink.gateway.admin.entity.GaRouteDO;

/**
 * 路由异步同步服务接口
 * 用于路由信息变更后异步同步到网关
 * 通过 CacheMsg 的 operator 字段区分操作类型：A(新增)/M(修改)/D(删除)
 *
 * @author binblink
 * @since 2026-04-11
 */
public interface RouteAsyncSyncService {

    /**
     * 异步同步新增路由数据到网关
     * 发送 CacheMsg(operator="A") 实现新增缓存
     *
     * @param routeId     路由ID
     * @param routeDO     路由数据
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncAddRoute(String routeId, GaRouteDO routeDO, Integer operatorUser, String operatorName);

    /**
     * 异步同步修改路由数据到网关
     * 发送 CacheMsg(operator="M") 实现直接更新缓存
     *
     * @param routeId     路由ID
     * @param routeDO     路由数据
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncModifyRoute(String routeId, GaRouteDO routeDO, Integer operatorUser, String operatorName);

    /**
     * 异步同步删除路由数据到网关
     * 发送 CacheMsg(operator="D") 实现删除缓存
     *
     * @param routeId     路由ID
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    void syncDeleteRoute(String routeId, Integer operatorUser, String operatorName);
}