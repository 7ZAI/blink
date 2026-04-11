package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.CacheSyncFailureService;
import com.blink.gateway.admin.service.RouteAsyncSyncService;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;

/**
 * 路由异步同步服务实现类
 * 使用 IO 线程池异步执行路由数据同步到网关
 *
 * @author binblink
 * @since 2026-04-11
 */
@Service
@Slf4j
public class RouteAsyncSyncServiceImpl implements RouteAsyncSyncService {

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Resource
    private CacheSyncFailureService cacheSyncFailureService;

    @Resource
    private RedisClient redisClient;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 3;

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncAddRoute(String routeId, GaRouteDO routeDO, Integer operatorUser, String operatorName) {
        syncRouteWithRetry(routeId, routeDO, RouteConstant.OPERATION_ADD, operatorUser, operatorName);
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncModifyRoute(String routeId, GaRouteDO routeDO, Integer operatorUser, String operatorName) {
        syncRouteWithRetry(routeId, routeDO, RouteConstant.OPERATION_MODIFY, operatorUser, operatorName);
    }

    @Async("ioIntensiveThreadPool")
    @Override
    public void syncDeleteRoute(String routeId, Integer operatorUser, String operatorName) {
        syncRouteWithRetry(routeId, null, RouteConstant.OPERATION_DELETE, operatorUser, operatorName);
    }

    /**
     * 带重试机制的同步方法
     *
     * @param routeId     路由ID
     * @param routeDO     路由数据（删除时为 null）
     * @param operation   操作类型：A/M/D
     * @param operatorUser 操作人用户ID
     * @param operatorName 操作人用户名
     */
    private void syncRouteWithRetry(String routeId, GaRouteDO routeDO, String operation,
                                     Integer operatorUser, String operatorName) {
        // 构建 Redis 路由存储 key
        String routesGroup = routeDO != null ? routeDO.getRoutesGroup() : RouteConstant.DEFAULT_ROUTES_GROUP;
        if (StrUtil.isBlank(routesGroup)) {
            routesGroup = RouteConstant.DEFAULT_ROUTES_GROUP;
        }
        String redisKey = GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup;

        // 根据 Redis 模式同步
        if (RouteConstant.STORAGE_MODE_REDIS.equals(routeDO != null ? routeDO.getStorageMode() : RouteConstant.STORAGE_MODE_REDIS)) {
            syncToRedis(redisKey, routeId, routeDO, operation, operatorUser, operatorName);
        }

        // 发送路由同步消息到 Stream
        RouteSyncMsg syncMsg = new RouteSyncMsg();
        syncMsg.setStorageMode(RouteConstant.STORAGE_MODE_REDIS);
        syncMsg.setDynamicRouteKey(redisKey);
        syncMsg.setPushMode(RouteConstant.OPERATION_ADD.equals(operation) ? "broadcast" : "broadcast");

        // 重试发送 Stream 消息
        for (int retryCount = 0; retryCount < MAX_RETRY_TIMES; retryCount++) {
            try {
                gateWayStreamMessageProducer.routesOnChangeWithTarget(syncMsg);
                log.info("[RouteAsyncSync] 路由同步成功 | routeId: {}, operation: {}, operatorUser: {}",
                        routeId, operation, operatorUser);
                return;
            } catch (Exception e) {
                log.warn("[RouteAsyncSync] 同步失败，第{}次重试 | routeId: {}, operation: {}, error: {}",
                        retryCount + 1, routeId, operation, e.getMessage());

                if (retryCount == MAX_RETRY_TIMES - 1) {
                    log.error("[RouteAsyncSync] 同步失败，已达最大重试次数 | routeId: {}, operation: {}", routeId, operation);
                } else {
                    try {
                        Thread.sleep(100 * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[RouteAsyncSync] 线程被中断");
                        return;
                    }
                }
            }
        }
    }

    /**
     * 同步路由到 Redis
     *
     * @param redisKey     Redis Hash key
     * @param routeId      路由ID
     * @param routeDO      路由数据
     * @param operation    操作类型
     * @param operatorUser 操作人
     * @param operatorName 操作人名称
     */
    private void syncToRedis(String redisKey, String routeId, GaRouteDO routeDO,
                              String operation, Integer operatorUser, String operatorName) {
        try {
            if (RouteConstant.OPERATION_DELETE.equals(operation)) {
                // 删除操作：从 Redis Hash 中移除
                redisClient.hDeleteFields(redisKey, routeId);
                log.info("[RouteAsyncSync] Redis 删除路由成功 | key: {}, routeId: {}", redisKey, routeId);
            } else {
                // 新增/修改操作：更新 Redis Hash
                String routeJson = JacksonUtil.toJson(routeDO);
                redisClient.hPutField(redisKey, routeId, routeJson);
                log.info("[RouteAsyncSync] Redis 更新路由成功 | key: {}, routeId: {}, operation: {}",
                        redisKey, routeId, operation);
            }
        } catch (Exception e) {
            log.error("[RouteAsyncSync] Redis 同步失败 | key: {}, routeId: {}, operation: {}, error: {}",
                    redisKey, routeId, operation, e.getMessage(), e);
        }
    }
}