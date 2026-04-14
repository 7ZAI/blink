package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetTrafficHistoryReq;
import com.blink.gateway.admin.dto.rsp.GetTrafficHistoryRsp;
import com.blink.gateway.admin.dto.vo.TrafficPointVO;
import com.blink.gateway.admin.entity.GatewayTrafficHistoryDO;
import com.blink.gateway.admin.mapper.GatewayTrafficHistoryMapper;
import com.blink.gateway.admin.service.TrafficHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 流量历史查询服务实现
 *
 * @author binblink
 * @since 2026-04-14
 */
@Service
@Slf4j
public class TrafficHistoryServiceImpl implements TrafficHistoryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private GatewayTrafficHistoryMapper trafficHistoryMapper;

    @Override
    public ResponseDTO<GetTrafficHistoryRsp> getTrafficHistory(GetTrafficHistoryReq req) {
        GetTrafficHistoryRsp rsp = new GetTrafficHistoryRsp();

        // 参数校验和默认值
        long endTime = req.getEndTime() != null ? req.getEndTime() : System.currentTimeMillis();
        long startTime = req.getStartTime() != null ? req.getStartTime() : endTime - 3600000; // 默认 1 小时
        String granularity = req.getGranularity() != null ? req.getGranularity() : "MINUTE";

        // 转换为 LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(endTime), ZoneId.systemDefault());

        // 查询数据库
        List<GatewayTrafficHistoryDO> historyList = trafficHistoryMapper.selectByTimeRangeAndGranularity(
                startDateTime, endDateTime, granularity);

        if (CollUtil.isEmpty(historyList)) {
            rsp.setPoints(new ArrayList<>());
            rsp.setTotalRequests(0L);
            rsp.setPeakQps(0);
            return ResponseDTO.newSuccessInstance(rsp);
        }

        // 转换为 VO
        List<TrafficPointVO> points = new ArrayList<>();
        long totalRequests = 0;
        int peakQps = 0;

        for (GatewayTrafficHistoryDO history : historyList) {
            TrafficPointVO point = new TrafficPointVO();

            // 格式化时间
            LocalDateTime timeBucket = history.getTimeBucket();
            if ("MINUTE".equals(granularity)) {
                point.setTime(timeBucket.format(TIME_FORMATTER));
            } else {
                point.setTime(timeBucket.format(DATE_TIME_FORMATTER));
            }
            point.setTimestamp(timeBucket.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            point.setCount(history.getRequestCount());
            point.setSuccessCount(history.getSuccessCount());
            point.setFailedCount(history.getFailedCount());
            point.setPeakQps(history.getPeakQps());

            points.add(point);

            // 计算汇总值
            totalRequests += history.getRequestCount();
            if (history.getPeakQps() != null && history.getPeakQps() > peakQps) {
                peakQps = history.getPeakQps();
            }
        }

        rsp.setPoints(points);
        rsp.setTotalRequests(totalRequests);
        rsp.setPeakQps(peakQps);

        log.info("[TrafficHistory] 查询完成 | points: {}, totalRequests: {}, peakQps: {}",
                points.size(), totalRequests, peakQps);

        return ResponseDTO.newSuccessInstance(rsp);
    }
}