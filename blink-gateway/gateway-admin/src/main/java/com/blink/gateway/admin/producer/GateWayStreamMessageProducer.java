package com.blink.gateway.admin.producer;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.entity.MessageType;
import com.blink.framework.redis.mq.RedisStreamProducer;
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.gateway.admin.entity.RedisMqDO;
import com.blink.gateway.admin.mapper.RedisMqMapper;
import com.blink.gateway.dto.CacheMsg;
import com.blink.gateway.dto.MonitorConfigMsg;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.blink.gateway.admin.constants.MessageStatusConstant.REDIS_MSG_STATUS_SEND_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;

/**
 * 事件发布生产者
 * gateway缓存、路由同步
 * 它们公用一个stream 相当于事件总线
 * @Author binblink
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class GateWayStreamMessageProducer extends RedisStreamProducer {

    @Resource
    private RedisMqMapper redisMqMapper;

    @Value("${spring.application.name}")
    private String appName;

    public GateWayStreamMessageProducer(RedisClient redisClient) {
        super(redisClient);
    }

    /**
     * 数据写操作后 发送同步事件 删除数据缓存
     * 缓存同步（仅删除）
     * @param cacheKey
     */
    public void cacheOnChange(String cacheKey) {
        CacheMsg cacheMsg = new CacheMsg();
        cacheMsg.setKey(cacheKey);
        // D = Delete，表示删除缓存
        cacheMsg.setOperator("D");
        // 设置操作人信息
        setOperatorInfo(cacheMsg);
        //发送通知同步
        StreamMessage<CacheMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, cacheMsg);
        msg.setSender(appName);
        msg.setPayloadClass(CacheMsg.class.getName());
        //发送并记录
        sendAndRecord(msg, cacheMsg);
    }

    /**
     * 发送缓存同步消息（支持增加/修改操作）
     * 用于渠道信息更新等场景，直接更新缓存而非删除重建
     *
     * @param cacheMsg 缓存消息对象，包含 key、value、operator
     */
    public void sendCacheSyncMsg(CacheMsg cacheMsg) {
        // 设置操作人信息
        setOperatorInfo(cacheMsg);
        //发送通知同步
        StreamMessage<CacheMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, cacheMsg);
        msg.setSender(appName);
        msg.setPayloadClass(CacheMsg.class.getName());
        //发送并记录
        sendAndRecord(msg, cacheMsg);
    }

    /**
     * 路由同步
     * @param dynamicRouteKey
     */
    public void routesOnChange(String dynamicRouteKey) {

        RouteSyncMsg routeSyncMsgDTO = new RouteSyncMsg();
        routeSyncMsgDTO.setDynamicRouteKey(dynamicRouteKey);
        //发送消息通知同步
        StreamMessage<RouteSyncMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, routeSyncMsgDTO);
        msg.setSender(appName);
        msg.setPayloadClass(RouteSyncMsg.class.getName());
        //发送并记录
        sendAndRecord(msg, routeSyncMsgDTO);

    }

    /**
     * 路由同步（支持指定实例推送）
     *
     * @param routeSyncMsg 路由同步消息
     */
    public void routesOnChangeWithTarget(RouteSyncMsg routeSyncMsg) {
        StreamMessage<RouteSyncMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, routeSyncMsg);
        msg.setSender(appName);
        msg.setPayloadClass(RouteSyncMsg.class.getName());
        sendAndRecord(msg, routeSyncMsg);
    }

    /**
     * 监控配置同步
     * 向所有 gateway-reactive 实例推送监控配置变更
     *
     * @param configMsg 监控配置消息
     */
    public void monitorConfigOnChange(MonitorConfigMsg configMsg) {
        // 设置操作人信息
        setOperatorInfoForMonitor(configMsg);
        // 发送消息通知同步
        StreamMessage<MonitorConfigMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, configMsg);
        msg.setSender(appName);
        msg.setPayloadClass(MonitorConfigMsg.class.getName());
        sendAndRecord(msg, configMsg);
        log.info("[StreamProducer] 监控配置同步消息已发送 | configKey: {}, configValue: {}",
                configMsg.getConfigKey(), configMsg.getConfigValue());
    }

    /**
     * 设置操作人信息到 CacheMsg
     * 从 Sa-Token 获取当前登录用户信息
     *
     * @param cacheMsg 缓存消息对象
     */
    private void setOperatorInfo(CacheMsg cacheMsg) {
        try {
            // 检查是否已登录
            if (StpUtil.isLogin()) {
                Integer userId = StpUtil.getLoginIdAsInt();
                String userName = StpUtil.getLoginIdAsString();
                cacheMsg.setOperatorUser(userId);
                cacheMsg.setOperatorName(userName);
                log.debug("[StreamProducer] 设置操作人 | userId: {}, userName: {}", userId, userName);
            } else {
                log.debug("[StreamProducer] 当前无登录用户，跳过操作人设置");
            }
        } catch (Exception e) {
            // 获取登录信息失败不影响主流程
            log.warn("[StreamProducer] 获取登录用户信息失败: {}", e.getMessage());
        }
    }

    /**
     * 设置操作人信息到 MonitorConfigMsg
     * 从 Sa-Token 获取当前登录用户信息
     *
     * @param configMsg 监控配置消息对象
     */
    private void setOperatorInfoForMonitor(MonitorConfigMsg configMsg) {
        try {
            // 检查是否已登录
            if (StpUtil.isLogin()) {
                Integer userId = StpUtil.getLoginIdAsInt();
                String userName = StpUtil.getLoginIdAsString();
                configMsg.setOperatorUser(userId);
                configMsg.setOperatorName(userName);
                log.debug("[StreamProducer] 设置操作人 | userId: {}, userName: {}", userId, userName);
            } else {
                log.debug("[StreamProducer] 当前无登录用户，跳过操作人设置");
            }
        } catch (Exception e) {
            // 获取登录信息失败不影响主流程
            log.warn("[StreamProducer] 获取登录用户信息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送并记录
     *
     * @param msg 消息
     * @param t 消息 payload 对象
     * @param <T> payload类型
     */
    private <T> void sendAndRecord(StreamMessage<T> msg, T t){

        RedisMqDO redisMqDO = new RedisMqDO();
        BeanUtil.copyProperties(msg, redisMqDO);

        redisMqDO.setPayload(JacksonUtil.toJson(t));
        redisMqMapper.insert(redisMqDO);

        //带重试机制的发送消息
        Retry retry = new Retry(0, 0, 3);
        String streamId = sendMessageWithRetry(msg, retry);

        redisMqDO.setFailTimes(retry.getFailTimes());
        redisMqDO.setRetryTimes(retry.getTryTimes());

        //失败
        if (Objects.isNull(streamId)) {
            log.error("向Gateway发送Redis Stream 消息 失败！messageInfo:{}", msg);
            redisMqDO.setMsgStatus(REDIS_MSG_STATUS_SEND_FAILED);
        }
        //消息id
        redisMqDO.setStreamId(streamId);
        redisMqMapper.updateById(redisMqDO);
    }



}