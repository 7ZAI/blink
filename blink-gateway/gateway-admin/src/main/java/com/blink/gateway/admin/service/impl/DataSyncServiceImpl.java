package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.SyncChannelDataReq;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.gateway.admin.service.DataSyncService;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_SYNC_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;

/**
 * 数据同步服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class DataSyncServiceImpl implements DataSyncService {

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Resource
    private ChannelService channelService;

    @Resource
    private RouteService routeService;

    @Override
    public ResponseDTO<EmptyBody> syncChannelData(SyncChannelDataReq req) {
        try {
            Byte syncType = ObjectUtil.isNotNull(req.getSyncType()) ? req.getSyncType() : (byte) 0;

            log.info("[DataSync] 开始同步渠道数据 | syncType: {}", syncType == 0 ? "全量同步" : "增量同步");

            // 通过 Redis Stream 通知网关刷新渠道缓存
            messageProducer.cacheOnChange("channel:*");

            log.info("[DataSync] 渠道数据同步完成");

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步渠道数据失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("同步渠道数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> syncRouteData() {
        try {
            log.info("[DataSync] 开始同步路由数据");

            // 通过 Redis Stream 通知网关刷新路由
            messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);

            log.info("[DataSync] 路由数据同步完成");

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步路由数据失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("同步路由数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> syncConfigData() {
        try {
            log.info("[DataSync] 开始同步配置数据");

            // 通过 Redis Stream 通知网关刷新配置
            messageProducer.cacheOnChange("config:*");

            log.info("[DataSync] 配置数据同步完成");

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步配置数据失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("同步配置数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }
}