package com.blink.base.service.impl;

import com.blink.base.dto.req.AddRoutesReq;
import com.blink.base.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.base.entity.RouteDefinitionDO;
import com.blink.base.producer.GateWayStreamMessageProducer;
import com.blink.base.service.GateWayRoutesService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blink.base.constans.RedisKeyConstans.*;

/**
 * TODO 异步发送消息 通知网关刷新路由
 *
 * @Author binblink
 * @Date 2025/11/3
 */
@Service
public class GateWayRoutesServiceImpl implements GateWayRoutesService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer messageProducer;



    /**
     * 添加 修改 都在同一个接口
     * @param addRoutesReq
     */
    @Override
    public void saveRoute(AddRoutesReq addRoutesReq) {

        String group = addRoutesReq.getRoutesGroup();
        List<RouteDefinitionDO> routes = addRoutesReq.getRoutes();
        String key = GATEWAY_DYNAMIC_ROUTES + ":" + group;
        Map<String, Object> map = redisClient.hGetStringMap(key);
        for (RouteDefinitionDO route : routes) {
            map.put(route.getId(), route);
        }
        redisClient.hSet(key, map);
        messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);
    }

    @Override
    public void deleteRoute(List<String> routes) {
        for (String routeId : routes) {
            redisClient.hDeleteFields(GATEWAY_DYNAMIC_ROUTES, routeId);
        }
        messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);
    }


    @Override
    public QueryGateWayRoutesRsp getRouteslList(EmptyBody body) {

        var pageRsp = new QueryGateWayRoutesRsp();

        Map<String, Object> map = redisClient.hGetStringMap(GATEWAY_DYNAMIC_ROUTES);
        int total = map.size();
        //记录数
        pageRsp.setTotal(total);
        //总页数
        int pages = (total % pageRsp.getPageSize()) != 0 ? (total / pageRsp.getPageSize() + 1) : (total / pageRsp.getPageSize());

        pageRsp.setPages(pages);
        List<RouteDefinitionDO> routes = new ArrayList<>(total);

        map.forEach((k, v) -> {
            RouteDefinitionDO route = JacksonUtil.parseMessyJson(v.toString(), RouteDefinitionDO.class);
            routes.add(route);
        });

        pageRsp.setRows(routes);
        return pageRsp;
    }
}
