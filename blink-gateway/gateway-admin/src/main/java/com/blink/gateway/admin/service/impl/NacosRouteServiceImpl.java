package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.RouteDefinitionReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.entity.FilterDefinitionDO;
import com.blink.gateway.admin.entity.PredicateDefinitionDO;
import com.blink.gateway.admin.entity.RouteDefinitionDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.admin.service.RouteValidator;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ConfigValueConstant.NACOS_CONFIG_TIMEOUT_MS;
import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.RouteConstant.DEFAULT_NACOS_GROUP;
import static com.blink.gateway.admin.constants.RouteConstant.PUSH_MODE_BROADCAST;
import static com.blink.gateway.admin.constants.RouteConstant.STORAGE_MODE_NACOS;
import static com.blink.gateway.admin.constants.RouteConstant.URI_PREFIX_HTTP;
import static com.blink.gateway.admin.constants.RouteConstant.URI_PREFIX_HTTPS;
import static com.blink.gateway.admin.constants.RouteConstant.URI_PREFIX_LB;

/**
 * Nacos 路由管理服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class NacosRouteServiceImpl implements NacosRouteService {

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private RouteValidator routeValidator;

    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req) {
        QueryGateWayRoutesRsp rsp = new QueryGateWayRoutesRsp();

        String dataId = req.getDataId();
        String group = req.getGroup();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = DEFAULT_NACOS_GROUP;
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String configContent = configService.getConfig(dataId, group, NACOS_CONFIG_TIMEOUT_MS);

            if (StrUtil.isBlank(configContent)) {
                log.warn("[NacosRoute] 未找到路由配置 | dataId: {}, group: {}", dataId, group);
                rsp.setTotal(0);
                rsp.setRows(new ArrayList<>());
                return ResponseDTO.newSuccessInstance(rsp);
            }

            List<RouteDefinitionDO> routes = JacksonUtil.fromJsonToList(configContent, RouteDefinitionDO.class);
            if (ObjectUtil.isNull(routes)) {
                routes = new ArrayList<>();
            }

            rsp.setTotal(routes.size());
            rsp.setRows(routes);

            log.info("[NacosRoute] 查询路由列表成功 | dataId: {}, group: {}, count: {}", dataId, group, routes.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (NacosException e) {
            log.error("[NacosRoute] 从 Nacos 获取配置失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("获取 Nacos 配置失败: " + e.getMessage(), e, GET_NACOS_CONFIG_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req) {
        String dataId = req.getDataId();
        String group = req.getGroup();
        List<RouteDefinitionReq> newRouteRequests = req.getRoutes();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = DEFAULT_NACOS_GROUP;
        }
        if (ObjectUtil.isNull(newRouteRequests) || newRouteRequests.isEmpty()) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String currentConfig = configService.getConfig(dataId, group, NACOS_CONFIG_TIMEOUT_MS);

            List<RouteDefinitionDO> existingRoutes = new ArrayList<>();
            if (StrUtil.isNotBlank(currentConfig)) {
                existingRoutes = JacksonUtil.fromJsonToList(currentConfig, RouteDefinitionDO.class);
                if (ObjectUtil.isNull(existingRoutes)) {
                    existingRoutes = new ArrayList<>();
                }
            }

            // 合合路由：按 ID 去重更新
            Map<String, RouteDefinitionDO> routeMap = existingRoutes.stream()
                    .collect(Collectors.toMap(RouteDefinitionDO::getId, Function.identity(), (a, b) -> a));

            // 将请求 DTO 转换为实体并合并
            for (RouteDefinitionReq routeReq : newRouteRequests) {
                if (StrUtil.isNotBlank(routeReq.getId())) {
                    // 校验 URI 格式
                    String uri = routeReq.getUri();
                    if (StrUtil.isBlank(uri) ||
                        (!uri.startsWith(URI_PREFIX_LB) && !uri.startsWith(URI_PREFIX_HTTP) && !uri.startsWith(URI_PREFIX_HTTPS))) {
                        log.warn("[NacosRoute] URI格式无效，跳过 | routeId: {}, uri: {}", routeReq.getId(), uri);
                        continue;
                    }

                    // 校验断言必填
                    if (StrUtil.isBlank(routeReq.getPredicates())) {
                        log.warn("[NacosRoute] 断言配置为空，跳过 | routeId: {}", routeReq.getId());
                        continue;
                    }

                    RouteDefinitionDO routeDO = convertToRouteDefinitionDO(routeReq);
                    routeMap.put(routeReq.getId(), routeDO);
                }
            }

            List<RouteDefinitionDO> mergedRoutes = new ArrayList<>(routeMap.values());
            String newConfigContent = JacksonUtil.toJson(mergedRoutes);

            configService.publishConfig(dataId, group, newConfigContent);

            // 发送同步消息
            RouteSyncMsg syncMsg = new RouteSyncMsg();
            syncMsg.setStorageMode(STORAGE_MODE_NACOS);
            syncMsg.setDataId(dataId);
            syncMsg.setGroup(group);
            syncMsg.setPushMode(PUSH_MODE_BROADCAST);
            messageProducer.routesOnChangeWithTarget(syncMsg);

            log.info("[NacosRoute] 保存路由成功 | dataId: {}, group: {}, count: {}", dataId, group, mergedRoutes.size());

            return ResponseDTO.newSuccessInstance();
        } catch (NacosException e) {
            log.error("[NacosRoute] 发布配置失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("发布 Nacos 配置失败: " + e.getMessage(), e, PUBLISH_NACOS_CONFIG_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req) {
        String dataId = req.getDataId();
        String group = req.getGroup();
        List<String> routeIds = req.getRouteIds();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = DEFAULT_NACOS_GROUP;
        }
        if (ObjectUtil.isNull(routeIds) || routeIds.isEmpty()) {
            return ResponseDTO.newSuccessInstance();
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String currentConfig = configService.getConfig(dataId, group, NACOS_CONFIG_TIMEOUT_MS);

            if (StrUtil.isBlank(currentConfig)) {
                log.warn("[NacosRoute] 配置不存在，无需删除 | dataId: {}, group: {}", dataId, group);
                return ResponseDTO.newSuccessInstance();
            }

            List<RouteDefinitionDO> existingRoutes = JacksonUtil.fromJsonToList(currentConfig, RouteDefinitionDO.class);
            if (ObjectUtil.isNull(existingRoutes)) {
                return ResponseDTO.newSuccessInstance();
            }

            // 移除指定路由
            existingRoutes.removeIf(route -> routeIds.contains(route.getId()));

            String newConfigContent = JacksonUtil.toJson(existingRoutes);
            configService.publishConfig(dataId, group, newConfigContent);

            // 发送同步消息
            RouteSyncMsg syncMsg = new RouteSyncMsg();
            syncMsg.setStorageMode(STORAGE_MODE_NACOS);
            syncMsg.setDataId(dataId);
            syncMsg.setGroup(group);
            syncMsg.setPushMode(PUSH_MODE_BROADCAST);
            messageProducer.routesOnChangeWithTarget(syncMsg);

            log.info("[NacosRoute] 删除路由成功 | dataId: {}, group: {}, deletedIds: {}", dataId, group, routeIds);

            return ResponseDTO.newSuccessInstance();
        } catch (NacosException e) {
            log.error("[NacosRoute] 删除路由失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("删除 Nacos 路由失败: " + e.getMessage(), e, DELETE_NACOS_ROUTE_FAILED);
        }
    }

    /**
     * 将请求 DTO 转换为路由定义实体
     *
     * @param req 路由定义请求 DTO
     * @return 路由定义实体
     */
    private RouteDefinitionDO convertToRouteDefinitionDO(RouteDefinitionReq req) {
        RouteDefinitionDO routeDO = new RouteDefinitionDO();
        routeDO.setId(req.getId());

        // 设置 URI
        if (StrUtil.isNotBlank(req.getUri())) {
            routeDO.setUri(URI.create(req.getUri()));
        }

        // 设置 order
        if (ObjectUtil.isNotNull(req.getOrder())) {
            routeDO.setOrder(req.getOrder());
        }

        // 解析 predicates
        if (StrUtil.isNotBlank(req.getPredicates())) {
            List<PredicateDefinitionDO> predicates = parsePredicates(req.getPredicates());
            routeDO.setPredicates(predicates);
        } else {
            routeDO.setPredicates(new ArrayList<>());
        }

        // 解析 filters
        if (StrUtil.isNotBlank(req.getFilters())) {
            List<FilterDefinitionDO> filters = parseFilters(req.getFilters());
            routeDO.setFilters(filters);
        } else {
            routeDO.setFilters(new ArrayList<>());
        }

        // 解析 metadata
        if (StrUtil.isNotBlank(req.getMetadata())) {
            Map<String, Object> metadata = JacksonUtil.fromJson(req.getMetadata(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            routeDO.setMetadata(ObjectUtil.isNotNull(metadata) ? metadata : new HashMap<>());
        } else {
            routeDO.setMetadata(new HashMap<>());
        }

        return routeDO;
    }

    /**
     * 解析断言定义字符串
     *
     * @param predicatesStr 断言定义字符串
     * @return 断言定义列表
     */
    private List<PredicateDefinitionDO> parsePredicates(String predicatesStr) {
        List<PredicateDefinitionDO> predicates = new ArrayList<>();
        if (StrUtil.isBlank(predicatesStr)) {
            return predicates;
        }

        // 支持多种格式：
        // 格式1: "Path=/api/**,Method=GET"
        // 格式2: JSON数组格式
        if (predicatesStr.startsWith("[")) {
            // JSON数组格式
            return JacksonUtil.fromJsonToList(predicatesStr, PredicateDefinitionDO.class);
        }

        // 逗号分隔的键值对格式
        String[] parts = predicatesStr.split(",");
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                predicates.add(new PredicateDefinitionDO(part.trim()));
            }
        }
        return predicates;
    }

    /**
     * 解析过滤器定义字符串
     *
     * @param filtersStr 过滤器定义字符串
     * @return 过滤器定义列表
     */
    private List<FilterDefinitionDO> parseFilters(String filtersStr) {
        List<FilterDefinitionDO> filters = new ArrayList<>();
        if (StrUtil.isBlank(filtersStr)) {
            return filters;
        }

        // 支持多种格式：
        // 格式1: "StripPrefix=1,AddRequestHeader=X-Request-id,${requestId}"
        // 格式2: JSON数组格式
        if (filtersStr.startsWith("[")) {
            // JSON数组格式
            return JacksonUtil.fromJsonToList(filtersStr, FilterDefinitionDO.class);
        }

        // 逗号分隔的键值对格式
        String[] parts = filtersStr.split(",");
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                filters.add(new FilterDefinitionDO(part.trim()));
            }
        }
        return filters;
    }
}