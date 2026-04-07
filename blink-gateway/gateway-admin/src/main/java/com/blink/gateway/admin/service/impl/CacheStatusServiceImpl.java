package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.CacheItemStatus;
import com.blink.gateway.admin.dto.rsp.InstanceCacheStatus;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.entity.SyncLogDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import com.blink.gateway.admin.mapper.SyncLogMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.CacheStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.Cursor;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_SYNC_FAILED;
import static com.blink.framework.common.constrant.RedisCacheKeyConstant.*;

/**
 * 缓存状态服务实现
 *
 * 一致性检查流程：
 * 1. 从数据库获取源数据，计算 checksum
 * 2. 直接从 Redis 获取缓存数据，与数据库对比
 * 3. 通过 actuator 调用各 gateway-reactive 实例，获取本地 Caffeine 缓存状态
 * 4. 对比三方数据：数据库、Redis、各实例本地缓存
 *
 * @author binblink
 */
@Service
@Slf4j
public class CacheStatusServiceImpl implements CacheStatusService {

    private static final String GATEWAY_SERVICE_NAME = "gateway-reactive";

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GaChannelMapper channelMapper;

    @Resource
    private SyncLogMapper syncLogMapper;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    /**
     * 缓存类型与 Redis key 前缀的映射
     */
    private static final Map<String, String> CACHE_KEY_PREFIX_MAP = Map.of(
            "channel", CHANNEL_CACHE_PREFIX,
            "config", GATEWAY_CONFIG_PREFIX,
            "route", GATEWAY_DYNAMIC_ROUTES_PREFIX
    );

    @Override
    public ResponseDTO<?> getGatewayInstances() {
        List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);

