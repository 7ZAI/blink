package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayMetricsReq;
import com.blink.gateway.admin.dto.req.QueryGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryHealthStatusReq;
import com.blink.gateway.admin.dto.req.QueryStatisticsReq;
import com.blink.gateway.admin.dto.rsp.GatewayHealthStatusRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.GatewayMetricsRsp;
import com.blink.gateway.admin.dto.rsp.GatewayStatisticsRsp;

/**
 * 网关监控服务接口
 *
 * @author binblink
 */
public interface MonitorService {

    /**
     * 获取网关实例列表
     *
     * @param req 请求参数
     * @return 实例列表
     */
    ResponseDTO<GatewayInstanceListRsp> getGatewayInstances(QueryGatewayInstanceReq req);

    /**
     * 获取网关统计数据
     *
     * @param req 请求参数
     * @return 统计数据
     */
    ResponseDTO<GatewayStatisticsRsp> getStatistics(QueryStatisticsReq req);

    /**
     * 获取网关健康状态
     *
     * @param req 请求参数
     * @return 健康状态
     */
    ResponseDTO<GatewayHealthStatusRsp> getHealthStatus(QueryHealthStatusReq req);

    /**
     * 获取网关指标数据
     *
     * @param req 请求参数
     * @return 指标数据
     */
    ResponseDTO<GatewayMetricsRsp> getGatewayMetrics(GetGatewayMetricsReq req);
}