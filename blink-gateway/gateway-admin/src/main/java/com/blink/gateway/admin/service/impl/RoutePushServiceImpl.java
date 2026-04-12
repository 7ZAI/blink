package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.dto.req.FullPushRoutesReq;
import com.blink.gateway.admin.dto.req.PushRoutesReq;
import com.blink.gateway.admin.dto.req.QueryInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.QueryPushLogReq;
import com.blink.gateway.admin.dto.req.RollbackPushReq;
import com.blink.gateway.admin.dto.rsp.QueryInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushLogRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import com.blink.gateway.admin.mapper.GaRouteMapper;
import com.blink.gateway.admin.mapper.GaRoutePushLogMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.service.RoutePushService;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PARAMETER_NOT_NULL;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PUSH_LOG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_NOT_EXIST;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;

/**
 * 路由推送服务实现
 * 管理路由从仓库推送到实例
 *
 * @author binblink
 * @since 2026-04-11
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class RoutePushServiceImpl implements RoutePushService {

    @Resource
    private GaRouteMapper gaRouteMapper;

    @Resource
    private GaRoutePushLogMapper gaRoutePushLogMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private GatewayInstanceService gatewayInstanceService;

    @Override
    public ResponseDTO<EmptyBody> pushRoutes(PushRoutesReq req) {
        // 参数校验
        if (CollUtil.isEmpty(req.getRouteIds())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(req.getStorageMode())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(req.getPushMode())) {
            req.setPushMode(RouteConstant.PUSH_MODE_BROADCAST);
        }

        // 查询要推送的路由
        List<GaRouteDO> routes = gaRouteMapper.selectBatchIds(req.getRouteIds());
        if (CollUtil.isEmpty(routes)) {
            BlinkException.throwBusinessException(ROUTE_NOT_EXIST);
        }

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        // 构建推送记录
        GaRoutePushLogDO pushLog = new GaRoutePushLogDO();
        pushLog.setStorageMode(req.getStorageMode());
        pushLog.setRouteIds(JacksonUtil.toJson(req.getRouteIds()));
        pushLog.setRouteSnapshot(routes);
        pushLog.setPushMode(req.getPushMode());
        pushLog.setOperatorId(operatorUser);
        pushLog.setOperatorName(operatorName);
        pushLog.setRemark(req.getRemark());

        // 设置存储方式相关参数
        if (RouteConstant.STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            String routesGroup = StrUtil.isBlank(req.getRoutesGroup())
                ? RouteConstant.DEFAULT_ROUTES_GROUP : req.getRoutesGroup();
            pushLog.setRoutesGroup(routesGroup);

            // 推送到 Redis
            String redisKey = GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup;
            for (GaRouteDO route : routes) {
                String routeJson = JacksonUtil.toJson(route);
                redisClient.hPutField(redisKey, route.getRouteId(), routeJson);
            }
        } else if (RouteConstant.STORAGE_MODE_NACOS.equals(req.getStorageMode())) {
            pushLog.setNacosDataId(req.getNacosDataId());
            pushLog.setNacosGroup(req.getNacosGroup());
            // Nacos 推送由 NacosRouteService 处理
        }

        // 设置目标实例信息
        List<String> targetInstanceIds = new ArrayList<>();
        if (RouteConstant.PUSH_MODE_SPECIFIED.equals(req.getPushMode())
            && CollUtil.isNotEmpty(req.getTargetInstanceIds())) {
            targetInstanceIds = req.getTargetInstanceIds();
            pushLog.setTargetInstanceIds(JacksonUtil.toJson(targetInstanceIds));
            pushLog.setInstanceCount(targetInstanceIds.size());
        } else {
            // 广播模式：获取所有在线实例
            ResponseDTO<GatewayInstanceListRsp> instancesRsp = gatewayInstanceService.getGatewayInstances();
            if (instancesRsp.getBody() != null && instancesRsp.getBody().getInstances() != null) {
                targetInstanceIds = instancesRsp.getBody().getInstances().stream()
                    .filter(inst -> inst.getStatus().equals(INSTANCE_STATUS_ONLINE))
                    .map(GatewayInstanceVO::getInstanceId)
                    .toList();
                pushLog.setInstanceCount(targetInstanceIds.size());
            }
        }

        // 发送 Stream 消息通知网关刷新
        RouteSyncMsg syncMsg = new RouteSyncMsg();
        syncMsg.setStorageMode(req.getStorageMode());
        syncMsg.setPushMode(req.getPushMode());
        syncMsg.setTargetInstanceIds(targetInstanceIds);

        if (RouteConstant.STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            syncMsg.setDynamicRouteKey(GATEWAY_DYNAMIC_ROUTES + ":" + pushLog.getRoutesGroup());
        } else {
            syncMsg.setDataId(req.getNacosDataId());
            syncMsg.setGroup(req.getNacosGroup());
        }

        try {
            messageProducer.routesOnChangeWithTarget(syncMsg);
            pushLog.setPushResult(RouteConstant.PUSH_RESULT_SUCCESS);
            pushLog.setSuccessCount(pushLog.getInstanceCount());
            pushLog.setConfirmStatus(RouteConstant.CONFIRM_STATUS_PENDING);

            // 更新路由推送状态为已推送
            updateRoutePushStatus(req.getRouteIds(), RouteConstant.PUSH_STATUS_PUSHED);
        } catch (Exception e) {
            log.error("[RoutePush] 发送推送消息失败 | error: {}", e.getMessage(), e);
            pushLog.setPushResult(RouteConstant.PUSH_RESULT_FAILED);
            pushLog.setSuccessCount(0);
            pushLog.setFailedInstanceIds(targetInstanceIds);

            // 记录失败详情
            Map<String, String> instanceErrors = new HashMap<>();
            for (String instanceId : targetInstanceIds) {
                instanceErrors.put(instanceId, e.getMessage());
            }
            pushLog.setInstanceErrors(instanceErrors);

            // 更新路由推送状态为失败
            updateRoutePushStatus(req.getRouteIds(), RouteConstant.PUSH_STATUS_PUSH_FAILED);
        }

        // 保存推送记录
        gaRoutePushLogMapper.insert(pushLog);

        log.info("[RoutePush] 推送路由成功 | pushId: {}, routeIds: {}, targetInstances: {}, operatorUser: {}",
            pushLog.getPushId(), req.getRouteIds(), targetInstanceIds.size(), operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<QueryPushLogRsp> getPushHistory(QueryPushLogReq req) {
        QueryPushLogRsp rsp = new QueryPushLogRsp();

        // 构建查询条件
        LambdaQueryWrapper<GaRoutePushLogDO> queryWrapper = new LambdaQueryWrapper<GaRoutePushLogDO>()
            .eq(StrUtil.isNotBlank(req.getStorageMode()), GaRoutePushLogDO::getStorageMode, req.getStorageMode())
            .eq(StrUtil.isNotBlank(req.getRoutesGroup()), GaRoutePushLogDO::getRoutesGroup, req.getRoutesGroup())
            .eq(req.getPushResult() != null, GaRoutePushLogDO::getPushResult, req.getPushResult())
            .like(StrUtil.isNotBlank(req.getOperatorName()), GaRoutePushLogDO::getOperatorName, req.getOperatorName())
            .orderByDesc(GaRoutePushLogDO::getPushTime);

        PageUtils.queryPage(req, () -> gaRoutePushLogMapper.selectList(queryWrapper), rsp);

        log.info("[RoutePush] 查询推送历史成功 | count: {}", rsp.getTotal());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<QueryInstanceRoutesRsp> getInstanceRoutes(QueryInstanceRoutesReq req) {
        QueryInstanceRoutesRsp rsp = new QueryInstanceRoutesRsp();

        if (StrUtil.isBlank(req.getStorageMode())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        List<GaRouteDO> routes = new ArrayList<>();

        if (RouteConstant.STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            // 从 Redis Hash 查询
            String routesGroup = StrUtil.isBlank(req.getRoutesGroup())
                ? RouteConstant.DEFAULT_ROUTES_GROUP : req.getRoutesGroup();
            String redisKey = GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup;

            Map<String, Object> routeMap = redisClient.hGetStringMap(redisKey);
            if (routeMap != null && !routeMap.isEmpty()) {
                for (Map.Entry<String, Object> entry : routeMap.entrySet()) {
                    try {
                        GaRouteDO route = JacksonUtil.fromJson(entry.getValue().toString(), GaRouteDO.class);
                        if (route != null) {
                            routes.add(route);
                        }
                    } catch (Exception e) {
                        log.warn("[RoutePush] 解析路由 JSON 失败 | routeId: {}", entry.getKey());
                    }
                }
            }
        }
        // Nacos 模式由 NacosRouteService 处理

        rsp.setRows(routes);
        rsp.setTotal(routes.size());

        log.info("[RoutePush] 查询实例路由成功 | storageMode: {}, routesGroup: {}, count: {}",
            req.getStorageMode(), req.getRoutesGroup(), routes.size());

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<EmptyBody> rollbackPush(RollbackPushReq req) {
        if (req.getPushId() == null) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询推送记录
        GaRoutePushLogDO pushLog = gaRoutePushLogMapper.selectById(req.getPushId());
        if (ObjectUtil.isNull(pushLog)) {
            BlinkException.throwBusinessException(PUSH_LOG_NOT_EXIST);
        }

        // 获取历史路由配置
        List<GaRouteDO> routes = pushLog.getRouteSnapshot();
        if (CollUtil.isEmpty(routes)) {
            log.warn("[RoutePush] 推送记录无路由快照 | pushId: {}", req.getPushId());
            return ResponseDTO.newSuccessInstance();
        }

        // 构建新的推送请求
        PushRoutesReq pushReq = new PushRoutesReq();
        pushReq.setStorageMode(pushLog.getStorageMode());
        pushReq.setRoutesGroup(pushLog.getRoutesGroup());
        pushReq.setNacosDataId(pushLog.getNacosDataId());
        pushReq.setNacosGroup(pushLog.getNacosGroup());
        pushReq.setRouteIds(routes.stream().map(GaRouteDO::getRouteId).toList());
        pushReq.setPushMode(StrUtil.isBlank(req.getPushMode())
            ? pushLog.getPushMode() : req.getPushMode());
        pushReq.setTargetInstanceIds(CollUtil.isEmpty(req.getTargetInstanceIds())
            ? parseTargetInstanceIds(pushLog.getTargetInstanceIds())
            : req.getTargetInstanceIds());
        pushReq.setRemark(RouteConstant.REMARK_ROLLBACK_PUSH_PREFIX + req.getPushId());

        // 执行推送
        return pushRoutes(pushReq);
    }

    /**
     * 解析目标实例ID列表
     */
    private List<String> parseTargetInstanceIds(String targetInstanceIdsJson) {
        if (StrUtil.isBlank(targetInstanceIdsJson)) {
            return new ArrayList<>();
        }
        try {
            return JacksonUtil.fromJsonToList(targetInstanceIdsJson, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 批量更新路由推送状态
     *
     * @param routeIds 路由ID列表
     * @param pushStatus 推送状态
     */
    private void updateRoutePushStatus(List<String> routeIds, Byte pushStatus) {
        if (CollUtil.isEmpty(routeIds)) {
            return;
        }

        LambdaUpdateWrapper<GaRouteDO> updateWrapper = new LambdaUpdateWrapper<GaRouteDO>()
            .in(GaRouteDO::getRouteId, routeIds)
            .set(GaRouteDO::getPushStatus, pushStatus);

        if (RouteConstant.PUSH_STATUS_PUSHED.equals(pushStatus)) {
            updateWrapper.set(GaRouteDO::getLastPushTime, java.time.LocalDateTime.now());
        }

        gaRouteMapper.update(null, updateWrapper);
        log.debug("[RoutePush] 更新路由推送状态 | routeIds: {}, pushStatus: {}", routeIds, pushStatus);
    }

    @Override
    public ResponseDTO<EmptyBody> fullPushRoutes(FullPushRoutesReq req) {
        // 参数校验
        if (StrUtil.isBlank(req.getStorageMode())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询所有启用状态路由
        LambdaQueryWrapper<GaRouteDO> queryWrapper = new LambdaQueryWrapper<GaRouteDO>()
            .eq(GaRouteDO::getStatus, RouteConstant.STATUS_ENABLE);

        // 按分组筛选
        if (StrUtil.isNotBlank(req.getRoutesGroup())) {
            queryWrapper.eq(GaRouteDO::getRoutesGroup, req.getRoutesGroup());
        }

        List<GaRouteDO> enabledRoutes = gaRouteMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(enabledRoutes)) {
            log.warn("[RoutePush] 无启用状态路由可推送 | routesGroup: {}", req.getRoutesGroup());
            return ResponseDTO.newSuccessInstance();
        }

        // 构建推送请求
        PushRoutesReq pushReq = new PushRoutesReq();
        pushReq.setStorageMode(req.getStorageMode());
        pushReq.setRoutesGroup(req.getRoutesGroup());
        pushReq.setNacosDataId(req.getNacosDataId());
        pushReq.setNacosGroup(req.getNacosGroup());
        pushReq.setRouteIds(enabledRoutes.stream().map(GaRouteDO::getRouteId).toList());
        pushReq.setPushMode(RouteConstant.PUSH_MODE_BROADCAST);
        pushReq.setRemark(RouteConstant.REMARK_FULL_PUSH);

        // 执行推送
        ResponseDTO<EmptyBody> result = pushRoutes(pushReq);

        log.info("[RoutePush] 全量推送路由完成 | routesGroup: {}, count: {}", req.getRoutesGroup(), enabledRoutes.size());

        return result;
    }
}