package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.SyncChannelDataReq;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.gateway.admin.service.DataSyncService;
import com.blink.gateway.admin.service.NotificationPublishService;
import com.blink.gateway.admin.service.RouteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_SYNC_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CHANNEL_INFO;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;
import static com.blink.gateway.base.constants.CommonConstants.SYNC_TYPE_FULL;
import static com.blink.gateway.base.constants.CommonConstants.SYNC_TYPE_INCREMENT;

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

    @Resource
    private NotificationPublishService notificationPublishService;

    @Override
    public ResponseDTO<EmptyBody> syncChannelData(SyncChannelDataReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        String syncTaskId = UUID.randomUUID().toString();

        try {
            Byte syncType = ObjectUtil.isNotNull(req.getSyncType()) ? req.getSyncType() : SYNC_TYPE_FULL;

            // 判断是否指定了渠道ID列表
            if (CollUtil.isNotEmpty(req.getChannelIds())) {
                // 批量同步指定渠道
                log.info("[DataSync] 开始批量同步渠道数据 | syncType: {}, channelCount: {}",
                    syncType.equals(SYNC_TYPE_FULL) ? "全量同步" : "增量同步", req.getChannelIds().size());

                // 针对每个渠道发送缓存变更通知
                for (String channelId : req.getChannelIds()) {
                    String cacheKey = CHANNEL_INFO + channelId;
                    messageProducer.cacheOnChange(cacheKey);
                    log.debug("[DataSync] 同步渠道 | channelId: {}", channelId);
                }

                log.info("[DataSync] 批量渠道数据同步完成 | count: {}", req.getChannelIds().size());
            } else {
                // 同步所有渠道
                log.info("[DataSync] 开始同步所有渠道数据 | syncType: {}", syncType.equals(SYNC_TYPE_FULL) ? "全量同步" : "增量同步");

                // 通过 Redis Stream 通知网关刷新渠道缓存
                messageProducer.cacheOnChange("channel:*");

                log.info("[DataSync] 所有渠道数据同步完成");
            }

            // 发送成功通知
            notificationPublishService.sendOperationSuccess(
                userId,
                "渠道数据同步完成",
                "渠道数据已成功同步到所有网关实例",
                syncTaskId
            );
            log.info("[DataSync] 渠道同步成功 | userId: {}, syncTaskId: {}", userId, syncTaskId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            log.error("[DataSync] 渠道同步失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "渠道数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步渠道数据失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "渠道数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw new BlinkException("同步渠道数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> syncRouteData() {
        Integer userId = StpUtil.getLoginIdAsInt();
        String syncTaskId = UUID.randomUUID().toString();

        try {
            log.info("[DataSync] 开始同步路由数据 | userId: {}", userId);

            // 通过 Redis Stream 通知网关刷新路由
            messageProducer.routesOnChange(GATEWAY_STREAM_EVENT);

            log.info("[DataSync] 路由数据同步完成");

            // 发送成功通知
            notificationPublishService.sendOperationSuccess(
                userId,
                "路由数据同步完成",
                "路由数据已成功同步到所有网关实例",
                syncTaskId
            );
            log.info("[DataSync] 路由同步成功 | userId: {}, syncTaskId: {}", userId, syncTaskId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            log.error("[DataSync] 路由同步失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "路由数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步路由数据失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "路由数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw new BlinkException("同步路由数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> syncConfigData() {
        Integer userId = StpUtil.getLoginIdAsInt();
        String syncTaskId = UUID.randomUUID().toString();

        try {
            log.info("[DataSync] 开始同步配置数据 | userId: {}", userId);

            // 通过 Redis Stream 通知网关刷新配置
            messageProducer.cacheOnChange("config:*");

            log.info("[DataSync] 配置数据同步完成");

            // 发送成功通知
            notificationPublishService.sendOperationSuccess(
                userId,
                "配置数据同步完成",
                "配置数据已成功同步到所有网关实例",
                syncTaskId
            );
            log.info("[DataSync] 配置同步成功 | userId: {}, syncTaskId: {}", userId, syncTaskId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            log.error("[DataSync] 配置同步失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "配置数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw e;
        } catch (Exception e) {
            log.error("[DataSync] 同步配置数据失败 | userId: {}, error: {}", userId, e.getMessage(), e);

            notificationPublishService.sendOperationError(
                userId,
                "配置数据同步失败",
                "同步失败: " + e.getMessage(),
                syncTaskId
            );
            throw new BlinkException("同步配置数据失败：" + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }
}