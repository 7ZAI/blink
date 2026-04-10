package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.RouteDefinitionReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.entity.RouteDefinitionDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.admin.service.RouteService;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_ROUTE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.NACOS_ROUTE_NOT_IMPLEMENTED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_GROUP_EMPTY;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SAVE_ROUTE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SYNC_ROUTE_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;

/**
 * 路由管理服务实现
 * 直接操作Redis存储路由，发送Stream通知网关同步
 *
 * @author binblink
 */
@Service
@Slf4j
public class RouteServiceImpl implements RouteService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private GatewayInstanceService gatewayInstanceService;

    private static final String STORAGE_MODE_REDIS = "redis";
    private static final String STORAGE_MODE_NACOS = "nacos";
    private static final String PUSH_MODE_BROADCAST = "broadcast";
    private static final String PUSH_MODE_SPECIFIED = "specified";

    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getRouteList(QueryRouteReq req) {
        QueryGateWayRoutesRsp pageRsp = new QueryGateWayRoutesRsp();

        // 根据路由组查询
        String routesGroup = req.getRoutesGroup();
        String key = StrUtil.isNotBlank(routesGroup)
                ? GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup
                : GATEWAY_DYNAMIC_ROUTES;

        Map<String, Object> map = redisClient.hGetStringMap(key);
        int total = map.size();
        // 记录数
        pageRsp.setTotal(total);
        // 总页数
        int pages = (total % pageRsp.getPageSize()) != 0 ? (total / pageRsp.getPageSize() + 1) : (total / pageRsp.getPageSize());

        pageRsp.setPages(pages);
        List<RouteDefinitionDO> routes = new ArrayList<>(total);

        map.forEach((k, v) -> {
            RouteDefinitionDO route = JacksonUtil.parseMessyJson(v.toString(), RouteDefinitionDO.class);
            routes.add(route);
        });

        pageRsp.setRows(routes);
        return ResponseDTO.newSuccessInstance(pageRsp);
    }

    @Override
    public ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req) {
        try {
            // 校验路由组参数
            String routesGroup = req.getRoutesGroup();
            if (StrUtil.isBlank(routesGroup)) {
                BlinkException.throwBusinessException(ROUTE_GROUP_EMPTY);
            }

            List<RouteDefinitionReq> routesList = req.getRoutes();
            if (ObjectUtil.isNull(routesList) || routesList.isEmpty()) {
                BlinkException.throwBusinessException(ROUTE_GROUP_EMPTY);
            }

            String key = GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup;
            Map<String, Object> map = redisClient.hGetStringMap(key);

            for (RouteDefinitionReq routeReq : routesList) {
                String routeId = routeReq.getId();
                if (StrUtil.isNotBlank(routeId)) {
                    // 将 RouteDefinitionReq 转换为 Map 存储
                    Map<String, Object> routeMap = convertRouteToMap(routeReq);
                    map.put(routeId, routeMap);
                }
            }

            redisClient.hSet(key, map);
            messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);

            log.info("[Route] 保存路由成功 | routesGroup: {}, count: {}", routesGroup, routesList.size());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Route] 保存路由失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("保存路由失败: " + e.getMessage(), e, SAVE_ROUTE_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req) {
        try {
            String routesGroup = req.getRoutesGroup();
            List<String> routeIds = req.getRouteIds();

            if (ObjectUtil.isNull(routeIds) || routeIds.isEmpty()) {
                return ResponseDTO.newSuccessInstance();
            }

            // 根据路由组删除
            String key = StrUtil.isNotBlank(routesGroup)
                    ? GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup
                    : GATEWAY_DYNAMIC_ROUTES;

            for (String routeId : routeIds) {
                redisClient.hDeleteFields(key, routeId);
            }
            messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);

            log.info("[Route] 删除路由成功 | routesGroup: {}, routeIds: {}", routesGroup, routeIds);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Route] 删除路由失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除路由失败: " + e.getMessage(), e, DELETE_ROUTE_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> refreshRoutes() {
        // 路由刷新会通过 Redis Stream 自动触发
        messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);
        log.info("[Route] 刷新路由缓存成功");
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 将 RouteDefinitionReq 转换为 Map
     *
     * @param routeReq 路由定义请求
     * @return 路由定义 Map
     */
    private Map<String, Object> convertRouteToMap(RouteDefinitionReq routeReq) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", routeReq.getId());
        map.put("name", routeReq.getName());
        map.put("uri", routeReq.getUri());
        map.put("predicates", routeReq.getPredicates());
        map.put("filters", routeReq.getFilters());
        map.put("order", routeReq.getOrder());
        map.put("metadata", routeReq.getMetadata());
        return map;
    }

    // ========== 存储方式和实例同步 ==========

    /**
     * 获取支持的存储方式列表
     *
     * @return 存储方式列表
     */
    @Override
    public ResponseDTO<List<StorageModeVO>> getStorageModes() {
        List<StorageModeVO> modes = new ArrayList<>();

        StorageModeVO redisMode = new StorageModeVO();
        redisMode.setMode(STORAGE_MODE_REDIS);
        redisMode.setName("Redis 存储");
        redisMode.setDescription("路由存储在 Redis Hash");
        modes.add(redisMode);

        StorageModeVO nacosMode = new StorageModeVO();
        nacosMode.setMode(STORAGE_MODE_NACOS);
        nacosMode.setName("Nacos 配置");
        nacosMode.setDescription("路由存储在 Nacos Config");
        modes.add(nacosMode);

        return ResponseDTO.newSuccessInstance(modes);
    }

    /**
     * 获取在线网关实例列表
     *
     * @return 在线实例列表
     */
    @Override
    public ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances() {
        ResponseDTO<GatewayInstanceListRsp> rsp = gatewayInstanceService.getGatewayInstances();
        List<GatewayInstanceVO> onlineInstances = rsp.getBody().getInstances().stream()
                .filter(instance -> instance.getStatus() == 0) // STATUS_ONLINE
                .collect(Collectors.toList());

        log.info("[Route] 获取在线网关实例成功 | count: {}", onlineInstances.size());
        return ResponseDTO.newSuccessInstance(onlineInstances);
    }

    /**
     * 同步路由到指定实例
     *
     * @param req 同步请求参数
     * @return 操作结果
     */
    @Override
    public ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req) {
        try {
            RouteSyncMsg routeSyncMsg = new RouteSyncMsg();
            routeSyncMsg.setStorageMode(req.getStorageMode());
            routeSyncMsg.setPushMode(req.getPushMode());
            routeSyncMsg.setTargetInstanceIds(req.getTargetInstanceIds());

            if (STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
                String routesGroup = req.getRoutesGroup();
                if (StrUtil.isBlank(routesGroup)) {
                    routesGroup = "default";
                }
                routeSyncMsg.setDynamicRouteKey(GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup);
            } else if (STORAGE_MODE_NACOS.equals(req.getStorageMode())) {
                routeSyncMsg.setDataId(req.getDataId());
                routeSyncMsg.setGroup(req.getGroup());
            }

            messageProducer.routesOnChangeWithTarget(routeSyncMsg);

            log.info("[Route] 同步路由到实例成功 | storageMode: {}, pushMode: {}, targetInstances: {}",
                    req.getStorageMode(), req.getPushMode(), req.getTargetInstanceIds());

            return ResponseDTO.newSuccessInstance();
        } catch (Exception e) {
            log.error("[Route] 同步路由到实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("同步路由失败: " + e.getMessage(), e, SYNC_ROUTE_FAILED);
        }
    }

    // ========== Nacos 路由管理（委托给 NacosRouteService） ==========

    /**
     * 查询 Nacos 路由列表
     * 由 NacosRouteService 实现
     *
     * @param req 查询请求
     * @return 路由列表
     */
    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req) {
        // 由 NacosRouteService 实现
        throw new BlinkException("Nacos 路由由 NacosRouteService 处理", NACOS_ROUTE_NOT_IMPLEMENTED);
    }

    /**
     * 保存 Nacos 路由
     * 由 NacosRouteService 实现
     *
     * @param req 保存请求
     * @return 操作结果
     */
    @Override
    public ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req) {
        // 由 NacosRouteService 实现
        throw new BlinkException("Nacos 路由由 NacosRouteService 处理", NACOS_ROUTE_NOT_IMPLEMENTED);
    }

    /**
     * 删除 Nacos 路由
     * 由 NacosRouteService 实现
     *
     * @param req 删除请求
     * @return 操作结果
     */
    @Override
    public ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req) {
        // 由 NacosRouteService 实现
        throw new BlinkException("Nacos 路由由 NacosRouteService 处理", NACOS_ROUTE_NOT_IMPLEMENTED);
    }
}