package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetTrafficHistoryReq;
import com.blink.gateway.admin.dto.rsp.GetTrafficHistoryRsp;
import com.blink.gateway.admin.service.TrafficHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流量历史查询控制器
 * 提供历史流量趋势数据查询
 *
 * @author binblink
 * @since 2026-04-14
 */
@RestController
@RequestMapping("/monitor")
@Slf4j
public class TrafficHistoryController {

    @Resource
    private TrafficHistoryService trafficHistoryService;

    /**
     * 查询流量历史数据
     *
     * @param reqDto 请求参数（时间范围、粒度）
     * @return 流量历史数据
     */
    @PostMapping("/getTrafficHistory")
    public ResponseDTO<GetTrafficHistoryRsp> getTrafficHistory(
            @RequestBody @Validated RequestDTO<GetTrafficHistoryReq> reqDto) {
        log.info("[TrafficHistory] 收到查询请求 | startTime: {}, endTime: {}, granularity: {}",
                reqDto.getBody().getStartTime(),
                reqDto.getBody().getEndTime(),
                reqDto.getBody().getGranularity());
        return trafficHistoryService.getTrafficHistory(reqDto.getBody());
    }
}