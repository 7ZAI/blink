package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.dto.req.*;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushStatusRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteHistoryRsp;
import com.blink.gateway.admin.dto.rsp.ImportRoutesRsp;
import com.blink.gateway.admin.dto.rsp.RoutesGroupStatsRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.RoutePushStatusVO;
import com.blink.gateway.admin.dto.vo.RoutesGroupStatsVO;
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
import com.blink.gateway.admin.service.RouteValidator;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blink.framework.common.utils.JacksonUtil;

import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PARAMETER_NOT_NULL;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_HISTORY_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_ID_EXISTS;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_ROLLBACK_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_VERSION_MISMATCH;
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

    @Resource
    private RouteValidator routeValidator;

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
        routeValidator.validateRouteIdUnique(req.getRouteId(), false);

        // 校验路由配置完整性（URI格式、断言必填、断言/过滤器类型、路由冲突）
        routeValidator.validateRouteConfig(null, req.getUri(), req.getPredicates(), req.getFilters());

        // 构建 GaRouteDO
        GaRouteDO routeDO = BeanUtil.copyProperties(req, GaRouteDO.class);
        routeDO.setStatus(RouteConstant.STATUS_ENABLE);
        if (StrUtil.isBlank(routeDO.getRoutesGroup())) {
            routeDO.setRoutesGroup(RouteConstant.DEFAULT_ROUTES_GROUP);
        }
        if (StrUtil.isBlank(routeDO.getStorageMode())) {
            routeDO.setStorageMode(RouteConstant.STORAGE_MODE_REDIS);
        }

        // 初始化乐观锁和推送状态字段
        routeDO.setVersion(0);
        routeDO.setPushStatus(RouteConstant.PUSH_STATUS_NOT_PUSHED);
        routeDO.setLastPushTime(null);

        // 写入数据库
        gaRouteMapper.insert(routeDO);

        // 记录历史（新增操作）
        saveRouteHistory(routeDO, null, routeDO, RouteConstant.OPERATION_ADD, null);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        // 自动同步到运行时存储
        if (Boolean.TRUE.equals(req.getAutoSync())) {
            routeAsyncSyncService.syncAddRoute(routeDO.getRouteId(), routeDO, operatorUser, operatorName);
            log.info("[Route] 自动同步路由到运行时存储 | routeId: {}", routeDO.getRouteId());
        }

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

        // 乐观锁校验：版本号必须匹配
        if (ObjectUtil.isNotNull(req.getVersion()) && !ObjectUtil.equals(req.getVersion(), existingRoute.getVersion())) {
            log.warn("[Route] 路由版本不匹配，拒绝更新 | routeId: {}, reqVersion: {}, dbVersion: {}",
                    req.getRouteId(), req.getVersion(), existingRoute.getVersion());
            BlinkException.throwBusinessException(ROUTE_VERSION_MISMATCH);
        }

        // 校验路由配置完整性（更新时传入routeId排除自身）
        routeValidator.validateRouteConfig(req.getRouteId(), req.getUri(), req.getPredicates(), req.getFilters());

        // 记录变更前数据（用于历史）
        GaRouteDO beforeData = BeanUtil.copyProperties(existingRoute, GaRouteDO.class);

        // 更新路由信息
        BeanUtil.copyProperties(req, existingRoute, "routeId", "createBy", "createTime");

        // 版本号自增
        existingRoute.setVersion(existingRoute.getVersion() + 1);

        gaRouteMapper.updateById(existingRoute);

        // 计算变更字段列表
        List<String> changedFields = calculateChangedFields(beforeData, existingRoute);

        // 记录历史（修改操作）
        saveRouteHistory(existingRoute, beforeData, existingRoute, RouteConstant.OPERATION_MODIFY, changedFields);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        // 自动同步到运行时存储
        if (Boolean.TRUE.equals(req.getAutoSync())) {
            routeAsyncSyncService.syncModifyRoute(req.getRouteId(), existingRoute, operatorUser, operatorName);
            log.info("[Route] 自动同步路由到运行时存储 | routeId: {}", req.getRouteId());
        }

        log.info("[Route] 更新路由成功（仓库路由，需手动推送生效） | routeId: {}, uri: {}, version: {}, operatorUser: {}",
                existingRoute.getRouteId(), existingRoute.getUri(), existingRoute.getVersion(), operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req) {
        List<String> routeIds = req.getRouteIds();
        if (CollUtil.isEmpty(routeIds)) {
            return ResponseDTO.newSuccessInstance();
        }

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        // 批量查询现有路由
        List<GaRouteDO> existingRoutes = gaRouteMapper.selectBatchIds(routeIds);
        if (CollUtil.isEmpty(existingRoutes)) {
            log.warn("[Route] 未找到要删除的路由 | routeIds: {}", routeIds);
            return ResponseDTO.newSuccessInstance();
        }

        // 批量记录历史（删除操作）
        for (GaRouteDO route : existingRoutes) {
            saveRouteHistory(route, route, null, RouteConstant.OPERATION_DELETE, null);
        }

        // 批量删除数据库记录
        gaRouteMapper.deleteBatchIds(routeIds);

        log.info("[Route] 批量删除路由成功 | routeIds: {}, count: {}, operatorUser: {}", routeIds, existingRoutes.size(), operatorUser);

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
        List<String> rollbackChangedFields = calculateChangedFields(beforeRollback, afterRollback);
        saveRouteHistory(afterRollback, beforeRollback, afterRollback, RouteConstant.OPERATION_MODIFY, rollbackChangedFields);

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
                .filter(instance -> instance.getStatus().equals(INSTANCE_STATUS_ONLINE))
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

    @Override
    public ResponseDTO<QueryPushStatusRsp> getPushStatus(QueryPushStatusReq req) {
        QueryPushStatusRsp rsp = new QueryPushStatusRsp();

        // 构建查询条件
        LambdaQueryWrapper<GaRouteDO> queryWrapper = new LambdaQueryWrapper<GaRouteDO>()
            .in(CollUtil.isNotEmpty(req.getRouteIds()), GaRouteDO::getRouteId, req.getRouteIds())
            .eq(StrUtil.isNotBlank(req.getRoutesGroup()), GaRouteDO::getRoutesGroup, req.getRoutesGroup())
            .eq(req.getPushStatus() != null, GaRouteDO::getPushStatus, req.getPushStatus())
            .orderByDesc(GaRouteDO::getUpdateTime);

        // 查询路由列表
        List<GaRouteDO> routes = gaRouteMapper.selectList(queryWrapper);

        // 转换为 VO
        List<RoutePushStatusVO> voList = routes.stream().map(route -> {
            RoutePushStatusVO vo = new RoutePushStatusVO();
            vo.setRouteId(route.getRouteId());
            vo.setRouteName(route.getRouteName());
            vo.setPushStatus(route.getPushStatus());
            vo.setPushStatusDesc(getPushStatusDesc(route.getPushStatus()));
            vo.setLastPushTime(route.getLastPushTime());
            vo.setVersion(route.getVersion());
            return vo;
        }).collect(Collectors.toList());

        rsp.setRows(voList);
        rsp.setTotal(voList.size());

        log.info("[Route] 查询推送状态成功 | count: {}", rsp.getTotal());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取推送状态描述
     *
     * @param pushStatus 推送状态码
     * @return 状态描述
     */
    private String getPushStatusDesc(Byte pushStatus) {
        if (pushStatus == null) {
            return RouteConstant.PUSH_STATUS_DESC_UNKNOWN;
        }
        if (RouteConstant.PUSH_STATUS_NOT_PUSHED.equals(pushStatus)) {
            return RouteConstant.PUSH_STATUS_DESC_NOT_PUSHED;
        }
        if (RouteConstant.PUSH_STATUS_PUSHED.equals(pushStatus)) {
            return RouteConstant.PUSH_STATUS_DESC_PUSHED;
        }
        if (RouteConstant.PUSH_STATUS_PUSH_FAILED.equals(pushStatus)) {
            return RouteConstant.PUSH_STATUS_DESC_PUSH_FAILED;
        }
        return RouteConstant.PUSH_STATUS_DESC_UNKNOWN;
    }

    // ========== 私有方法 ==========

    /**
     * 保存路由变更历史
     *
     * @param routeDO       路由数据
     * @param beforeData    变更前数据
     * @param afterData     变更后数据
     * @param operationType 操作类型
     * @param changedFields 变更字段列表
     */
    private void saveRouteHistory(GaRouteDO routeDO, GaRouteDO beforeData, GaRouteDO afterData, String operationType, List<String> changedFields) {
        GaRouteHistoryDO historyDO = new GaRouteHistoryDO();
        historyDO.setRouteId(routeDO.getRouteId());
        historyDO.setRouteName(routeDO.getRouteName());
        historyDO.setOperationType(operationType);
        historyDO.setBeforeData(beforeData);
        historyDO.setAfterData(afterData);
        historyDO.setChangedFields(changedFields);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;
        historyDO.setOperatorId(operatorUser);
        historyDO.setOperatorName(operatorName);

        gaRouteHistoryMapper.insert(historyDO);
    }

    /**
     * 计算变更字段列表
     * 对比变更前后数据，返回变更的字段名列表
     *
     * @param beforeData 变更前数据
     * @param afterData 变更后数据
     * @return 变更字段名列表
     */
    private List<String> calculateChangedFields(GaRouteDO beforeData, GaRouteDO afterData) {
        List<String> changedFields = new ArrayList<>();

        if (beforeData == null || afterData == null) {
            return changedFields;
        }

        // 对比各字段
        if (!ObjectUtil.equals(beforeData.getRouteName(), afterData.getRouteName())) {
            changedFields.add(RouteConstant.FIELD_ROUTE_NAME);
        }
        if (!ObjectUtil.equals(beforeData.getUri(), afterData.getUri())) {
            changedFields.add(RouteConstant.FIELD_URI);
        }
        if (!ObjectUtil.equals(beforeData.getPredicates(), afterData.getPredicates())) {
            changedFields.add(RouteConstant.FIELD_PREDICATES);
        }
        if (!ObjectUtil.equals(beforeData.getFilters(), afterData.getFilters())) {
            changedFields.add(RouteConstant.FIELD_FILTERS);
        }
        if (!ObjectUtil.equals(beforeData.getOrderNum(), afterData.getOrderNum())) {
            changedFields.add(RouteConstant.FIELD_ORDER_NUM);
        }
        if (!ObjectUtil.equals(beforeData.getMetadata(), afterData.getMetadata())) {
            changedFields.add(RouteConstant.FIELD_METADATA);
        }
        if (!ObjectUtil.equals(beforeData.getRoutesGroup(), afterData.getRoutesGroup())) {
            changedFields.add(RouteConstant.FIELD_ROUTES_GROUP);
        }
        if (!ObjectUtil.equals(beforeData.getStorageMode(), afterData.getStorageMode())) {
            changedFields.add(RouteConstant.FIELD_STORAGE_MODE);
        }
        if (!ObjectUtil.equals(beforeData.getStatus(), afterData.getStatus())) {
            changedFields.add(RouteConstant.FIELD_STATUS);
        }

        return changedFields;
    }

    @Override
    public ResponseDTO<EmptyBody> batchUpdateStatus(BatchUpdateStatusReq req) {
        // 参数校验
        Byte status = req.getStatus();
        if (status == null) {
            status = RouteConstant.STATUS_ENABLE;
        }

        List<String> routeIds = req.getRouteIds();
        if (CollUtil.isEmpty(routeIds) && StrUtil.isBlank(req.getRoutesGroup())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        // 构建查询条件
        LambdaQueryWrapper<GaRouteDO> queryWrapper = new LambdaQueryWrapper<GaRouteDO>();
        if (CollUtil.isNotEmpty(routeIds)) {
            queryWrapper.in(GaRouteDO::getRouteId, routeIds);
        } else {
            queryWrapper.eq(GaRouteDO::getRoutesGroup, req.getRoutesGroup());
        }

        // 查询要更新的路由
        List<GaRouteDO> routes = gaRouteMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(routes)) {
            log.warn("[Route] 未找到要更新状态的路由 | routeIds: {}, routesGroup: {}", routeIds, req.getRoutesGroup());
            return ResponseDTO.newSuccessInstance();
        }

        // 批量更新状态
        LambdaUpdateWrapper<GaRouteDO> updateWrapper = new LambdaUpdateWrapper<GaRouteDO>()
            .in(GaRouteDO::getRouteId, routes.stream().map(GaRouteDO::getRouteId).toList())
            .set(GaRouteDO::getStatus, status);

        gaRouteMapper.update(null, updateWrapper);

        // 批量记录历史
        for (GaRouteDO route : routes) {
            List<String> changedFields = List.of(RouteConstant.FIELD_STATUS);
            saveRouteHistory(route, route, route, RouteConstant.OPERATION_MODIFY, changedFields);
        }

        log.info("[Route] 批量更新状态成功 | routeIds: {}, count: {}, status: {}, operatorUser: {}",
                routeIds, routes.size(), status, operatorUser);

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<RoutesGroupStatsRsp> getRoutesGroupStats() {
        RoutesGroupStatsRsp rsp = new RoutesGroupStatsRsp();

        // 查询所有路由
        List<GaRouteDO> allRoutes = gaRouteMapper.selectList(null);

        // 按分组统计
        Map<String, List<GaRouteDO>> groupMap = allRoutes.stream()
            .collect(Collectors.groupingBy(
                route -> StrUtil.isBlank(route.getRoutesGroup()) ? RouteConstant.DEFAULT_ROUTES_GROUP : route.getRoutesGroup()
            ));

        List<RoutesGroupStatsVO> statsList = new ArrayList<>();
        for (Map.Entry<String, List<GaRouteDO>> entry : groupMap.entrySet()) {
            String group = entry.getKey();
            List<GaRouteDO> routes = entry.getValue();

            RoutesGroupStatsVO stats = new RoutesGroupStatsVO();
            stats.setRoutesGroup(group);
            stats.setTotalCount(routes.size());
            stats.setEnabledCount((int) routes.stream().filter(r -> RouteConstant.STATUS_ENABLE.equals(r.getStatus())).count());
            stats.setDisabledCount((int) routes.stream().filter(r -> RouteConstant.STATUS_DISABLE.equals(r.getStatus())).count());
            stats.setPushedCount((int) routes.stream().filter(r -> RouteConstant.PUSH_STATUS_PUSHED.equals(r.getPushStatus())).count());
            stats.setNotPushedCount((int) routes.stream().filter(r -> RouteConstant.PUSH_STATUS_NOT_PUSHED.equals(r.getPushStatus())).count());
            stats.setPushFailedCount((int) routes.stream().filter(r -> RouteConstant.PUSH_STATUS_PUSH_FAILED.equals(r.getPushStatus())).count());

            statsList.add(stats);
        }

        rsp.setGroups(statsList);

        log.info("[Route] 查询分组统计成功 | count: {}", statsList.size());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<String> exportRoutes(ExportRoutesReq req) {
        // 构建查询条件
        LambdaQueryWrapper<GaRouteDO> queryWrapper = new LambdaQueryWrapper<GaRouteDO>()
            .in(CollUtil.isNotEmpty(req.getRouteIds()), GaRouteDO::getRouteId, req.getRouteIds())
            .eq(StrUtil.isNotBlank(req.getRoutesGroup()), GaRouteDO::getRoutesGroup, req.getRoutesGroup())
            .eq(StrUtil.isNotBlank(req.getStorageMode()), GaRouteDO::getStorageMode, req.getStorageMode())
            .orderByAsc(GaRouteDO::getOrderNum);

        List<GaRouteDO> routes = gaRouteMapper.selectList(queryWrapper);
        String jsonContent = JacksonUtil.toJson(routes);

        log.info("[Route] 导出路由成功 | count: {}", routes.size());
        return ResponseDTO.newSuccessInstance(jsonContent);
    }

    @Override
    public ResponseDTO<ImportRoutesRsp> importRoutes(ImportRoutesReq req) {
        if (StrUtil.isBlank(req.getJsonContent())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 解析JSON
        List<GaRouteDO> routes = JacksonUtil.fromJsonToList(req.getJsonContent(), GaRouteDO.class);
        if (CollUtil.isEmpty(routes)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        ImportRoutesRsp rsp = new ImportRoutesRsp();
        List<ImportRoutesRsp.ImportFailureDetail> failures = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        Boolean overwrite = Boolean.TRUE.equals(req.getOverwrite());

        for (GaRouteDO route : routes) {
            try {
                // 校验路由配置
                routeValidator.validateRouteConfig(null, route.getUri(), route.getPredicates(), route.getFilters());

                // 检查是否已存在
                GaRouteDO existing = gaRouteMapper.selectById(route.getRouteId());
                if (existing != null && !overwrite) {
                    failures.add(new ImportRoutesRsp.ImportFailureDetail(route.getRouteId(), "路由ID已存在"));
                    failedCount++;
                    continue;
                }

                // 初始化字段
                route.setVersion(0);
                route.setPushStatus(RouteConstant.PUSH_STATUS_NOT_PUSHED);
                route.setLastPushTime(null);
                if (route.getStatus() == null) {
                    route.setStatus(RouteConstant.STATUS_ENABLE);
                }
                if (StrUtil.isBlank(route.getRoutesGroup())) {
                    route.setRoutesGroup(RouteConstant.DEFAULT_ROUTES_GROUP);
                }
                if (StrUtil.isBlank(route.getStorageMode())) {
                    route.setStorageMode(RouteConstant.STORAGE_MODE_REDIS);
                }

                // 保存或更新
                if (existing != null) {
                    route.setVersion(existing.getVersion() + 1);
                    gaRouteMapper.updateById(route);
                } else {
                    gaRouteMapper.insert(route);
                }

                // 记录历史
                saveRouteHistory(route, existing, route, existing != null ? RouteConstant.OPERATION_MODIFY : RouteConstant.OPERATION_ADD, null);

                successCount++;
            } catch (Exception e) {
                failures.add(new ImportRoutesRsp.ImportFailureDetail(route.getRouteId(), e.getMessage()));
                failedCount++;
            }
        }

        rsp.setSuccessCount(successCount);
        rsp.setFailedCount(failedCount);
        rsp.setFailures(failures);

        log.info("[Route] 导入路由完成 | success: {}, failed: {}", successCount, failedCount);
        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<EmptyBody> cloneRoute(CloneRouteReq req) {
        // 参数校验
        if (StrUtil.isBlank(req.getSourceRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(req.getNewRouteId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询源路由
        GaRouteDO sourceRoute = gaRouteMapper.selectById(req.getSourceRouteId());
        if (ObjectUtil.isNull(sourceRoute)) {
            BlinkException.throwBusinessException(ROUTE_NOT_EXIST);
        }

        // 检查新路由ID是否已存在
        routeValidator.validateRouteIdUnique(req.getNewRouteId(), false);

        // 克隆路由
        GaRouteDO clonedRoute = BeanUtil.copyProperties(sourceRoute, GaRouteDO.class);
        clonedRoute.setRouteId(req.getNewRouteId());
        clonedRoute.setRouteName(StrUtil.isNotBlank(req.getNewRouteName()) ? req.getNewRouteName() : sourceRoute.getRouteName() + RouteConstant.CLONED_ROUTE_NAME_SUFFIX);
        clonedRoute.setVersion(0);
        clonedRoute.setPushStatus(RouteConstant.PUSH_STATUS_NOT_PUSHED);
        clonedRoute.setLastPushTime(null);

        gaRouteMapper.insert(clonedRoute);

        // 记录历史
        saveRouteHistory(clonedRoute, null, clonedRoute, RouteConstant.OPERATION_ADD, null);

        log.info("[Route] 克隆路由成功 | sourceRouteId: {}, newRouteId: {}", req.getSourceRouteId(), req.getNewRouteId());
        return ResponseDTO.newSuccessInstance();
    }
}