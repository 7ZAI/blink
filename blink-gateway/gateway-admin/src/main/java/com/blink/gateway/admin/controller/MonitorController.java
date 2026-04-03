package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetGatewayMetricsReq;
import com.blink.gateway.admin.dto.req.QueryGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryHealthStatusReq;
import com.blink.gateway.admin.dto.req.QueryStatisticsReq;
import com.blink.gateway.admin.dto.rsp.GatewayHealthStatusRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.GatewayMetricsRsp;
import com.blink.gateway.admin.dto.rsp.GatewayStatisticsRsp;
import com.blink.gateway.admin.service.MonitorService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关监控控制器
 * 提供网关实例状态和监控数据
 *
 * @author binblink
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private MonitorService monitorService;

    /**
     * 获取网关实例列表
     *
     * @param reqDto 请求参数
     * @return 实例列表
     */
    @PostMapping("/getGatewayInstances")
    public ResponseDTO<GatewayInstanceListRsp> getGatewayInstances(@RequestBody @Validated RequestDTO<QueryGatewayInstanceReq> reqDto) {
        return monitorService.getGatewayInstances(reqDto.getBody());
    }

    /**
     * 获取网关统计数据
     *
     * @param reqDto 请求参数
     * @return 统计数据
     */
    @PostMapping("/getStatistics")
    public ResponseDTO<GatewayStatisticsRsp> getStatistics(@RequestBody @Validated RequestDTO<QueryStatisticsReq> reqDto) {
        return monitorService.getStatistics(reqDto.getBody());
    }

    /**
     * 获取网关健康状态
     *
     * @param reqDto 请求参数
     * @return 健康状态
     */
    @PostMapping("/getHealthStatus")
    public ResponseDTO<GatewayHealthStatusRsp> getHealthStatus(@RequestBody @Validated RequestDTO<QueryHealthStatusReq> reqDto) {
        return monitorService.getHealthStatus(reqDto.getBody());
    }

    /**
     * 获取网关指标数据
     *
     * @param reqDto 请求参数
     * @return 指标数据
     */
    @PostMapping("/getGatewayMetrics")
    public ResponseDTO<GatewayMetricsRsp> getGatewayMetrics(@RequestBody @Validated RequestDTO<GetGatewayMetricsReq> reqDto) {
        return monitorService.getGatewayMetrics(reqDto.getBody());
    }
}