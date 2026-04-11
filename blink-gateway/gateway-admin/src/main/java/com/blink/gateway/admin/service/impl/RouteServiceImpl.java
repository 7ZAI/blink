package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteHistoryRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.GaRouteHistoryDO;
import com.blink.gateway.admin.mapper.GaRouteHistoryMapper;
import com.blink.gateway.admin.mapper.GaRouteMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RouteAsyncSyncService;
import com.blink.gateway.admin.service.RouteService;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;

/**
 * 路由管理服务实现
 * 数据库为主存储 + Redis/Nacos 为运行时缓存
 *
 * @author binblink
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class RouteServiceImpl implements RouteService {

    @Resource
    private GaRouteMapper gaRouteMapper;

    @Resource
    private GaRouteHistoryMapper gaRouteHistoryMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private GatewayInstanceService gatewayInstanceService;

    @Resource
    private RouteAsyncSyncService routeAsyncSyncService;

    @Resource
    private NacosRouteService nacosRouteService;

    // ========== Redis 路由管理（数据库存储） ==========

    @Override
    public ResponseDTO<QueryRouteRsp> getRouteList(QueryRouteReq req) {
        QueryRouteRsp rsp = new QueryRouteRsp();

        // 构建动态查询条件
        LambdaQueryWrapper<GaRouteDO> queryWrapper = new LambdaQueryWrapper<GaRouteDO>()
                .eq(StrUtil.isNotBlank(req.getRouteId()), GaRouteDO::getRouteId, req.getRouteId())
                .like(StrUtil.isNotBlank(req.getRouteName()), GaRouteDO::getRouteName, req.getRouteName())
                .eq(StrUtil.isNotBlank(req.getRoutesGroup()), GaRouteDO::getRoutesGroup, req.getRoutesGroup())
                .eq(StrUtil.isNotBlank(req.getStorageMode()), GaRouteDO::getStorageMode, req.getStorageMode())
                .eq(req.getStatus() != null, GaRouteDO::getStatus, req.getStatus())
                .like(StrUtil.isNotBlank(req.getUri()), GaRouteDO::getUri, req.getUri())
                .orderByAsc(GaRouteDO::getOrderNum)
                .orderByDesc(GaRouteDO::getUpdateTime);

        PageUtils.queryPage(req, () -> gaRouteMapper.selectList(queryWrapper), rsp);

        log.info("[Route] 查询路由列表成功 | routesGroup: {}, count: {}", req.getRoutesGroup(), rsp.getTotal());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<GaRouteDO> getRouteDetail(String routeId) {
        if (StrUtil.isBlank(routeId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        GaRouteDO routeDO = gaRouteMapper.selectById(routeId);
        if (ObjectUtil.isNull(routeDO)) {
            BlinkException.throwBusinessException(ROUTE_NOT_EXIST);
        }

        log.info("[Route] 查询路由详情成功 | routeId: {}", routeId);
        return ResponseDTO.newSuccessInstance(routeDO);
    }

    @Override
    public ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req) {
        // 校验路由ID
        if (StrUtil.isBlank(req.getRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 检查路由ID是否已存在
        GaRouteDO existingRoute = gaRouteMapper.selectById(req.getRouteId());
        if (ObjectUtil.isNotNull(existingRoute)) {
            BlinkException.throwBusinessException(ROUTE_ID_EXISTS);
        }

        // 校验 URI
        if (StrUtil.isBlank(req.getUri())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 构建 GaRouteDO
        GaRouteDO routeDO = BeanUtil.copyProperties(req, GaRouteDO.class);
        routeDO.setStatus(RouteConstant.STATUS_ENABLE);
        if (StrUtil.isBlank(routeDO.getRoutesGroup())) {
            routeDO.setRoutesGroup(RouteConstant.DEFAULT_ROUTES_GROUP);
        }
        if (StrUtil.isBlank(routeDO.getStorageMode())) {
            routeDO.setStorageMode(RouteConstant.STORAGE_MODE_REDIS);
        }

        // 写入数据库
        gaRouteMapper.insert(routeDO);

        // 记录历史（新增操作）
        saveRouteHistory(routeDO, null, routeDO, RouteConstant.OPERATION_ADD);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        log.info("[Route] 保存路由成功（仓库路由，需手动推送生效） | routeId: {}, uri: {}, routesGroup: {}, operatorUser: {}",
                routeDO.getRouteId(), routeDO.getUri(), routeDO.getRoutesGroup(), operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<EmptyBody> updateRoute(UpdateRouteReq req) {
        // 校验路由ID
        if (StrUtil.isBlank(req.getRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询现有路由
        GaRouteDO existingRoute = gaRouteMapper.selectById(req.getRouteId());
        if (ObjectUtil.isNull(existingRoute)) {
            BlinkException.throwBusinessException(ROUTE_NOT_EXIST);
        }

        // 记录变更前数据（用于历史）
        GaRouteDO beforeData = BeanUtil.copyProperties(existingRoute, GaRouteDO.class);

        // 更新路由信息
        BeanUtil.copyProperties(req, existingRoute, "routeId", "createBy", "createTime");
        gaRouteMapper.updateById(existingRoute);

        // 记录历史（修改操作）
        saveRouteHistory(existingRoute, beforeData, existingRoute, RouteConstant.OPERATION_MODIFY);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        log.info("[Route] 更新路由成功（仓库路由，需手动推送生效） | routeId: {}, uri: {}, operatorUser: {}",
                existingRoute.getRouteId(), existingRoute.getUri(), operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req) {
        List<String> routeIds = req.getRouteIds();
        if (ObjectUtil.isNull(routeIds) || routeIds.isEmpty()) {
            return ResponseDTO.newSuccessInstance();
        }

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        for (String routeId : routeIds) {
            GaRouteDO existingRoute = gaRouteMapper.selectById(routeId);
            if (ObjectUtil.isNull(existingRoute)) {
                log.warn("[Route] 路由不存在，跳过删除 | routeId: {}", routeId);
                continue;
            }

            // 记录历史（删除操作）
            saveRouteHistory(existingRoute, existingRoute, null, RouteConstant.OPERATION_DELETE);

            // 删除数据库记录
            gaRouteMapper.deleteById(routeId);
        }

        log.info("[Route] 删除路由成功 | routeIds: {}, operatorUser: {}", routeIds, operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<QueryRouteHistoryRsp> getRouteHistory(QueryRouteHistoryReq req) {
        QueryRouteHistoryRsp rsp = new QueryRouteHistoryRsp();

        if (StrUtil.isBlank(req.getRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 构建查询条件
        LambdaQueryWrapper<GaRouteHistoryDO> queryWrapper = new LambdaQueryWrapper<GaRouteHistoryDO>()
                .eq(GaRouteHistoryDO::getRouteId, req.getRouteId())
                .eq(StrUtil.isNotBlank(req.getOperationType()), GaRouteHistoryDO::getOperationType, req.getOperationType())
                .like(StrUtil.isNotBlank(req.getOperatorName()), GaRouteHistoryDO::getOperatorName, req.getOperatorName())
                .orderByDesc(GaRouteHistoryDO::getOperateTime);

        PageUtils.queryPage(req, () -> gaRouteHistoryMapper.selectList(queryWrapper), rsp);

        log.info("[Route] 查询路由历史成功 | routeId: {}, count: {}", req.getRouteId(), rsp.getTotal());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<EmptyBody> rollbackRoute(RollbackRouteReq req) {
        if (StrUtil.isBlank(req.getRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (req.getHistoryId() == null) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询历史记录
        GaRouteHistoryDO historyDO = gaRouteHistoryMapper.selectById(req.getHistoryId());
        if (ObjectUtil.isNull(historyDO) || !req.getRouteId().equals(historyDO.getRouteId())) {
            BlinkException.throwBusinessException(ROUTE_HISTORY_NOT_EXIST);
        }

        // 根据操作类型确定回滚数据
        GaRouteDO rollbackData = null;
        if (RouteConstant.OPERATION_MODIFY.equals(historyDO.getOperationType())) {
            // 修改操作：回滚到变更前的数据
            rollbackData = historyDO.getBeforeData();
        } else if (RouteConstant.OPERATION_ADD.equals(historyDO.getOperationType())) {
            // 新增操作：回滚意味着删除（不允许）
            BlinkException.throwBusinessException(ROUTE_ROLLBACK_FAILED);
        } else if (RouteConstant.OPERATION_DELETE.equals(historyDO.getOperationType())) {
            // 删除操作：回滚到删除前的数据
            rollbackData = historyDO.getBeforeData();
        }

        if (ObjectUtil.isNull(rollbackData)) {
            BlinkException.throwBusinessException(ROUTE_ROLLBACK_FAILED);
        }

        // 查询当前路由状态
        GaRouteDO currentRoute = gaRouteMapper.selectById(req.getRouteId());

        // 记录当前数据作为回滚前数据
        GaRouteDO beforeRollback = ObjectUtil.isNotNull(currentRoute)
                ? BeanUtil.copyProperties(currentRoute, GaRouteDO.class)
                : null;

        // 执行回滚：更新或插入数据库
        if (ObjectUtil.isNotNull(currentRoute)) {
            BeanUtil.copyProperties(rollbackData, currentRoute, "routeId", "createBy", "createTime");
            gaRouteMapper.updateById(currentRoute);
        } else {
            gaRouteMapper.insert(rollbackData);
        }

        // 记录回滚历史（作为修改操作）
        GaRouteDO afterRollback = gaRouteMapper.selectById(req.getRouteId());
        saveRouteHistory(afterRollback, beforeRollback, afterRollback, RouteConstant.OPERATION_MODIFY);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        // 是否同步到运行时存储（默认同步）
        if (ObjectUtil.isNull(req.getSyncToStorage()) || req.getSyncToStorage()) {
            routeAsyncSyncService.syncModifyRoute(req.getRouteId(), afterRollback, operatorUser, operatorName);
        }

        log.info("[Route] 回滚路由成功 | routeId: {}, historyId: {}, operatorUser: {}",
                req.getRouteId(), req.getHistoryId(), operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<EmptyBody> refreshRoutes() {
        // 路由刷新会通过 Redis Stream 自动触发
        messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);
        log.info("[Route] 刷新路由缓存成功");
        return ResponseDTO.newSuccessInstance();
    }

    // ========== Nacos 路由管理（委托给 NacosRouteService） ==========

    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req) {
        return nacosRouteService.getNacosRouteList(req);
    }

    @Override
    public ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req) {
        return nacosRouteService.saveNacosRoute(req);
    }

    @Override
    public ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req) {
        return nacosRouteService.deleteNacosRoute(req);
    }

    // ========== 存储方式和实例同步 ==========

    @Override
    public ResponseDTO<List<StorageModeVO>> getStorageModes() {
        List<StorageModeVO> modes = new ArrayList<>();

        StorageModeVO redisMode = new StorageModeVO();
        redisMode.setMode(RouteConstant.STORAGE_MODE_REDIS);
        redisMode.setName("Redis 存储");
        redisMode.setDescription("路由存储在 Redis Hash，支持实时同步");
        modes.add(redisMode);

        StorageModeVO nacosMode = new StorageModeVO();
        nacosMode.setMode(RouteConstant.STORAGE_MODE_NACOS);
        nacosMode.setName("Nacos 配置");
        nacosMode.setDescription("路由存储在 Nacos Config，支持配置历史");
        modes.add(nacosMode);

        return ResponseDTO.newSuccessInstance(modes);
    }

    @Override
    public ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances() {
        ResponseDTO<GatewayInstanceListRsp> rsp = gatewayInstanceService.getGatewayInstances();
        List<GatewayInstanceVO> onlineInstances = rsp.getBody().getInstances().stream()
                .filter(instance -> instance.getStatus() == 0)
                .collect(Collectors.toList());

        log.info("[Route] 获取在线网关实例成功 | count: {}", onlineInstances.size());
        return ResponseDTO.newSuccessInstance(onlineInstances);
    }

    @Override
    public ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req) {
        RouteSyncMsg routeSyncMsg = new RouteSyncMsg();
        routeSyncMsg.setStorageMode(req.getStorageMode());
        routeSyncMsg.setPushMode(req.getPushMode());
        routeSyncMsg.setTargetInstanceIds(req.getTargetInstanceIds());

        if (RouteConstant.STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            String routesGroup = req.getRoutesGroup();
            if (StrUtil.isBlank(routesGroup)) {
                routesGroup = RouteConstant.DEFAULT_ROUTES_GROUP;
            }
            routeSyncMsg.setDynamicRouteKey(GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup);
        } else if (RouteConstant.STORAGE_MODE_NACOS.equals(req.getStorageMode())) {
            routeSyncMsg.setDataId(req.getDataId());
            routeSyncMsg.setGroup(req.getGroup());
        }

        messageProducer.routesOnChangeWithTarget(routeSyncMsg);

        log.info("[Route] 同步路由到实例成功 | storageMode: {}, pushMode: {}, targetInstances: {}",
                req.getStorageMode(), req.getPushMode(), req.getTargetInstanceIds());

        return ResponseDTO.newSuccessInstance();
    }

    // ========== 私有方法 ==========

    /**
     * 保存路由变更历史
     *
     * @param routeDO       路由数据
     * @param beforeData    变更前数据
     * @param afterData     变更后数据
     * @param operationType 操作类型
     */
    private void saveRouteHistory(GaRouteDO routeDO, GaRouteDO beforeData, GaRouteDO afterData, String operationType) {
        GaRouteHistoryDO historyDO = new GaRouteHistoryDO();
        historyDO.setRouteId(routeDO.getRouteId());
        historyDO.setRouteName(routeDO.getRouteName());
        historyDO.setOperationType(operationType);
        historyDO.setBeforeData(beforeData);
        historyDO.setAfterData(afterData);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;
        historyDO.setOperatorId(operatorUser);
        historyDO.setOperatorName(operatorName);

        gaRouteHistoryMapper.insert(historyDO);
    }
}