package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.constants.ServiceConstant;
import com.blink.gateway.admin.dto.req.ConfirmPushReq;
import com.blink.gateway.admin.dto.req.FullPushRoutesReq;
import com.blink.gateway.admin.dto.req.GetInstanceRoutesFromActuatorReq;
import com.blink.gateway.admin.dto.req.GetLatestPushReq;
import com.blink.gateway.admin.dto.req.PushRoutesReq;
import com.blink.gateway.admin.dto.req.GetLatestPushReq;
import com.blink.gateway.admin.dto.req.QueryInstancePushHistoryReq;
import com.blink.gateway.admin.dto.req.QueryInstanceRoutesReq;
import com.blink.gateway.admin.dto.req.QueryPushLogReq;
import com.blink.gateway.admin.dto.req.QueryRouteInstancePushStatusReq;
import com.blink.gateway.admin.dto.req.RollbackPushReq;
import com.blink.gateway.admin.dto.req.VerifyPushResultReq;
import com.blink.gateway.admin.dto.rsp.InstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceRoutesRsp;
import com.blink.gateway.admin.dto.rsp.QueryPushLogRsp;
import com.blink.gateway.admin.dto.rsp.RouteInstancePushStatusRsp;
import com.blink.gateway.admin.dto.rsp.VerifyPushResultRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import com.blink.gateway.admin.entity.GaRouteInstanceRelaDO;
import com.blink.gateway.admin.entity.FilterConfig;
import com.blink.gateway.admin.entity.PredicateConfig;
import com.blink.gateway.admin.mapper.GaRouteMapper;
import com.blink.gateway.admin.mapper.GaRoutePushLogMapper;
import com.blink.gateway.admin.mapper.GaRouteInstanceRelaMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.service.RoutePushService;
import com.blink.gateway.admin.component.NacosConfigComponent;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.Yaml;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PARAMETER_NOT_NULL;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PUSH_LOG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_CONFIG_NOT_FOUND;
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
    private GaRouteInstanceRelaMapper gaRouteInstanceRelaMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private GatewayInstanceService gatewayInstanceService;

    /**
     * Nacos 配置组件（可选注入）
     */
    private NacosConfigComponent nacosConfigComponent;

    /**
     * WebClient 用于调用网关实例 Actuator 端点
     */
    private final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();

    /**
     * Actuator 认证用户名
     */
    @Value("${blink.gateway.actuator.username:admin}")
    private String actuatorUsername;

    /**
     * Actuator 认证密码
     */
    @Value("${blink.gateway.actuator.password:123456}")
    private String actuatorPassword;

    @Value("${spring.cloud.nacos.discovery.namespace:public}")
    private String nacosNamespace;

    @Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}")
    private String nacosGroup;

    @Autowired(required = false)
    public void setNacosConfigComponent(NacosConfigComponent nacosConfigComponent) {
        this.nacosConfigComponent = nacosConfigComponent;
    }

    @Override
    public ResponseDTO<EmptyBody> pushRoutes(PushRoutesReq req) {
        // 参数校验
        if (CollUtil.isEmpty(req.getRouteIds())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(req.getPushMode())) {
            req.setPushMode(RouteConstant.PUSH_MODE_BROADCAST);
        }

        // 查询要推送的路由
        List<GaRouteDO> routes = gaRouteMapper.selectByIds(req.getRouteIds());
        if (CollUtil.isEmpty(routes)) {
            BlinkException.throwBusinessException(ROUTE_NOT_EXIST);
        }

        // 获取操作人信息
        Integer operatorUser = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        // 获取目标实例列表
        List<String> targetInstanceIds = new ArrayList<>();
        if (RouteConstant.PUSH_MODE_SPECIFIED.equals(req.getPushMode())
            && CollUtil.isNotEmpty(req.getTargetInstanceIds())) {
            targetInstanceIds = req.getTargetInstanceIds();
        } else {
            // 广播模式：获取所有在线实例
            ResponseDTO<GatewayInstanceListRsp> instancesRsp = gatewayInstanceService.getGatewayInstances();
            if (instancesRsp.getBody() != null && instancesRsp.getBody().getInstances() != null) {
                targetInstanceIds = instancesRsp.getBody().getInstances().stream()
                    .filter(inst -> inst.getStatus().equals(INSTANCE_STATUS_ONLINE))
                    .map(GatewayInstanceVO::getInstanceId)
                    .toList();
            }
        }

        // 如果 storageMode 为空，从实例配置获取
        if (StrUtil.isBlank(req.getStorageMode())) {
            if (CollUtil.isEmpty(targetInstanceIds)) {
                log.warn("[RoutePush] 无法确定路由模式：无目标实例 | routeIds: {}", req.getRouteIds());
                BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
            }
            // 从第一个目标实例的配置获取路由模式
            InstanceRouteConfig config = getStorageModeFromInstanceConfig(targetInstanceIds.get(0));
            req.setStorageMode(config.getStorageMode());
            req.setRoutesGroup(config.getRoutesGroup());
            req.setNacosDataId(config.getNacosDataId());
            req.setNacosGroup(config.getNacosGroup());
            log.info("[RoutePush] 从实例配置获取路由模式 | instanceId: {}, storageMode: {}, routesGroup: {}",
                targetInstanceIds.get(0), req.getStorageMode(), req.getRoutesGroup());
        }

        // 构建推送记录
        GaRoutePushLogDO pushLog = new GaRoutePushLogDO();
        pushLog.setStorageMode(req.getStorageMode());
        pushLog.setRouteIds(JacksonUtil.toJson(req.getRouteIds()));
        pushLog.setRouteSnapshot(routes);
        pushLog.setPushMode(req.getPushMode());
        pushLog.setOperatorId(operatorUser);
        pushLog.setOperatorName(operatorName);
        pushLog.setRemark(req.getRemark());
        pushLog.setTargetInstanceIds(JacksonUtil.toJson(targetInstanceIds));
        pushLog.setInstanceCount(targetInstanceIds.size());

        // 设置存储方式相关参数并执行推送
        if (RouteConstant.STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            String routesGroup = StrUtil.isBlank(req.getRoutesGroup())
                ? RouteConstant.DEFAULT_ROUTES_GROUP : req.getRoutesGroup();
            pushLog.setRoutesGroup(routesGroup);

            // 推送到 Redis Hash
            pushRoutesToRedis(routes, routesGroup);
        } else if (RouteConstant.STORAGE_MODE_NACOS.equals(req.getStorageMode())) {
            String nacosDataId = StrUtil.isBlank(req.getNacosDataId())
                ? RouteConstant.DEFAULT_NACOS_DATA_ID : req.getNacosDataId();
            String nacosGroup = StrUtil.isBlank(req.getNacosGroup())
                ? RouteConstant.DEFAULT_NACOS_GROUP : req.getNacosGroup();
            pushLog.setNacosDataId(nacosDataId);
            pushLog.setNacosGroup(nacosGroup);

            // 推送到 Nacos 配置文件
            pushRoutesToNacos(routes, nacosDataId, nacosGroup);
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

        // 记录实例级推送状态
        saveRouteInstanceRela(req.getRouteIds(), targetInstanceIds, pushLog.getPushId(),
            pushLog.getPushResult() == RouteConstant.PUSH_RESULT_SUCCESS);

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
        List<GaRouteDO> routes = new ArrayList<>();

        // 确定存储模式：优先使用请求参数，否则默认使用 nacos
        String storageMode = StrUtil.isBlank(req.getStorageMode())
            ? RouteConstant.STORAGE_MODE_NACOS : req.getStorageMode();

        if (RouteConstant.STORAGE_MODE_NACOS.equals(storageMode)) {
            // 从 Nacos 配置文件查询
            routes = getRoutesFromNacos(req.getNacosDataId(), req.getNacosGroup());
        } else if (RouteConstant.STORAGE_MODE_REDIS.equals(storageMode)) {
            // 从 Redis Hash 查询
            routes = getRoutesFromRedis(req.getRoutesGroup());
        }

        rsp.setRows(routes);
        rsp.setTotal(routes.size());

        log.info("[RoutePush] 查询实例路由成功 | storageMode: {}, count: {}", storageMode, routes.size());

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 从 Nacos 配置文件获取路由
     * Nacos 存储的是 Spring Cloud Gateway 的 RouteDefinition 格式
     */
    private List<GaRouteDO> getRoutesFromNacos(String dataId, String group) {
        List<GaRouteDO> routes = new ArrayList<>();

        if (nacosConfigComponent == null) {
            log.warn("[RoutePush] NacosConfigComponent 未注入，无法查询 Nacos 路由");
            return routes;
        }

        // 使用默认值
        String nacosDataId = StrUtil.isBlank(dataId)
            ? RouteConstant.DEFAULT_NACOS_DATA_ID : dataId;
        String nacosGroup = StrUtil.isBlank(group)
            ? RouteConstant.DEFAULT_NACOS_GROUP : group;

        try {
            String configContent = nacosConfigComponent.getConfig(nacosDataId, nacosGroup);
            if (StrUtil.isNotBlank(configContent)) {
                // Nacos 配置文件是 JSON 数组格式，使用 Map 解析以兼容不同字段名
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
            log.error("[RoutePush] 从 Nacos 获取路由配置失败 | dataId: {}, group: {}", nacosDataId, nacosGroup, e);
        }

        return routes;
    }

    /**
     * 将 Nacos 路由格式转换为 GaRouteDO
     * Nacos 格式：id, uri, predicates, filters, order, metadata
     * 数据库格式：routeId, routeName, uri, predicates, filters, orderNum, status
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
            List<PredicateConfig> predicates = convertToPredicateConfigList((List<?>) predicatesObj);
            route.setPredicates(predicates);
        }

        // filters - 转换为 List<FilterConfig>
        Object filtersObj = routeMap.get("filters");
        if (filtersObj instanceof List) {
            List<FilterConfig> filters = convertToFilterConfigList((List<?>) filtersObj);
            route.setFilters(filters);
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

    /**
     * 从 Redis Hash 获取路由
     */
    private List<GaRouteDO> getRoutesFromRedis(String routesGroup) {
        List<GaRouteDO> routes = new ArrayList<>();

        String group = StrUtil.isBlank(routesGroup)
            ? RouteConstant.DEFAULT_ROUTES_GROUP : routesGroup;
        String redisKey = GATEWAY_DYNAMIC_ROUTES + ":" + group;

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

        return routes;
    }

    /**
     * 推送路由到 Redis Hash
     *
     * @param routes 路由列表
     * @param routesGroup 路由分组
     */
    private void pushRoutesToRedis(List<GaRouteDO> routes, String routesGroup) {
        String redisKey = GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup;

        for (GaRouteDO route : routes) {
            String routeJson = JacksonUtil.toJson(route);
            redisClient.hPutField(redisKey, route.getRouteId(), routeJson);
        }

        log.info("[RoutePush] 推送路由到 Redis 成功 | redisKey: {}, count: {}", redisKey, routes.size());
    }

    /**
     * 推送路由到 Nacos 配置文件
     *
     * @param routes 路由列表
     * @param dataId Nacos Data ID
     * @param group Nacos Group
     */
    private void pushRoutesToNacos(List<GaRouteDO> routes, String dataId, String group) {
        if (nacosConfigComponent == null) {
            log.warn("[RoutePush] NacosConfigComponent 未注入，无法推送到 Nacos");
            BlinkException.throwBusinessException("Nacos 配置组件未启用");
        }

        try {
            // 获取当前 Nacos 中的路由配置
            String currentConfig = nacosConfigComponent.getConfig(dataId, group);
            List<GaRouteDO> existingRoutes = new ArrayList<>();

            if (StrUtil.isNotBlank(currentConfig)) {
                existingRoutes = JacksonUtil.fromJsonToList(currentConfig, GaRouteDO.class);
                if (existingRoutes == null) {
                    existingRoutes = new ArrayList<>();
                }
            }

            // 合并路由：按 routeId 去重更新
            Map<String, GaRouteDO> routeMap = existingRoutes.stream()
                .collect(java.util.stream.Collectors.toMap(
                    GaRouteDO::getRouteId,
                    r -> r,
                    (a, b) -> a
                ));

            // 更新或新增路由
            for (GaRouteDO route : routes) {
                routeMap.put(route.getRouteId(), route);
            }

            // 发布配置到 Nacos
            List<GaRouteDO> mergedRoutes = new ArrayList<>(routeMap.values());
            String newConfigContent = JacksonUtil.toJson(mergedRoutes);
            nacosConfigComponent.configPublisher(dataId, group, newConfigContent);

            log.info("[RoutePush] 推送路由到 Nacos 成功 | dataId: {}, group: {}, count: {}",
                dataId, group, mergedRoutes.size());
        } catch (Exception e) {
            log.error("[RoutePush] 推送路由到 Nacos 失败 | dataId: {}, group: {}", dataId, group, e);
            throw new BlinkException("推送到 Nacos 失败: " + e.getMessage(), e, "NACOS_PUSH_FAILED");
        }
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

    @Override
    public ResponseDTO<QueryPushLogRsp> getInstancePushHistory(QueryInstancePushHistoryReq req) {
        QueryPushLogRsp rsp = new QueryPushLogRsp();

        // 参数校验
        if (StrUtil.isBlank(req.getInstanceId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询推送历史，筛选包含该实例的记录
        // targetInstanceIds 和 failedInstanceIds 都是 JSON 数组格式
        LambdaQueryWrapper<GaRoutePushLogDO> queryWrapper = new LambdaQueryWrapper<GaRoutePushLogDO>()
            .and(wrapper -> wrapper
                // 广播模式（targetInstanceIds 为空或 null）所有实例都相关
                .or(w -> w.isNull(GaRoutePushLogDO::getTargetInstanceIds)
                    .or()
                    .eq(GaRoutePushLogDO::getTargetInstanceIds, "")
                    .or()
                    .like(GaRoutePushLogDO::getTargetInstanceIds, req.getInstanceId()))
                // 指定模式：targetInstanceIds 包含该实例
                .or(w -> w.like(GaRoutePushLogDO::getTargetInstanceIds, req.getInstanceId()))
                // 失败实例中包含该实例
                .or(w -> w.like(GaRoutePushLogDO::getFailedInstanceIds, req.getInstanceId()))
            )
            .orderByDesc(GaRoutePushLogDO::getPushTime);

        PageUtils.queryPage(req, () -> gaRoutePushLogMapper.selectList(queryWrapper), rsp);

        log.info("[RoutePush] 查询实例推送历史成功 | instanceId: {}, count: {}", req.getInstanceId(), rsp.getTotal());

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<InstanceRoutesRsp> getInstanceRoutesFromActuator(GetInstanceRoutesFromActuatorReq req) {
        // 参数校验
        if (StrUtil.isBlank(req.getInstanceId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        InstanceRoutesRsp rsp = new InstanceRoutesRsp();
        rsp.setInstanceId(req.getInstanceId());
        rsp.setFromActuator(true);

        // 解析实例ID获取 host:port
        // 格式：gateway-app:host:port
        String[] parts = req.getInstanceId().split(":");
        if (parts.length < 3) {
            log.warn("[RoutePush] 实例ID格式错误 | instanceId: {}", req.getInstanceId());
            rsp.setError("实例ID格式错误");
            rsp.setRows(new ArrayList<>());
            rsp.setTotal(0);
            return ResponseDTO.newSuccessInstance(rsp);
        }

        String host = parts[1];
        String port = parts[2];

        // 调用网关实例 Actuator 端点
        try {
            String actuatorUrl = String.format("http://%s:%s/actuator/gateway-routes", host, port);

            log.info("[RoutePush] 调用网关实例 Actuator 端点 | url: {}", actuatorUrl);

            // 使用 WebClient 调用（带 Basic 认证）
            String response = webClient.get()
                .uri(actuatorUrl)
                .headers(headers -> headers.setBasicAuth(actuatorUsername, actuatorPassword))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

            // 解析响应
            if (StrUtil.isNotBlank(response)) {
                Map<String, Object> responseMap = JacksonUtil.fromJson(response,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routesList = (List<Map<String, Object>>) responseMap.get("routes");
                List<GaRouteDO> routes = convertRouteMapListToGaRouteDOList(routesList);

                rsp.setRows(routes);
                rsp.setTotal(routes.size());
                rsp.setTimestamp(LocalDateTime.now());
            } else {
                rsp.setRows(new ArrayList<>());
                rsp.setTotal(0);
            }

            log.info("[RoutePush] 从实例获取路由成功 | instanceId: {}, count: {}",
                req.getInstanceId(), rsp.getTotal());

        } catch (Exception e) {
            log.error("[RoutePush] 从实例获取路由失败 | instanceId: {}, error: {}",
                req.getInstanceId(), e.getMessage(), e);

            // 根据异常类型提供更友好的错误信息
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Connection refused") || errorMessage.contains("connect timed out")) {
                errorMessage = "实例已离线或网络不可达";
            } else if (errorMessage.contains("401") || errorMessage.contains("Unauthorized")) {
                errorMessage = "Actuator 认证失败";
            } else if (errorMessage.contains("Timeout")) {
                errorMessage = "获取路由超时";
            }

            rsp.setError("获取失败：" + errorMessage);
            rsp.setRows(new ArrayList<>());
            rsp.setTotal(0);
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 将路由 Map 列表转换为 GaRouteDO 列表
     *
     * @param routesList 路由 Map 列表
     * @return GaRouteDO 列表
     */
    @SuppressWarnings("unchecked")
    private List<GaRouteDO> convertRouteMapListToGaRouteDOList(List<Map<String, Object>> routesList) {
        List<GaRouteDO> routes = new ArrayList<>();

        if (routesList == null) {
            return routes;
        }

        for (Map<String, Object> routeMap : routesList) {
            GaRouteDO route = convertNacosRouteToGaRouteDO(routeMap);
            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    /**
     * 保存路由实例关联记录
     * 记录每个路由在每个实例上的推送状态
     *
     * @param routeIds 路由ID列表
     * @param instanceIds 实例ID列表
     * @param pushId 推送记录ID
     * @param success 是否推送成功
     */
    private void saveRouteInstanceRela(List<String> routeIds, List<String> instanceIds,
            Long pushId, boolean success) {
        if (CollUtil.isEmpty(routeIds) || CollUtil.isEmpty(instanceIds)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Byte pushStatus = success ? RouteConstant.PUSH_STATUS_PUSHED : RouteConstant.PUSH_STATUS_PUSH_FAILED;

        for (String routeId : routeIds) {
            for (String instanceId : instanceIds) {
                // 检查是否已存在记录
                LambdaQueryWrapper<GaRouteInstanceRelaDO> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(GaRouteInstanceRelaDO::getRouteId, routeId)
                    .eq(GaRouteInstanceRelaDO::getInstanceId, instanceId);

                GaRouteInstanceRelaDO existingRela = gaRouteInstanceRelaMapper.selectOne(queryWrapper);

                if (existingRela != null) {
                    // 更新现有记录
                    existingRela.setPushId(pushId);
                    existingRela.setPushStatus(pushStatus);
                    existingRela.setPushTime(now);
                    gaRouteInstanceRelaMapper.updateById(existingRela);
                } else {
                    // 插入新记录
                    GaRouteInstanceRelaDO rela = new GaRouteInstanceRelaDO();
                    rela.setRouteId(routeId);
                    rela.setInstanceId(instanceId);
                    rela.setPushId(pushId);
                    rela.setPushStatus(pushStatus);
                    rela.setPushTime(now);
                    gaRouteInstanceRelaMapper.insert(rela);
                }
            }
        }

        log.info("[RoutePush] 保存路由实例关联 | routeCount: {}, instanceCount: {}, pushId: {}, success: {}",
            routeIds.size(), instanceIds.size(), pushId, success);
    }

    @Override
    public ResponseDTO<List<RouteInstancePushStatusRsp>> getRouteInstancePushStatus(QueryRouteInstancePushStatusReq req) {
        List<RouteInstancePushStatusRsp> result = new ArrayList<>();

        // 获取要查询的路由ID列表
        List<String> routeIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(req.getRouteIds())) {
            routeIds = req.getRouteIds();
        } else if (StrUtil.isNotBlank(req.getRouteId())) {
            routeIds.add(req.getRouteId());
        }

        if (CollUtil.isEmpty(routeIds)) {
            return ResponseDTO.newSuccessInstance(result);
        }

        // 获取所有在线实例
        ResponseDTO<GatewayInstanceListRsp> instancesRsp = gatewayInstanceService.getGatewayInstances();
        List<String> allInstanceIds = new ArrayList<>();
        if (instancesRsp.getBody() != null && instancesRsp.getBody().getInstances() != null) {
            allInstanceIds = instancesRsp.getBody().getInstances().stream()
                .filter(inst -> inst.getStatus().equals(INSTANCE_STATUS_ONLINE))
                .map(GatewayInstanceVO::getInstanceId)
                .toList();
        }

        // 查询每个路由的实例推送状态
        for (String routeId : routeIds) {
            RouteInstancePushStatusRsp statusRsp = new RouteInstancePushStatusRsp();
            statusRsp.setRouteId(routeId);

            // 查询该路由的所有实例关联记录
            List<GaRouteInstanceRelaDO> relaList = gaRouteInstanceRelaMapper.selectByRouteId(routeId);

            // 统计各状态数量
            int pushedCount = 0;
            int failedCount = 0;
            int notPushedCount = allInstanceIds.size() - relaList.size();

            List<RouteInstancePushStatusRsp.InstancePushDetail> details = new ArrayList<>();

            for (GaRouteInstanceRelaDO rela : relaList) {
                if (RouteConstant.PUSH_STATUS_PUSHED.equals(rela.getPushStatus())) {
                    pushedCount++;
                } else if (RouteConstant.PUSH_STATUS_PUSH_FAILED.equals(rela.getPushStatus())) {
                    failedCount++;
                }

                // 构建详情
                RouteInstancePushStatusRsp.InstancePushDetail detail = new RouteInstancePushStatusRsp.InstancePushDetail();
                detail.setInstanceId(rela.getInstanceId());
                detail.setPushStatus(rela.getPushStatus());
                detail.setPushStatusDesc(getPushStatusDesc(rela.getPushStatus()));
                detail.setPushTime(rela.getPushTime() != null ? rela.getPushTime().toString() : null);
                detail.setErrorMsg(rela.getErrorMsg());
                details.add(detail);
            }

            statusRsp.setTotalInstances(allInstanceIds.size());
            statusRsp.setPushedInstances(pushedCount);
            statusRsp.setFailedInstances(failedCount);
            statusRsp.setNotPushedInstances(notPushedCount);
            statusRsp.setInstanceDetails(details);

            // 生成状态描述
            statusRsp.setStatusDesc(buildStatusDesc(pushedCount, failedCount, notPushedCount, allInstanceIds.size()));

            result.add(statusRsp);
        }

        log.info("[RoutePush] 查询路由实例推送状态 | routeCount: {}", result.size());

        return ResponseDTO.newSuccessInstance(result);
    }

    /**
     * 获取推送状态描述
     */
    private String getPushStatusDesc(Byte pushStatus) {
        if (pushStatus == null) {
            return RouteConstant.PUSH_STATUS_DESC_UNKNOWN;
        }
        if (RouteConstant.PUSH_STATUS_PUSHED.equals(pushStatus)) {
            return RouteConstant.PUSH_STATUS_DESC_PUSHED;
        }
        if (RouteConstant.PUSH_STATUS_PUSH_FAILED.equals(pushStatus)) {
            return RouteConstant.PUSH_STATUS_DESC_PUSH_FAILED;
        }
        return RouteConstant.PUSH_STATUS_DESC_NOT_PUSHED;
    }

    /**
     * 构建状态描述
     * 格式：已推送(3/5)、推送失败(1/5)、部分成功(3/5) 等
     */
    private String buildStatusDesc(int pushedCount, int failedCount, int notPushedCount, int total) {
        if (total == 0) {
            return "无实例";
        }
        if (pushedCount == total) {
            return "已推送(" + pushedCount + "/" + total + ")";
        }
        if (failedCount == total) {
            return "推送失败(" + failedCount + "/" + total + ")";
        }
        if (notPushedCount == total) {
            return "未推送";
        }
        // 部分成功情况
        if (failedCount > 0 && pushedCount > 0) {
            return "部分成功(" + pushedCount + "/" + total + ")";
        }
        if (notPushedCount > 0 && pushedCount > 0) {
            return "已推送(" + pushedCount + "/" + total + ")";
        }
        return "已推送(" + pushedCount + "/" + total + ")";
    }

    @Override
    public ResponseDTO<EmptyBody> confirmPush(ConfirmPushReq req) {
        // 参数校验
        if (req.getPushId() == null) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询推送记录
        GaRoutePushLogDO pushLog = gaRoutePushLogMapper.selectById(req.getPushId());
        if (pushLog == null) {
            BlinkException.throwBusinessException(PUSH_LOG_NOT_EXIST);
        }

        // 检查是否已确认
        if (RouteConstant.CONFIRM_STATUS_CONFIRMED.equals(pushLog.getConfirmStatus())) {
            log.warn("[RoutePush] 推送已确认，无需重复操作 | pushId: {}", req.getPushId());
            return ResponseDTO.newSuccessInstance();
        }

        // 更新确认状态
        String operatorName = StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null;

        LambdaUpdateWrapper<GaRoutePushLogDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GaRoutePushLogDO::getPushId, req.getPushId())
            .set(GaRoutePushLogDO::getConfirmStatus, RouteConstant.CONFIRM_STATUS_CONFIRMED)
            .set(GaRoutePushLogDO::getConfirmTime, LocalDateTime.now())
            .set(GaRoutePushLogDO::getConfirmBy, operatorName);

        gaRoutePushLogMapper.update(null, updateWrapper);

        log.info("[RoutePush] 确认推送成功 | pushId: {}, operator: {}", req.getPushId(), operatorName);

        return ResponseDTO.newSuccessInstance();
    }

    // ==================== 推送结果验证 ====================

    @Override
    public ResponseDTO<VerifyPushResultRsp> verifyPushResult(VerifyPushResultReq req) {
        if (req.getPushId() == null) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询推送记录
        GaRoutePushLogDO pushLog = gaRoutePushLogMapper.selectById(req.getPushId());
        if (pushLog == null) {
            BlinkException.throwBusinessException(PUSH_LOG_NOT_EXIST);
        }

        VerifyPushResultRsp rsp = new VerifyPushResultRsp();
        rsp.setPushId(req.getPushId());

        // 解析目标实例
        List<String> targetInstanceIds = new ArrayList<>();
        if (StrUtil.isNotBlank(pushLog.getTargetInstanceIds())) {
            try {
                targetInstanceIds = JacksonUtil.fromJson(pushLog.getTargetInstanceIds(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("[RoutePush] 解析目标实例列表失败 | pushId: {}", req.getPushId());
            }
        }

        // 如果指定了实例ID，只验证该实例
        if (StrUtil.isNotBlank(req.getInstanceId())) {
            targetInstanceIds = List.of(req.getInstanceId());
        }

        // 解析路由快照
        List<GaRouteDO> routeSnapshot = pushLog.getRouteSnapshot();
        if (routeSnapshot == null || routeSnapshot.isEmpty()) {
            rsp.setConsistent(false);
            rsp.setErrorMessage("推送记录无路由快照");
            return ResponseDTO.newSuccessInstance(rsp);
        }

        // 验证每个实例的路由配置
        List<VerifyPushResultRsp.InstanceVerifyResult> results = new ArrayList<>();
        boolean allConsistent = true;

        for (String instanceId : targetInstanceIds) {
            VerifyPushResultRsp.InstanceVerifyResult result = new VerifyPushResultRsp.InstanceVerifyResult();
            result.setInstanceId(instanceId);

            try {
                // 从实例获取实际路由
                InstanceRoutesRsp instanceRoutesRsp = getInstanceRoutesFromActuatorInternal(instanceId);
                if (StrUtil.isNotBlank(instanceRoutesRsp.getError())) {
                    result.setConsistent(false);
                    result.setErrorMessage(instanceRoutesRsp.getError());
                    allConsistent = false;
                } else if (CollUtil.isEmpty(instanceRoutesRsp.getRows())) {
                    // 空路由列表处理
                    result.setConsistent(false);
                    result.setErrorMessage("实例路由列表为空");
                    allConsistent = false;
                } else {
                    // 比对路由数量
                    List<GaRouteDO> actualRoutes = instanceRoutesRsp.getRows();
                    Set<String> expectedRouteIds = routeSnapshot.stream()
                        .map(GaRouteDO::getRouteId)
                        .collect(Collectors.toSet());
                    Set<String> actualRouteIds = actualRoutes.stream()
                        .map(GaRouteDO::getRouteId)
                        .collect(Collectors.toSet());

                    if (expectedRouteIds.equals(actualRouteIds)) {
                        result.setConsistent(true);
                        result.setRouteCount(actualRoutes.size());
                    } else {
                        result.setConsistent(false);
                        result.setExpectedCount(routeSnapshot.size());
                        result.setActualCount(actualRoutes.size());
                        result.setMissingRoutes(expectedRouteIds.stream()
                            .filter(id -> !actualRouteIds.contains(id))
                            .collect(Collectors.toList()));
                        allConsistent = false;
                    }
                }
            } catch (Exception e) {
                result.setConsistent(false);
                result.setErrorMessage("验证失败：" + e.getMessage());
                allConsistent = false;
            }

            results.add(result);
        }

        rsp.setConsistent(allConsistent);
        rsp.setInstanceResults(results);

        log.info("[RoutePush] 验证推送结果 | pushId: {}, instanceCount: {}, consistent: {}",
            req.getPushId(), results.size(), allConsistent);

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 内部方法：从实例获取路由
     *
     * @param instanceId 实例ID
     * @return 实例路由响应，获取失败时返回包含错误信息的响应
     */
    private InstanceRoutesRsp getInstanceRoutesFromActuatorInternal(String instanceId) {
        GetInstanceRoutesFromActuatorReq req = new GetInstanceRoutesFromActuatorReq();
        req.setInstanceId(instanceId);
        ResponseDTO<InstanceRoutesRsp> response = getInstanceRoutesFromActuator(req);
        return ObjectUtil.isNotNull(response) ? response.getBody() : new InstanceRoutesRsp();
    }

    @Override
    public ResponseDTO<GaRoutePushLogDO> getLatestPush(GetLatestPushReq req) {
        if (StrUtil.isBlank(req.getInstanceId())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // 查询该实例相关的最新推送记录
        LambdaQueryWrapper<GaRoutePushLogDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.apply("JSON_CONTAINS(target_instance_ids, {0})", "\"" + req.getInstanceId() + "\"")
            .orderByDesc(GaRoutePushLogDO::getPushTime)
            .last("LIMIT 1");

        GaRoutePushLogDO latestPush = gaRoutePushLogMapper.selectOne(queryWrapper);

        log.info("[RoutePush] 获取实例最新推送记录 | instanceId: {}, found: {}",
            req.getInstanceId(), latestPush != null);

        return ResponseDTO.newSuccessInstance(latestPush);
    }
}