package com.blink.base.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.blink.base.dto.rsp.QueryGateWayRoutesRspDTO;
import com.blink.base.entity.RouteDefinitionDO;
import com.blink.base.producer.GateWayStreamMessageProducer;
import com.blink.base.service.GateWayRoutesService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.redis.component.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private GateWayStreamMessageProducer messageProducer;



    /**
     * 添加 修改 都在同一个接口
     * @param routes
     */
    @Override
    public void saveRoute(List<RouteDefinitionDO> routes) {

        Map<String, Object> map = redisClient.hGetStringMap(GATEWAY_DYNAMIC_ROUTES);

        for (RouteDefinitionDO route : routes) {
            map.put(route.getId(), JSON.toJSONString(route));
        }
        redisClient.hSet(GATEWAY_DYNAMIC_ROUTES, map);
        messageProducer.routesOnChange(GATEWAY_STREAM_SYNC);
    }

    @Override
    public void deleteRoute(List<String> routes) {
        for (String routeId : routes) {
            redisClient.hDeleteFields(GATEWAY_DYNAMIC_ROUTES, routeId);
        }
        messageProducer.routesOnChange(GATEWAY_STREAM_SYNC);
    }


    @Override
    public QueryGateWayRoutesRspDTO getRouteslList(EmptyBody body) {

        QueryGateWayRoutesRspDTO pageRsp = new QueryGateWayRoutesRspDTO();

        Map<String, Object> map = redisClient.hGetStringMap(GATEWAY_DYNAMIC_ROUTES);
        int total = map.size();
        //记录数
        pageRsp.setTotal(total);
        //总页数
        int pages = (total % pageRsp.getPageSize()) != 0 ? (total / pageRsp.getPageSize() + 1) : (total / pageRsp.getPageSize());

        pageRsp.setPages(pages);
        List<RouteDefinitionDO> routes = new ArrayList<>(total);

        map.forEach((k, v) -> {
            RouteDefinitionDO route = JSON.parseObject(v.toString(), RouteDefinitionDO.class);
            routes.add(route);
        });

        pageRsp.setRows(routes);
        return pageRsp;
    }
}
