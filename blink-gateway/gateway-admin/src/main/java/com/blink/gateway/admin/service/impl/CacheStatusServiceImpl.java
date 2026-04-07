package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
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

import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_SYNC_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CHANNEL_INFO;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;

/**
 * 缓存状态服务实现
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
    private GaChannelMapper channelMapper;

    @Resource
    private SyncLogMapper syncLogMapper;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

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
            // 获取数据库中的数据
            List<CacheItemStatus> dbItems = getDbItems(type, req.getKeys());

            // 获取各网关实例的缓存状态
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<InstanceCacheStatus> instanceStatusList = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                InstanceCacheStatus instanceStatus = getInstanceCacheStatus(instance, type);
                instanceStatusList.add(instanceStatus);
            }

            // 对比并设置状态
            for (InstanceCacheStatus instanceStatus : instanceStatusList) {
                for (CacheItemStatus item : instanceStatus.getItems()) {
                    CacheItemStatus dbItem = findDbItem(dbItems, item.getKey());
                    if (dbItem == null) {
                        // 数据库中不存在，标记为多余
                        item.setStatus("ORPHAN");
                    } else if (dbItem.getChecksum().equals(item.getChecksum())) {
                        item.setStatus("MATCH");
                    } else {
                        item.setStatus("MISMATCH");
                    }
                }

                // 检查缺失的项
                for (CacheItemStatus dbItem : dbItems) {
                    CacheItemStatus instanceItem = findInstanceItem(instanceStatus.getItems(), dbItem.getKey());
                    if (instanceItem == null) {
                        instanceStatus.getItems().add(CacheItemStatus.builder()
                                .key(dbItem.getKey())
                                .status("MISSING")
                                .checksum(null)
                                .build());
                    }
                }
            }

            CacheCheckRsp rsp = CacheCheckRsp.builder()
                    .type(type)
                    .dbItems(dbItems)
                    .instances(instanceStatusList)
                    .checkTime(LocalDateTime.now())
                    .build();

            log.info("[CacheStatus] 一致性检查完成 | type: {}, dbCount: {}, instanceCount: {}",
                    type, dbItems.size(), instanceStatusList.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CacheStatus] 一致性检查失败 | type: {}, error: {}", type, e.getMessage(), e);
            throw new BlinkException("一致性检查失败: " + e.getMessage(), e, DATA_SYNC_FAILED);
        }
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
                        messageProducer.cacheOnChange("channel:*");
                    } else if (CollUtil.isNotEmpty(req.getKeys())) {
                        for (String key : req.getKeys()) {
                            messageProducer.cacheOnChange(CHANNEL_INFO + key);
                        }
                    }
                }
                case "route" -> messageProducer.routesOnChange(GATEWAY_DYNAMIC_ROUTES);
                case "config" -> messageProducer.cacheOnChange("config:*");
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
                            .key(channel.getChannelId())
                            .checksum(calculateChecksum(channel))
                            .updateTime(channel.getUpdateTime())
                            .build());
                }
            }
            case "route" -> {
                // TODO: 从 Redis 或数据库获取路由数据
            }
            case "config" -> {
                // TODO: 获取配置数据
            }
        }

        return items;
    }

    /**
     * 获取实例缓存状态
     */
    @SuppressWarnings("unchecked")
    private InstanceCacheStatus getInstanceCacheStatus(ServiceInstance instance, String type) {
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
                    items.add(CacheItemStatus.builder()
                            .key((String) itemMap.get("key"))
                            .checksum((String) itemMap.get("checksum"))
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
            log.error("[CacheStatus] 获取实例缓存状态失败 | instance: {}, error: {}",
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
     * 计算对象 checksum
     */
    private String calculateChecksum(Object obj) {
        String json = JacksonUtil.toJson(obj);
        return DigestUtil.md5Hex(json);
    }

    /**
     * 从数据库项列表中查找指定 key
     */
    private CacheItemStatus findDbItem(List<CacheItemStatus> items, String key) {
        return items.stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从实例项列表中查找指定 key
     */
    private CacheItemStatus findInstanceItem(List<CacheItemStatus> items, String key) {
        return items.stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElse(null);
    }
}