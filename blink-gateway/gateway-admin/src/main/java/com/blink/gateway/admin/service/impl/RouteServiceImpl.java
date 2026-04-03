package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.RouteDefinitionReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.entity.RouteDefinitionDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_ROUTE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_GROUP_EMPTY;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SAVE_ROUTE_FAILED;
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
}