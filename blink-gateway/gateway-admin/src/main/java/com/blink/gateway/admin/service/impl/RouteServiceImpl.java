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
import com.blink.gateway.admin.dto.rsp.DiffStats;
import com.blink.gateway.admin.dto.rsp.FieldDiff;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.InstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushStatusRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteRsp;
import com.blink.gateway.admin.dto.rsp.QueryRouteHistoryRsp;
import com.blink.gateway.admin.dto.rsp.ImportRoutesRsp;
import com.blink.gateway.admin.dto.rsp.RouteDiffItem;
import com.blink.gateway.admin.dto.rsp.RouteDiffRsp;
import com.blink.gateway.admin.dto.rsp.RoutesGroupStatsRsp;
import com.blink.gateway.admin.dto.rsp.SyncRoutesFromInstanceRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.RoutePushStatusVO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.dto.vo.RoutesGroupStatsVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.GaRouteHistoryDO;
import com.blink.gateway.admin.entity.GatewayRouteGroupDO;
import com.blink.gateway.admin.entity.PredicateConfig;
import com.blink.gateway.admin.entity.FilterConfig;
import com.blink.gateway.admin.mapper.GaRouteHistoryMapper;
import com.blink.gateway.admin.mapper.GaRouteMapper;
import com.blink.gateway.admin.mapper.GatewayRouteGroupMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RoutePushService;
import com.blink.gateway.admin.service.RouteService;
import com.blink.gateway.admin.service.RouteValidator;
import com.blink.gateway.admin.component.NacosConfigComponent;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

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
    private NacosRouteService nacosRouteService;

    @Resource
    private RouteValidator routeValidator;

    @Resource
    private RoutePushService routePushService;

    @Resource
    private GatewayRouteGroupMapper gatewayRouteGroupMapper;

    /**
     * Nacos 配置组件（可选注入）
     */
    private NacosConfigComponent nacosConfigComponent;

    @Autowired(required = false)
    public void setNacosConfigComponent(NacosConfigComponent nacosConfigComponent) {
        this.nacosConfigComponent = nacosConfigComponent;
    }

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
        // 同时检查服务名是否在注册中心，返回警告信息
        String serviceWarning = routeValidator.validateRouteConfigWithServiceWarning(
                null, req.getUri(), req.getPredicates(), req.getFilters(), req.getIgnoreServiceNotRegistered());

        // 如果有警告且用户未确认，返回警告让前端提示用户
        if (serviceWarning != null) {
            ResponseDTO<EmptyBody> warningRsp = ResponseDTO.newSuccessInstance();
            warningRsp.setMsgInfo(serviceWarning);
            return warningRsp;
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

        log.info("[Route] 保存路由成功 | routeId: {}, uri: {}, routesGroup: {}, operatorUser: {}",
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

        // 乐观锁校验：版本号必须匹配（仅在数据库中version不为null时校验）
        if (ObjectUtil.isNotNull(existingRoute.getVersion())
                && ObjectUtil.isNotNull(req.getVersion())
                && !ObjectUtil.equals(req.getVersion(), existingRoute.getVersion())) {
            log.warn("[Route] 路由版本不匹配，拒绝更新 | routeId: {}, reqVersion: {}, dbVersion: {}",
                    req.getRouteId(), req.getVersion(), existingRoute.getVersion());
            BlinkException.throwBusinessException(ROUTE_VERSION_MISMATCH);
        }

        // 校验路由配置完整性（更新时传入routeId排除自身）
        // 同时检查服务名是否在注册中心，返回警告信息
        String serviceWarning = routeValidator.validateRouteConfigWithServiceWarning(
                req.getRouteId(), req.getUri(), req.getPredicates(), req.getFilters(), req.getIgnoreServiceNotRegistered());

        // 如果有警告且用户未确认，返回警告让前端提示用户
        if (serviceWarning != null) {
            ResponseDTO<EmptyBody> warningRsp = ResponseDTO.newSuccessInstance();
            warningRsp.setMsgInfo(serviceWarning);
            return warningRsp;
        }

        // 记录变更前数据（用于历史）
        GaRouteDO beforeData = BeanUtil.copyProperties(existingRoute, GaRouteDO.class);

        // 更新路由信息
        BeanUtil.copyProperties(req, existingRoute, "routeId", "createBy", "createTime");

        // 版本号自增（处理null情况）
        Integer currentVersion = existingRoute.getVersion();
        existingRoute.setVersion(currentVersion == null ? 1 : currentVersion + 1);

        gaRouteMapper.updateById(existingRoute);

        // 计算变更字段列表
        List<String> changedFields = calculateChangedFields(beforeData, existingRoute);

        // 记录历史（修改操作）
        saveRouteHistory(existingRoute, beforeData, existingRoute, RouteConstant.OPERATION_MODIFY, changedFields);

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;

        log.info("[Route] 更新路由成功 | routeId: {}, uri: {}, version: {}, operatorUser: {}",
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
        List<GaRouteDO> existingRoutes = gaRouteMapper.selectByIds(routeIds);
        if (CollUtil.isEmpty(existingRoutes)) {
            log.warn("[Route] 未找到要删除的路由 | routeIds: {}", routeIds);
            return ResponseDTO.newSuccessInstance();
        }

        // 批量记录历史（删除操作）
        for (GaRouteDO route : existingRoutes) {
            saveRouteHistory(route, route, null, RouteConstant.OPERATION_DELETE, null);
        }

        // 批量删除数据库记录
        gaRouteMapper.deleteByIds(routeIds);

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

    @Override
    public ResponseDTO<SyncRoutesFromInstanceRsp> syncRoutesFromInstance(SyncRoutesFromInstanceReq req) {
        // 分组必填
        if (StrUtil.isBlank(req.getRoutesGroup())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        String routesGroup = req.getRoutesGroup();

        // 获取分组对应的存储方式
        String storageMode = getStorageModeByGroupKey(routesGroup);

        // 1. 根据存储方式从 Redis 或 Nacos 获取路由
        List<GaRouteDO> storageRoutes = getRoutesFromStorage(routesGroup, storageMode);

        if (CollUtil.isEmpty(storageRoutes)) {
            log.info("[Route] 存储中无路由可同步 | routesGroup: {}, storageMode: {}", routesGroup, storageMode);
            SyncRoutesFromInstanceRsp emptyRsp = new SyncRoutesFromInstanceRsp();
            emptyRsp.setAddedCount(0);
            emptyRsp.setUpdatedCount(0);
            emptyRsp.setAddedRoutes(new ArrayList<>());
            emptyRsp.setUpdatedRoutes(new ArrayList<>());
            return ResponseDTO.newSuccessInstance(emptyRsp);
        }

        // 2. 查询本地路由
        List<GaRouteDO> localRoutes = gaRouteMapper.selectList(
            new LambdaQueryWrapper<GaRouteDO>()
                .eq(GaRouteDO::getRoutesGroup, routesGroup)
        );

        // 本地路由ID集合
        Map<String, GaRouteDO> localRouteMap = localRoutes.stream()
            .collect(Collectors.toMap(GaRouteDO::getRouteId, r -> r, (a, b) -> a));

        // 3. 增量同步：新增 + 更新
        List<String> addedRoutes = new ArrayList<>();
        List<String> updatedRoutes = new ArrayList<>();

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        for (GaRouteDO storageRoute : storageRoutes) {
            // 设置目标分组和存储方式
            storageRoute.setRoutesGroup(routesGroup);
            storageRoute.setStorageMode(storageMode);

            GaRouteDO localRoute = localRouteMap.get(storageRoute.getRouteId());

            if (ObjectUtil.isNull(localRoute)) {
                // 新增：本地不存在该路由
                storageRoute.setVersion(0);
                storageRoute.setPushStatus(RouteConstant.PUSH_STATUS_PUSHED);
                storageRoute.setLastPushTime(LocalDateTime.now());
                if (storageRoute.getStatus() == null) {
                    storageRoute.setStatus(RouteConstant.STATUS_ENABLE);
                }

                gaRouteMapper.insert(storageRoute);

                // 记录历史
                saveRouteHistory(storageRoute, null, storageRoute, RouteConstant.OPERATION_ADD, null);

                addedRoutes.add(storageRoute.getRouteId());
            } else {
                // 更新：本地已存在该路由，保留 version、pushStatus 等
                GaRouteDO beforeData = BeanUtil.copyProperties(localRoute, GaRouteDO.class);

                BeanUtil.copyProperties(storageRoute, localRoute,
                    "routeId", "version", "pushStatus", "lastPushTime", "createBy", "createTime", "routesGroup", "storageMode");

                // 版本号自增
                Integer currentVersion = localRoute.getVersion();
                localRoute.setVersion(currentVersion == null ? 1 : currentVersion + 1);

                gaRouteMapper.updateById(localRoute);

                // 计算变更字段
                List<String> changedFields = calculateChangedFields(beforeData, localRoute);

                // 记录历史
                saveRouteHistory(localRoute, beforeData, localRoute, RouteConstant.OPERATION_MODIFY, changedFields);

                updatedRoutes.add(storageRoute.getRouteId());
            }
        }

        // 4. 返回结果
        SyncRoutesFromInstanceRsp rsp = new SyncRoutesFromInstanceRsp();
        rsp.setAddedCount(addedRoutes.size());
        rsp.setUpdatedCount(updatedRoutes.size());
        rsp.setAddedRoutes(addedRoutes);
        rsp.setUpdatedRoutes(updatedRoutes);

        log.info("[Route] 从存储同步路由成功 | routesGroup: {}, storageMode: {}, added: {}, updated: {}, operatorUser: {}",
            routesGroup, storageMode, addedRoutes.size(), updatedRoutes.size(), operatorUser);

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 根据存储方式从 Redis 或 Nacos 获取路由
     *
     * @param routesGroup 路由分组
     * @param storageMode 存储方式（redis/nacos）
     * @return 路由列表
     */
    private List<GaRouteDO> getRoutesFromStorage(String routesGroup, String storageMode) {
        if (RouteConstant.STORAGE_MODE_REDIS.equals(storageMode)) {
            return getRoutesFromRedis(routesGroup);
        } else if (RouteConstant.STORAGE_MODE_NACOS.equals(storageMode)) {
            return getRoutesFromNacos(routesGroup);
        }

        log.warn("[Route] 未知的存储方式 | storageMode: {}", storageMode);
        return new ArrayList<>();
    }

    /**
     * 从 Redis Hash 获取路由
     * Key 格式：blink:gateway:admin:gateway:routes:{groupKey}
     *
     * @param routesGroup 路由分组
     * @return 路由列表
     */
    private List<GaRouteDO> getRoutesFromRedis(String routesGroup) {
        List<GaRouteDO> routes = new ArrayList<>();

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
                    log.warn("[Route] 解析 Redis 路由 JSON 失败 | routeId: {}, error: {}", entry.getKey(), e.getMessage());
                }
            }
        }

        log.info("[Route] 从 Redis 获取路由 | redisKey: {}, count: {}", redisKey, routes.size());
        return routes;
    }

    /**
     * 从 Nacos 配置文件获取路由
     * DataId 格式：gateway-routes-{groupKey}.json
     *
     * @param routesGroup 路由分组
     * @return 路由列表
     */
    private List<GaRouteDO> getRoutesFromNacos(String routesGroup) {
        List<GaRouteDO> routes = new ArrayList<>();

        if (nacosConfigComponent == null) {
            log.warn("[Route] NacosConfigComponent 未注入，无法从 Nacos 获取路由");
            return routes;
        }

        // 构建配置文件 DataId：gateway-routes-{groupKey}.json
        String dataId = RouteConstant.NACOS_ROUTE_CONFIG_PREFIX + "-" + routesGroup + RouteConstant.NACOS_ROUTE_CONFIG_SUFFIX;
        String group = RouteConstant.NACOS_ROUTE_CONFIG_GROUP;

        try {
            String configContent = nacosConfigComponent.getConfig(dataId, group);
            if (StrUtil.isNotBlank(configContent)) {
                // 解析 JSON 数组格式的路由配置
                List<Map<String, Object>> routeMapList = JacksonUtil.fromJson(configContent,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

                if (routeMapList != null) {
                    for (Map<String, Object> routeMap : routeMapList) {
                        GaRouteDO route = convertNacosRouteToGaRouteDO(routeMap);
                        if (route != null) {
                            routes.add(route);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Route] 从 Nacos 获取路由配置失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
        }

        log.info("[Route] 从 Nacos 获取路由 | dataId: {}, group: {}, count: {}", dataId, group, routes.size());
        return routes;
    }

    /**
     * 将 Nacos 路由格式转换为 GaRouteDO
     * Nacos 格式：id, uri, predicates, filters, order, metadata
     * 数据库格式：routeId, routeName, uri, predicates, filters, orderNum, status
     *
     * @param routeMap 路由 Map
     * @return GaRouteDO
     */
    @SuppressWarnings("unchecked")
    private GaRouteDO convertNacosRouteToGaRouteDO(Map<String, Object> routeMap) {
        if (routeMap == null) {
            return null;
        }

        GaRouteDO route = new GaRouteDO();

        // id -> routeId
        Object id = routeMap.get("id");
        if (id != null) {
            route.setRouteId(id.toString());
        }

        // uri
        Object uri = routeMap.get("uri");
        if (uri != null) {
            route.setUri(uri.toString());
        }

        // order -> orderNum
        Object order = routeMap.get("order");
        if (order != null) {
            route.setOrderNum(((Number) order).intValue());
        }

        // predicates - 转换为 List<PredicateConfig>
        Object predicatesObj = routeMap.get("predicates");
        if (predicatesObj instanceof List) {
            route.setPredicates(convertToPredicateConfigList((List<?>) predicatesObj));
        }

        // filters - 转换为 List<FilterConfig>
        Object filtersObj = routeMap.get("filters");
        if (filtersObj instanceof List) {
            route.setFilters(convertToFilterConfigList((List<?>) filtersObj));
        }

        // metadata - 转换为 Map<String, Object>
        Object metadataObj = routeMap.get("metadata");
        if (metadataObj instanceof Map) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) metadataObj).entrySet()) {
                if (entry.getKey() != null) {
                    metadata.put(entry.getKey().toString(), entry.getValue());
                }
            }
            route.setMetadata(metadata);
        }

        // 默认值
        route.setStatus(RouteConstant.STATUS_ENABLE);
        route.setRouteName(route.getRouteId());

        return route;
    }

    /**
     * 将 Nacos predicates 列表转换为 List<PredicateConfig>
     */
    @SuppressWarnings("unchecked")
    private List<PredicateConfig> convertToPredicateConfigList(List<?> predicatesObj) {
        List<PredicateConfig> predicates = new ArrayList<>();
        for (Object item : predicatesObj) {
            if (item instanceof Map) {
                Map<?, ?> predMap = (Map<?, ?>) item;
                PredicateConfig config = new PredicateConfig();
                Object name = predMap.get("name");
                if (name != null) {
                    config.setName(name.toString());
                }
                Object args = predMap.get("args");
                if (args instanceof Map) {
                    Map<String, String> argsMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) args).entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            argsMap.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                    config.setArgs(argsMap);
                }
                predicates.add(config);
            }
        }
        return predicates;
    }

    /**
     * 将 Nacos filters 列表转换为 List<FilterConfig>
     */
    @SuppressWarnings("unchecked")
    private List<FilterConfig> convertToFilterConfigList(List<?> filtersObj) {
        List<FilterConfig> filters = new ArrayList<>();
        for (Object item : filtersObj) {
            if (item instanceof Map) {
                Map<?, ?> filterMap = (Map<?, ?>) item;
                FilterConfig config = new FilterConfig();
                Object name = filterMap.get("name");
                if (name != null) {
                    config.setName(name.toString());
                }
                Object args = filterMap.get("args");
                if (args instanceof Map) {
                    Map<String, String> argsMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) args).entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            argsMap.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                    config.setArgs(argsMap);
                }
                filters.add(config);
            }
        }
        return filters;
    }

    @Override
    public ResponseDTO<RouteDiffRsp> getRouteDiff(RouteDiffReq req) {
        // 1. 查询仓库路由（启用状态的路由）
        List<GaRouteDO> repositoryRoutes = gaRouteMapper.selectList(
            new LambdaQueryWrapper<GaRouteDO>()
                .eq(GaRouteDO::getRoutesGroup, req.getRoutesGroup())
                .eq(GaRouteDO::getStatus, RouteConstant.STATUS_ENABLE)
        );

        // 2. 从存储获取路由
        String storageMode = getStorageModeByGroupKey(req.getRoutesGroup());
        List<GaRouteDO> storageRoutes = getRoutesFromStorage(req.getRoutesGroup(), storageMode);

        // 3. 对比差异
        Map<String, GaRouteDO> repoRouteMap = repositoryRoutes.stream()
            .collect(Collectors.toMap(GaRouteDO::getRouteId, r -> r, (a, b) -> a));
        Map<String, GaRouteDO> storageRouteMap = storageRoutes.stream()
            .collect(Collectors.toMap(GaRouteDO::getRouteId, r -> r, (a, b) -> a));

        List<RouteDiffItem> diffDetails = new ArrayList<>();
        DiffStats diffStats = new DiffStats();
        int addedCount = 0, modifiedCount = 0, deletedCount = 0, unchangedCount = 0;

        // 仓库路由遍历：检查新增、修改、不变
        for (GaRouteDO repoRoute : repositoryRoutes) {
            RouteDiffItem item = new RouteDiffItem();
            item.setRouteId(repoRoute.getRouteId());
            item.setRepositoryRoute(repoRoute);

            GaRouteDO storageRoute = storageRouteMap.get(repoRoute.getRouteId());
            if (ObjectUtil.isNull(storageRoute)) {
                // 新增：仓库有但存储没有
                item.setDiffType("added");
                addedCount++;
            } else {
                item.setInstanceRoute(storageRoute);
                // 对比内容
                List<FieldDiff> fieldDiffs = compareRouteFields(repoRoute, storageRoute);
                if (CollUtil.isNotEmpty(fieldDiffs)) {
                    // 修改：内容不同
                    item.setDiffType("modified");
                    item.setFieldDiffs(fieldDiffs);
                    modifiedCount++;
                } else {
                    // 不变
                    item.setDiffType("unchanged");
                    unchangedCount++;
                }
            }
            diffDetails.add(item);
        }

        // 存储路由遍历：检查删除（存储有但仓库没有）
        for (GaRouteDO storageRoute : storageRoutes) {
            if (!repoRouteMap.containsKey(storageRoute.getRouteId())) {
                RouteDiffItem item = new RouteDiffItem();
                item.setRouteId(storageRoute.getRouteId());
                item.setDiffType("deleted");
                item.setInstanceRoute(storageRoute);
                diffDetails.add(item);
                deletedCount++;
            }
        }

        // 4. 设置统计结果
        diffStats.setAddedCount(addedCount);
        diffStats.setModifiedCount(modifiedCount);
        diffStats.setDeletedCount(deletedCount);
        diffStats.setUnchangedCount(unchangedCount);

        RouteDiffRsp rsp = new RouteDiffRsp();
        rsp.setRepositoryRoutes(repositoryRoutes);
        rsp.setRepositoryCount(repositoryRoutes.size());
        rsp.setInstanceRoutes(storageRoutes);
        rsp.setInstanceCount(storageRoutes.size());
        rsp.setDiffStats(diffStats);
        rsp.setDiffDetails(diffDetails);

        log.info("[Route] 路由差异对比完成 | routesGroup: {}, storageMode: {}, added: {}, modified: {}, deleted: {}, unchanged: {}",
            req.getRoutesGroup(), storageMode, addedCount, modifiedCount, deletedCount, unchangedCount);

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 对比两个路由的字段差异
     */
    private List<FieldDiff> compareRouteFields(GaRouteDO repoRoute, GaRouteDO instRoute) {
        List<FieldDiff> diffs = new ArrayList<>();

        // 对比 URI
        if (!StrUtil.equals(repoRoute.getUri(), instRoute.getUri())) {
            FieldDiff diff = new FieldDiff();
            diff.setFieldName("uri");
            diff.setOldValue(instRoute.getUri());
            diff.setNewValue(repoRoute.getUri());
            diffs.add(diff);
        }

        // 对比 orderNum
        if (!ObjectUtil.equals(repoRoute.getOrderNum(), instRoute.getOrderNum())) {
            FieldDiff diff = new FieldDiff();
            diff.setFieldName("orderNum");
            diff.setOldValue(String.valueOf(instRoute.getOrderNum()));
            diff.setNewValue(String.valueOf(repoRoute.getOrderNum()));
            diffs.add(diff);
        }

        // 对比 predicates（JSON 序列化对比）
        String repoPredicatesJson = JacksonUtil.toJson(repoRoute.getPredicates());
        String instPredicatesJson = JacksonUtil.toJson(instRoute.getPredicates());
        if (!StrUtil.equals(repoPredicatesJson, instPredicatesJson)) {
            FieldDiff diff = new FieldDiff();
            diff.setFieldName("predicates");
            diff.setOldValue(instPredicatesJson);
            diff.setNewValue(repoPredicatesJson);
            diffs.add(diff);
        }

        // 对比 filters
        String repoFiltersJson = JacksonUtil.toJson(repoRoute.getFilters());
        String instFiltersJson = JacksonUtil.toJson(instRoute.getFilters());
        if (!StrUtil.equals(repoFiltersJson, instFiltersJson)) {
            FieldDiff diff = new FieldDiff();
            diff.setFieldName("filters");
            diff.setOldValue(instFiltersJson);
            diff.setNewValue(repoFiltersJson);
            diffs.add(diff);
        }

        // 对比 metadata
        String repoMetadataJson = JacksonUtil.toJson(repoRoute.getMetadata());
        String instMetadataJson = JacksonUtil.toJson(instRoute.getMetadata());
        if (!StrUtil.equals(repoMetadataJson, instMetadataJson)) {
            FieldDiff diff = new FieldDiff();
            diff.setFieldName("metadata");
            diff.setOldValue(instMetadataJson);
            diff.setNewValue(repoMetadataJson);
            diffs.add(diff);
        }

        return diffs;
    }

    @Override
    public ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(GetGroupInstanceRoutesReq req) {
        // 参数校验
        if (StrUtil.isBlank(req.getRoutesGroup())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        GroupInstanceRoutesRsp rsp = new GroupInstanceRoutesRsp();
        rsp.setTimestamp(LocalDateTime.now());
        rsp.setFromActuator(false);

        String routesGroup = req.getRoutesGroup();
        // 从路由分组获取存储方式
        String storageMode = getStorageModeByGroupKey(routesGroup);

        rsp.setRoutesGroup(routesGroup);
        rsp.setStorageMode(storageMode);

        log.info("[Route] 从存储获取路由 | routesGroup: {}, storageMode: {}", routesGroup, storageMode);

        try {
            // 从存储获取路由
            List<GaRouteDO> routes = getRoutesFromStorage(routesGroup, storageMode);

            rsp.setRows(routes);
            rsp.setTotal(routes.size());

            log.info("[Route] 成功获取存储路由 | routesGroup: {}, storageMode: {}, count: {}",
                routesGroup, storageMode, rsp.getTotal());

        } catch (Exception e) {
            log.error("[Route] 获取存储路由异常 | routesGroup: {}, error: {}", routesGroup, e.getMessage(), e);
            rsp.setRows(new ArrayList<>());
            rsp.setTotal(0);
            rsp.setError("获取存储路由异常: " + e.getMessage());
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 根据分组标识获取存储方式
     * 优先从指定的路由分组获取，如果分组不存在则使用默认分组
     *
     * @param groupKey 分组标识
     * @return 存储方式：nacos/redis
     */
    private String getStorageModeByGroupKey(String groupKey) {
        // 尝试从路由分组获取
        LambdaQueryWrapper<GatewayRouteGroupDO> groupQuery = new LambdaQueryWrapper<>();
        groupQuery.eq(GatewayRouteGroupDO::getGroupKey, groupKey);
        GatewayRouteGroupDO group = gatewayRouteGroupMapper.selectOne(groupQuery);

        if (group != null && StrUtil.isNotBlank(group.getStorageMode())) {
            return group.getStorageMode();
        }

        // 使用默认分组的存储方式
        LambdaQueryWrapper<GatewayRouteGroupDO> defaultQuery = new LambdaQueryWrapper<>();
        defaultQuery.eq(GatewayRouteGroupDO::getGroupKey, "default");
        GatewayRouteGroupDO defaultGroup = gatewayRouteGroupMapper.selectOne(defaultQuery);

        if (defaultGroup != null && StrUtil.isNotBlank(defaultGroup.getStorageMode())) {
            return defaultGroup.getStorageMode();
        }

        // 最终兜底：返回 nacos
        return RouteConstant.STORAGE_MODE_NACOS;
    }
}