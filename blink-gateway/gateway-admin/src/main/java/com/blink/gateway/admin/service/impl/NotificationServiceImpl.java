package com.blink.gateway.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.constants.ErrCodeConstant;
import com.blink.gateway.admin.constants.RedisKeyConstant;
import com.blink.gateway.admin.dto.req.MarkReadReq;
import com.blink.gateway.admin.dto.req.QueryHistoryReq;
import com.blink.gateway.admin.dto.req.QueryNotificationReq;
import com.blink.gateway.admin.dto.rsp.NotificationHistoryRsp;
import com.blink.gateway.admin.dto.rsp.NotificationItemRsp;
import com.blink.gateway.admin.dto.rsp.NotificationListRsp;
import com.blink.gateway.admin.dto.rsp.UnreadCountRsp;
import com.blink.gateway.admin.entity.SysNotificationDO;
import com.blink.gateway.admin.entity.SysNotificationReadDO;
import com.blink.gateway.admin.mapper.SysNotificationMapper;
import com.blink.gateway.admin.mapper.SysNotificationReadMapper;
import com.blink.gateway.admin.service.NotificationService;
import com.blink.gateway.base.constants.CommonConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现
 *
 * @author binblink
 * @since 2026-04-06
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private SysNotificationMapper notificationMapper;

    @Resource
    private SysNotificationReadMapper notificationReadMapper;

    @Resource
    private RedisClient redisClient;

    @Override
    public NotificationListRsp getNotificationList(QueryNotificationReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();

        // 查询未读消息列表
        List<SysNotificationDO> unreadList = notificationMapper.selectUnreadByUserId(userId, req.getLimit());

        // 转换为响应DTO
        List<NotificationItemRsp> notifications = CollUtil.isEmpty(unreadList)
            ? new ArrayList<>()
            : unreadList.stream()
                .map(n -> {
                    NotificationItemRsp rsp = BeanUtil.copyProperties(n, NotificationItemRsp.class);
                    rsp.setRead(false);
                    return rsp;
                })
                .collect(Collectors.toList());

        NotificationListRsp result = new NotificationListRsp();
        result.setNotifications(notifications);
        result.setUnreadCount(getUnreadCountFromCache(userId));

        return result;
    }

    @Override
    public UnreadCountRsp getUnreadCount() {
        Integer userId = StpUtil.getLoginIdAsInt();

        UnreadCountRsp rsp = new UnreadCountRsp();
        rsp.setUnreadCount(getUnreadCountFromCache(userId));

        return rsp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(MarkReadReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();

        // 全部标记已读
        if (Boolean.TRUE.equals(req.getMarkAll())) {
            markAllRead(userId);
            return;
        }

        // 单条标记已读
        if (ObjectUtil.isNull(req.getNotificationId())) {
            BlinkException.throwBusinessException(ErrCodeConstant.PARAMETER_NOT_NULL);
        }

        SysNotificationReadDO readDO = new SysNotificationReadDO();
        readDO.setNotificationId(req.getNotificationId());
        readDO.setUserId(userId);
        readDO.setReadTime(LocalDateTime.now());

        try {
            notificationReadMapper.insert(readDO);
            decrementUnreadCount(userId);
            log.info("[Notification] 标记已读成功 | userId: {}, notificationId: {}", userId, req.getNotificationId());
        } catch (Exception e) {
            log.warn("[Notification] 消息已读状态已存在 | userId: {}, notificationId: {}", userId, req.getNotificationId());
        }
    }

    /**
     * 全部标记已读
     *
     * @param userId 用户ID
     */
    private void markAllRead(Integer userId) {
        // 获取所有未读消息（最多 MAX_UNREAD_BATCH_SIZE 条）
        List<SysNotificationDO> unreadList = notificationMapper.selectUnreadByUserId(userId, CommonConstants.MAX_UNREAD_BATCH_SIZE);

        if (CollUtil.isEmpty(unreadList)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (SysNotificationDO notification : unreadList) {
            SysNotificationReadDO readDO = new SysNotificationReadDO();
            readDO.setNotificationId(notification.getNotificationId());
            readDO.setUserId(userId);
            readDO.setReadTime(now);
            notificationReadMapper.insert(readDO);
        }

        // 清空未读计数
        clearUnreadCount(userId);
        log.info("[Notification] 全部标记已读成功 | userId: {}, count: {}", userId, unreadList.size());
    }

    @Override
    public NotificationHistoryRsp getHistory(QueryHistoryReq req) {
        Integer userId = StpUtil.getLoginIdAsInt();

        // 构建查询条件
        LambdaQueryWrapper<SysNotificationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysNotificationDO::getTargetType, "ALL")
            .or().eq(SysNotificationDO::getTargetUserId, userId));

        // 类型过滤
        if (ObjectUtil.isNotEmpty(req.getType())) {
            wrapper.eq(SysNotificationDO::getType, req.getType());
        }
        // 严重级别过滤
        if (ObjectUtil.isNotEmpty(req.getSeverity())) {
            wrapper.eq(SysNotificationDO::getSeverity, req.getSeverity());
        }
        // 开始时间过滤
        if (ObjectUtil.isNotEmpty(req.getStartTime())) {
            wrapper.ge(SysNotificationDO::getCreatedTime, req.getStartTime());
        }
        // 结束时间过滤
        if (ObjectUtil.isNotEmpty(req.getEndTime())) {
            wrapper.le(SysNotificationDO::getCreatedTime, req.getEndTime());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(SysNotificationDO::getCreatedTime);

        // 使用自定义分页查询，支持查询后转换VO
        NotificationHistoryRsp rsp = new NotificationHistoryRsp();
        return PageUtils.queryPageCustom(
            req,
            () -> notificationMapper.selectCount(wrapper),
            () -> {
                List<SysNotificationDO> list = notificationMapper.selectList(wrapper);
                return list.stream()
                    .map(notification -> {
                        NotificationItemRsp item = BeanUtil.copyProperties(notification, NotificationItemRsp.class);
                        item.setRead(checkRead(notification.getNotificationId(), userId));
                        return item;
                    })
                    .collect(Collectors.toList());
            },
            rsp
        );
    }

    /**
     * 从缓存获取未读消息数量
     *
     * @param userId 用户ID
     * @return 未读消息数量
     */
    private Integer getUnreadCountFromCache(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        Object countObj = redisClient.get(key);

        if (ObjectUtil.isEmpty(countObj)) {
            // 缓存不存在，从数据库查询
            Integer dbCount = notificationMapper.countUnreadByUserId(userId);
            redisClient.set(key, dbCount);
            redisClient.expire(key, CommonConstants.UNREAD_COUNT_EXPIRE_SECONDS);
            return dbCount;
        }

        return Integer.parseInt(countObj.toString());
    }

    /**
     * 减少未读消息计数
     *
     * @param userId 用户ID
     */
    private void decrementUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        Object countObj = redisClient.get(key);
        if (ObjectUtil.isNotEmpty(countObj) && Integer.parseInt(countObj.toString()) > 0) {
            redisClient.decrement(key);
        }
    }

    /**
     * 清空未读消息计数
     *
     * @param userId 用户ID
     */
    private void clearUnreadCount(Integer userId) {
        String key = RedisKeyConstant.NOTIFICATION_USER_UNREAD + userId;
        redisClient.set(key, "0");
    }

    /**
     * 检查消息是否已读
     *
     * @param notificationId 消息ID
     * @param userId 用户ID
     * @return 是否已读
     */
    private Boolean checkRead(Long notificationId, Integer userId) {
        LambdaQueryWrapper<SysNotificationReadDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotificationReadDO::getNotificationId, notificationId)
            .eq(SysNotificationReadDO::getUserId, userId);
        return notificationReadMapper.selectCount(wrapper) > 0;
    }
}