        List<Map<String, Object>> instanceList = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            Map<String, Object> map = new HashMap<>();
            map.put("instanceId", instance.getInstanceId());
            map.put("host", instance.getHost());
            map.put("port", instance.getPort());
            map.put("uri", instance.getUri().toString());
            instanceList.add(map);
        }

        log.info("[CacheStatus] 获取网关实例列表成功 | total: {}", instanceList.size());

        return ResponseDTO.newSuccessInstance(instanceList);
    }

    @Override
    public ResponseDTO<CacheCheckRsp> checkConsistency(CacheCheckReq req) {
        String type = req.getType();

        if (StrUtil.isBlank(type)) {
            BlinkException.throwBusinessException("检查类型不能为空");
        }

        try {
            // 1. 获取数据库中的数据
            List<CacheItemStatus> dbItems = getDbItems(type, req.getKeys());

            // 2. 直接从 Redis 获取缓存数据并对比
            List<CacheItemStatus> redisItems = getRedisCacheItems(type);

            // 3. 对比 Redis 与数据库
            compareRedisWithDb(redisItems, dbItems);

            // 4. 获取各网关实例的本地缓存状态
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<InstanceCacheStatus> instanceStatusList = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                InstanceCacheStatus instanceStatus = getInstanceCacheStatus(instance, type, dbItems);
                instanceStatusList.add(instanceStatus);
            }

            CacheCheckRsp rsp = CacheCheckRsp.builder()
                    .type(type)
                    .dbItems(dbItems)
                    .redisItems(redisItems)
                    .instances(instanceStatusList)
                    .checkTime(LocalDateTime.now())
                    .build();

            // 统计不一致数量
            int mismatchCount = countMismatches(redisItems, instanceStatusList);
            log.info("[CacheStatus] 一致性检查完成 | type: {}, dbCount: {}, redisCount: {}, instanceCount: {}, mismatch: {}",
                    type, dbItems.size(), redisItems.size(), instanceStatusList.size(), mismatchCount);

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CacheStatus] 一致性检查失败 | type: {}, error: {}", type, e.getMessage(), e);
            throw new BlinkException("一致性检查失败: " + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    /**
     * 从 Redis 获取缓存项
     *
     * @param type 缓存类型
     * @return Redis 缓存项列表
     */
    private List<CacheItemStatus> getRedisCacheItems(String type) {
        List<CacheItemStatus> items = new ArrayList<>();
        String prefix = CACHE_KEY_PREFIX_MAP.get(type.toLowerCase());

        if (StrUtil.isBlank(prefix)) {
            return items;
        }

        try {
            // 使用 scan 获取所有匹配的 key
            String matchPattern = prefix + "*";
            List<String> keys = new ArrayList<>();

            try (Cursor<String> cursor = redisClient.scan(matchPattern, 1000)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            if (CollUtil.isEmpty(keys)) {
                log.info("[CacheStatus] Redis 缓存为空 | type: {}, prefix: {}", type, prefix);
                return items;
            }

            for (String key : keys) {
                Object value = redisClient.get(key);
                if (value != null) {
                    String checksum = calculateChecksum(value);
                    String businessKey = extractBusinessKey(key, prefix);

                    items.add(CacheItemStatus.builder()
                            .key(businessKey)
                            .checksum(checksum)
                            .updateTime(LocalDateTime.now())
                            .build());
                }
            }

            log.info("[CacheStatus] 获取 Redis 缓存项 | type: {}, count: {}", type, items.size());
        } catch (Exception e) {
            log.error("[CacheStatus] 获取 Redis 缓存失败 | type: {}, error: {}", type, e.getMessage(), e);
        }

        return items;
    }

    /**
     * 对比 Redis 缓存与数据库数据
     *
     * @param redisItems Redis 缓存项
     * @param dbItems    数据库项
     */
    private void compareRedisWithDb(List<CacheItemStatus> redisItems, List<CacheItemStatus> dbItems) {
        // 设置 Redis 缓存状态
        for (CacheItemStatus redisItem : redisItems) {
            CacheItemStatus dbItem = findItemByKey(dbItems, redisItem.getKey());
            if (dbItem == null) {
                // 数据库中不存在，标记为多余
                redisItem.setStatus("ORPHAN");
            } else if (Objects.equals(dbItem.getChecksum(), redisItem.getChecksum())) {
                redisItem.setStatus("MATCH");
            } else {
                redisItem.setStatus("MISMATCH");
            }
        }

        // 检查缺失的项
        for (CacheItemStatus dbItem : dbItems) {
            CacheItemStatus redisItem = findItemByKey(redisItems, dbItem.getKey());
            if (redisItem == null) {
                redisItems.add(CacheItemStatus.builder()
                        .key(dbItem.getKey())
                        .status("MISSING")
                        .checksum(null)
                        .build());
            }
        }
    }

    /**
     * 获取实例本地缓存状态并与数据库对比
     *
     * @param instance 网关实例
     * @param type     缓存类型
     * @param dbItems  数据库数据
     * @return 实例缓存状态
     */
    @SuppressWarnings("unchecked")
    private InstanceCacheStatus getInstanceCacheStatus(ServiceInstance instance, String type, List<CacheItemStatus> dbItems) {
        try {
            String url = String.format("%s/actuator/cache-status/%s", instance.getUri(), type);

            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .build();

            String response = webClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应
            Map<String, Object> responseMap = JacksonUtil.parseMessyJson(response, Map.class);
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) responseMap.get("items");

            List<CacheItemStatus> items = new ArrayList<>();
            if (CollUtil.isNotEmpty(itemsMap)) {
                for (Map<String, Object> itemMap : itemsMap) {
                    CacheItemStatus item = CacheItemStatus.builder()
                            .key((String) itemMap.get("key"))
                            .checksum((String) itemMap.get("checksum"))
                            .build();

                    // 与数据库对比
                    CacheItemStatus dbItem = findItemByKey(dbItems, item.getKey());
                    if (dbItem == null) {
                        item.setStatus("ORPHAN");
                    } else if (Objects.equals(dbItem.getChecksum(), item.getChecksum())) {
                        item.setStatus("MATCH");
                    } else {
                        item.setStatus("MISMATCH");
                    }

                    items.add(item);
                }
            }

            // 检查缺失的项（本地缓存没有但数据库有）
            for (CacheItemStatus dbItem : dbItems) {
                CacheItemStatus instanceItem = findItemByKey(items, dbItem.getKey());
                if (instanceItem == null) {
                    items.add(CacheItemStatus.builder()
                            .key(dbItem.getKey())
                            .status("MISSING")
                            .checksum(null)
                            .build());
                }
            }

            return InstanceCacheStatus.builder()
                    .instanceId(instance.getInstanceId())
                    .ip(instance.getHost())
                    .port(instance.getPort())
                    .items(items)
                    .build();
        } catch (Exception e) {
            log.error("[CacheStatus] 获取实例本地缓存状态失败 | instance: {}, error: {}",
                    instance.getInstanceId(), e.getMessage(), e);

            return InstanceCacheStatus.builder()
                    .instanceId(instance.getInstanceId())
                    .ip(instance.getHost())
                    .port(instance.getPort())
                    .items(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 统计不一致数量
     */
    private int countMismatches(List<CacheItemStatus> redisItems, List<InstanceCacheStatus> instanceStatusList) {
        int count = 0;

        // Redis 不一致
        for (CacheItemStatus item : redisItems) {
            if (!"MATCH".equals(item.getStatus())) {
                count++;
            }
        }

        // 实例本地缓存不一致
        for (InstanceCacheStatus instance : instanceStatusList) {
            for (CacheItemStatus item : instance.getItems()) {
                if (!"MATCH".equals(item.getStatus())) {
                    count++;
                }
            }
        }

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> syncData(CacheSyncReq req) {
        String type = req.getType();
        boolean syncAll = Boolean.TRUE.equals(req.getSyncAll());

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            int successCount = 0;

            // 发送同步消息
            switch (type.toLowerCase()) {
                case "channel" -> {
                    if (syncAll) {
                        // 全量同步：删除所有 Redis 缓存，通知各实例删除本地缓存
                        redisClient.deleteByPrefixScan(CHANNEL_CACHE_PREFIX);
                        messageProducer.cacheOnChange("channel:*");
                    } else if (CollUtil.isNotEmpty(req.getKeys())) {
                        // 指定同步：删除指定 Redis 缓存
                        for (String key : req.getKeys()) {
                            String redisKey = CHANNEL_CACHE_PREFIX + key;
                            redisClient.delete(redisKey);
                            messageProducer.cacheOnChange(redisKey);
                        }
                    }
                }
                case "route" -> {
                    // 路由同步：通知各实例刷新路由
                    messageProducer.routesOnChange(GATEWAY_DYNAMIC_ROUTES_PREFIX);
                }
                case "config" -> {
                    if (syncAll) {
                        redisClient.deleteByPrefixScan(GATEWAY_CONFIG_PREFIX);
                        messageProducer.cacheOnChange("config:*");
                    } else if (CollUtil.isNotEmpty(req.getKeys())) {
                        for (String key : req.getKeys()) {
                            String redisKey = GATEWAY_CONFIG_PREFIX + key;
                            redisClient.delete(redisKey);
                            messageProducer.cacheOnChange(redisKey);
                        }
                    }
                }
                default -> BlinkException.throwBusinessException("不支持的同步类型: " + type);
            }

            successCount = instances.size();

            // 记录同步日志
            SyncLogDO syncLog = new SyncLogDO();
            syncLog.setSyncType(type);
            syncLog.setSyncMode((byte) (syncAll ? 0 : 1));
            syncLog.setSyncKeys(CollUtil.isNotEmpty(req.getKeys()) ? JacksonUtil.toJson(req.getKeys()) : null);
            syncLog.setOperator("admin");
            syncLog.setStatus((byte) 0);
            syncLog.setInstanceCount(instances.size());
            syncLog.setSuccessCount(successCount);
            syncLog.setCreateTime(LocalDateTime.now());
            syncLogMapper.insert(syncLog);

            log.info("[CacheStatus] 同步完成 | type: {}, syncAll: {}, successCount: {}", type, syncAll, successCount);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CacheStatus] 同步失败 | type: {}, error: {}", type, e.getMessage(), e);
            throw new BlinkException("同步失败: " + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<SyncLogRsp> getSyncLogs(Integer pageNum, Integer pageSize) {
        SyncLogRsp rsp = new SyncLogRsp();
        rsp.setPageNum(pageNum);
        rsp.setPageSize(pageSize);

        LambdaQueryWrapper<SyncLogDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(SyncLogDO::getCreateTime);

        Long total = syncLogMapper.selectCount(queryWrapper);
        rsp.setTotal(total.intValue());

        if (total > 0) {
            queryWrapper.last("LIMIT " + pageSize + " OFFSET " + (pageNum - 1) * pageSize);
            List<SyncLogDO> logs = syncLogMapper.selectList(queryWrapper);

            List<SyncLogRsp.SyncLogItem> items = logs.stream().map(log -> {
                SyncLogRsp.SyncLogItem item = new SyncLogRsp.SyncLogItem();
                item.setId(log.getId());
                item.setSyncType(log.getSyncType());
                item.setSyncMode(log.getSyncMode());
                item.setSyncKeys(StrUtil.isNotBlank(log.getSyncKeys())
                        ? JacksonUtil.fromJsonToList(log.getSyncKeys(), String.class)
                        : new ArrayList<>());
                item.setOperator(log.getOperator());
                item.setStatus(log.getStatus());
                item.setInstanceCount(log.getInstanceCount());
                item.setSuccessCount(log.getSuccessCount());
                item.setCreateTime(log.getCreateTime());
                return item;
            }).collect(Collectors.toList());

            rsp.setRows(items);
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取数据库中的数据
     */
    private List<CacheItemStatus> getDbItems(String type, List<String> keys) {
        List<CacheItemStatus> items = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "channel" -> {
                List<GaChannelDO> channels = channelMapper.selectList(
                        new LambdaQueryWrapper<GaChannelDO>()
                                .in(CollUtil.isNotEmpty(keys), GaChannelDO::getChannelId, keys)
                );
                for (GaChannelDO channel : channels) {
                    items.add(CacheItemStatus.builder()
                            .key(channel.getAppKey())
                            .checksum(calculateChecksum(channel))
                            .updateTime(channel.getUpdateTime())
                            .build());
                }
            }
            case "route" -> {
                // TODO: 从数据库获取路由数据
            }
            case "config" -> {
                // TODO: 获取配置数据
            }
        }

        return items;
    }

    /**
     * 计算对象 checksum
     */
    private String calculateChecksum(Object obj) {
        String json = JacksonUtil.toJson(obj);
        return DigestUtil.md5Hex(json);
    }

    /**
     * 从 Redis key 中提取业务 key
     *
     * @param redisKey Redis key
     * @param prefix   key 前缀
     * @return 业务 key
     */
    private String extractBusinessKey(String redisKey, String prefix) {
        if (StrUtil.isNotBlank(prefix) && redisKey.startsWith(prefix)) {
            return redisKey.substring(prefix.length());
        }
        return redisKey;
    }

    /**
     * 从列表中查找指定 key 的项
     */
    private CacheItemStatus findItemByKey(List<CacheItemStatus> items, String key) {
        return items.stream()
                .filter(item -> Objects.equals(key, item.getKey()))
                .findFirst()
                .orElse(null);
    }
}