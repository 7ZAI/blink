package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.InstanceDetailRsp;
import com.blink.gateway.admin.dto.rsp.QueryInstanceListRsp;
import com.blink.gateway.admin.dto.vo.ComponentHealthVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.HealthDetailVO;
import com.blink.gateway.admin.dto.vo.HttpMetricsVO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import com.blink.gateway.admin.dto.vo.JvmMetricsVO;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ConfigValueConstant.DEFAULT_DRAIN_WAIT_SECONDS;
import static com.blink.gateway.admin.constants.ConfigValueConstant.HEALTH_CHECK_TIMEOUT_SECONDS;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_DRAINING;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_OFFLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_SHUTDOWN;
import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_INSTANCE_DETAIL_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GATEWAY_INSTANCE_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ONLINE_CANNOT_DELETE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.LAST_INSTANCE_CANNOT_OFFLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_NOT_READY;
import static com.blink.gateway.admin.constants.ErrCodeConstant.OFFLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ONLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.QUERY_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
import static com.blink.gateway.admin.constants.ScheduleConstant.INSTANCE_SYNC_CRON;
import static com.blink.gateway.admin.constants.ServiceConstant.GATEWAY_SERVICE_NAME;

/**
 * 网关实例管理服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class GatewayInstanceServiceImpl implements GatewayInstanceService {

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private GatewayInstanceMapper gatewayInstanceMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private NamingMaintainService namingMaintainService;

    @Value("${spring.cloud.nacos.discovery.namespace:public}")
    private String namespaceId;

    @Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}")
    private String groupName;

    /**
     * 流量排空等待时间（秒）
     */
    @Value("${blink.gateway.instance.drain-wait-seconds:30}")
    private Integer drainWaitSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    @Override
    public ResponseDTO<GatewayInstanceListRsp> getGatewayInstances() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<GatewayInstanceVO> instanceList = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                GatewayInstanceVO vo = new GatewayInstanceVO();
                // 统一使用 serviceId:host:port 格式
                String instanceId = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
                vo.setInstanceId(instanceId);
                vo.setServiceId(instance.getServiceId());
                vo.setHost(instance.getHost());
                vo.setPort(instance.getPort());
                vo.setUri(instance.getUri().toString());
                vo.setStatus(INSTANCE_STATUS_ONLINE);
                vo.setStatusDesc("在线");
                instanceList.add(vo);
            }

            GatewayInstanceListRsp rsp = new GatewayInstanceListRsp();
            rsp.setTotal(instanceList.size());
            rsp.setInstances(instanceList);

            log.info("[GatewayInstance] 获取网关实例列表成功 | total: {}", instanceList.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 获取网关实例列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关实例列表失败：" + e.getMessage(), e, GET_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    public ResponseDTO<GatewayInstanceVO> getGatewayInstanceDetail(GetGatewayInstanceDetailReq req) {
        try {
            String instanceId = req.getInstanceId();
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(instanceId);
            if (ObjectUtil.isNull(instanceDO)) {
                // 尝试从注册中心获取
                List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
                for (ServiceInstance instance : instances) {
                    // 统一使用 serviceId:host:port 格式比对
                    String registryInstanceId = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
                    if (registryInstanceId.equals(instanceId)) {
                        GatewayInstanceVO vo = new GatewayInstanceVO();
                        vo.setInstanceId(instanceId);
                        vo.setServiceId(instance.getServiceId());
                        vo.setHost(instance.getHost());
                        vo.setPort(instance.getPort());
                        vo.setUri(instance.getUri().toString());
                        vo.setStatus(INSTANCE_STATUS_ONLINE);
                        vo.setStatusDesc("在线");

                        log.info("[GatewayInstance] 获取实例详情成功(从注册中心) | instanceId: {}", instanceId);

                        return ResponseDTO.newSuccessInstance(vo);
                    }
                }
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            GatewayInstanceVO vo = BeanUtil.copyProperties(instanceDO, GatewayInstanceVO.class);
            vo.setStatusDesc(getStatusDesc(instanceDO.getStatus()));

            log.info("[GatewayInstance] 获取实例详情成功 | instanceId: {}", instanceId);

            return ResponseDTO.newSuccessInstance(vo);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 获取网关实例详情失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关实例详情失败：" + e.getMessage(), e, GET_INSTANCE_DETAIL_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> offlineInstance(OfflineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 查询实例
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(instanceId);
            if (ObjectUtil.isNull(instanceDO)) {
                // 如果数据库中没有，尝试从注册中心查找
                instanceDO = findInstanceFromRegistry(instanceId);
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }
            }

            // 唯一实例保护：检查在线实例数量
            Long onlineCount = gatewayInstanceMapper.selectCount(
                    new LambdaQueryWrapper<GatewayInstanceDO>()
                            .eq(GatewayInstanceDO::getStatus, INSTANCE_STATUS_ONLINE)
            );
            if (onlineCount <= 1) {
                log.warn("[GatewayInstance] 拒绝下线操作：这是最后一个在线实例 | instanceId: {}, onlineCount: {}", instanceId, onlineCount);
                BlinkException.throwBusinessException(LAST_INSTANCE_CANNOT_OFFLINE);
            }

            // 通过 Nacos API 设置实例 enabled=false
            updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), false);

            // 更新数据库状态为下线
            instanceDO.setStatus(INSTANCE_STATUS_SHUTDOWN);
            instanceDO.setOfflineTime(LocalDateTime.now());
            instanceDO.setOfflineReason(req.getReason());
            instanceDO.setOfflineType("MANUAL");  // 主动下线
            gatewayInstanceMapper.updateById(instanceDO);

            log.info("[GatewayInstance] 网关实例下线成功 | instanceId: {}, reason: {}", instanceId, req.getReason());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 下线网关实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("下线网关实例失败：" + e.getMessage(), e, OFFLINE_INSTANCE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> gracefulOfflineInstance(OfflineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 查询实例
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(instanceId);
            if (ObjectUtil.isNull(instanceDO)) {
                instanceDO = findInstanceFromRegistry(instanceId);
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }
            }

            // 唯一实例保护：检查在线实例数量
            Long onlineCount = gatewayInstanceMapper.selectCount(
                    new LambdaQueryWrapper<GatewayInstanceDO>()
                            .eq(GatewayInstanceDO::getStatus, INSTANCE_STATUS_ONLINE)
            );
            if (onlineCount <= 1) {
                log.warn("[GatewayInstance] 拒绝优雅下线操作：这是最后一个在线实例 | instanceId: {}, onlineCount: {}", instanceId, onlineCount);
                BlinkException.throwBusinessException(LAST_INSTANCE_CANNOT_OFFLINE);
            }

            int waitSeconds = ObjectUtil.isNotNull(drainWaitSeconds) ? drainWaitSeconds : DEFAULT_DRAIN_WAIT_SECONDS;
            log.info("[GatewayInstance] 开始优雅下线流程 | instanceId: {}, drainWaitSeconds: {}s", instanceId, waitSeconds);

            // 提取 lambda 需要的 final 变量
            final String host = instanceDO.getHost();
            final Integer port = instanceDO.getPort();
            final Integer dbId = instanceDO.getId();
            final String reason = req.getReason();

            // 第一步：更新数据库状态为 DRAINING（排空中）
            instanceDO.setStatus(INSTANCE_STATUS_DRAINING);
            instanceDO.setOfflineReason(reason);
            instanceDO.setOfflineType("DRAINING");  // 排空下线
            gatewayInstanceMapper.updateById(instanceDO);
            log.info("[GatewayInstance] 实例状态更新为 DRAINING | instanceId: {}", instanceId);

            // 第二步：通过 Nacos API 设置实例 weight=0（停止接收新流量）
            updateInstanceWeight(host, port, 0.0);
            log.info("[GatewayInstance] Nacos 实例权重设置为 0 | instanceId: {}", instanceId);

            // 第三步：等待排空时间（异步执行，不阻塞请求）
            // 使用 CompletableFuture 异步处理
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    log.info("[GatewayInstance] 等待流量排空... | instanceId: {}, waitSeconds: {}s", instanceId, waitSeconds);
                    Thread.sleep(waitSeconds * 1000L);

                    // 第四步：排空完成后，设置 enabled=false
                    updateInstanceEnabled(host, port, false);
                    log.info("[GatewayInstance] Nacos 实例已禁用 | instanceId: {}", instanceId);

                    // 第五步：更新数据库状态为 SHUTDOWN
                    GatewayInstanceDO updateDO = new GatewayInstanceDO();
                    updateDO.setId(dbId);
                    updateDO.setStatus(INSTANCE_STATUS_SHUTDOWN);
                    updateDO.setOfflineTime(LocalDateTime.now());
                    updateDO.setOfflineType("MANUAL");  // 最终标记为主动下线完成
                    gatewayInstanceMapper.updateById(updateDO);

                    log.info("[GatewayInstance] 优雅下线完成 | instanceId: {}", instanceId);
                } catch (InterruptedException e) {
                    log.error("[GatewayInstance] 流量排空等待被中断 | instanceId: {}", instanceId, e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("[GatewayInstance] 优雅下线异步处理失败 | instanceId: {}", instanceId, e);
                }
            });

            log.info("[GatewayInstance] 优雅下线流程启动成功，正在后台执行 | instanceId: {}", instanceId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 优雅下线网关实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("优雅下线网关实例失败：" + e.getMessage(), e, OFFLINE_INSTANCE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 查询实例
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(instanceId);
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            // 健康检查预检：验证实例是否真正可用
            HealthDetailVO healthDetail = fetchHealthDetail(instanceDO.getUri());
            if (ObjectUtil.isNull(healthDetail)) {
                log.warn("[GatewayInstance] 实例健康检查失败，无法获取健康状态 | instanceId: {}, uri: {}", instanceId, instanceDO.getUri());
                BlinkException.throwBusinessException(INSTANCE_NOT_READY);
            }
            if (!"UP".equals(healthDetail.getStatus())) {
                log.warn("[GatewayInstance] 实例健康状态非 UP，不允许上线 | instanceId: {}, healthStatus: {}", instanceId, healthDetail.getStatus());
                BlinkException.throwBusinessException(INSTANCE_NOT_READY);
            }

            log.info("[GatewayInstance] 实例健康检查通过 | instanceId: {}, healthStatus: {}", instanceId, healthDetail.getStatus());

            // 通过 Nacos API 设置实例 enabled=true
            updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), true);

            // 更新数据库状态为在线
            instanceDO.setStatus(INSTANCE_STATUS_ONLINE);
            instanceDO.setOnlineTime(LocalDateTime.now());
            instanceDO.setOfflineReason(null);  // 清空下线原因
            instanceDO.setOfflineType(null);    // 清空下线类型
            gatewayInstanceMapper.updateById(instanceDO);

            log.info("[GatewayInstance] 网关实例上线成功 | instanceId: {}", instanceId);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 上线网关实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("上线网关实例失败：" + e.getMessage(), e, ONLINE_INSTANCE_FAILED);
        }
    }

    /**
     * 通过 Nacos API 更新实例的 enabled 状态
     *
     * @param ip      实例IP
     * @param port    实例端口
     * @param enabled 是否启用
     */
    private void updateInstanceEnabled(String ip, Integer port, boolean enabled) {
        try {
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setEnabled(enabled);
            instance.setEphemeral(true);

            namingMaintainService.updateInstance(groupName, GATEWAY_SERVICE_NAME, instance);

            log.info("[GatewayInstance] Nacos 实例状态更新成功 | ip: {}, port: {}, enabled: {}", ip, port, enabled);
        } catch (NacosException e) {
            log.error("[GatewayInstance] Nacos 实例状态更新失败 | ip: {}, port: {}, enabled: {}, error: {}",
                    ip, port, enabled, e.getMessage(), e);
            throw new BlinkException("Nacos 实例状态更新失败：" + e.getMessage(), e,
                    enabled ? ONLINE_INSTANCE_FAILED : OFFLINE_INSTANCE_FAILED);
        }
    }

    /**
     * 通过 Nacos API 更新实例的权重
     *
     * @param ip     实例IP
     * @param port   实例端口
     * @param weight 权重值（0-100，0表示不接收新流量）
     */
    private void updateInstanceWeight(String ip, Integer port, double weight) {
        try {
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setWeight(weight);
            instance.setEphemeral(true);

            namingMaintainService.updateInstance(groupName, GATEWAY_SERVICE_NAME, instance);

            log.info("[GatewayInstance] Nacos 实例权重更新成功 | ip: {}, port: {}, weight: {}", ip, port, weight);
        } catch (NacosException e) {
            log.error("[GatewayInstance] Nacos 实例权重更新失败 | ip: {}, port: {}, weight: {}, error: {}",
                    ip, port, weight, e.getMessage(), e);
            throw new BlinkException("Nacos 实例权重更新失败：" + e.getMessage(), e, OFFLINE_INSTANCE_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> refreshInstanceStatus() {
        try {
            log.info("[GatewayInstance] 手动刷新实例状态...");

            // 执行同步逻辑
            doSyncInstanceStatus();

            log.info("[GatewayInstance] 实例状态刷新完成");

            return ResponseDTO.newSuccessInstance();
        } catch (Exception e) {
            log.error("[GatewayInstance] 刷新实例状态失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("刷新实例状态失败：" + e.getMessage(), e, GET_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    // 已移除自动定时调度，改为通过 SSE 监控消息实时同步
    // @Scheduled(cron = INSTANCE_SYNC_CRON)
    @Transactional(rollbackFor = Exception.class)
    public void syncInstanceStatus() {
        try {
            log.info("[GatewayInstance] 开始同步网关实例状态...");
            doSyncInstanceStatus();
            log.info("[GatewayInstance] 网关实例状态同步完成");
        } catch (Exception e) {
            log.error("[GatewayInstance] 同步网关实例状态失败 | error: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行实例状态同步逻辑
     */
    private void doSyncInstanceStatus() {
        // 获取注册中心的所有实例
        List<ServiceInstance> registryInstances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
        // 使用统一格式 serviceId:host:port 作为 key
        Map<String, ServiceInstance> registryMap = registryInstances.stream()
                .collect(Collectors.toMap(
                        instance -> instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort(),
                        i -> i));

        // 查询数据库中的所有实例
        List<GatewayInstanceDO> dbInstances = gatewayInstanceMapper.selectList(null);

        // 更新数据库中在线实例的状态
        for (GatewayInstanceDO instanceDO : dbInstances) {
            // 根据下线类型区分处理
            String offlineType = instanceDO.getOfflineType();
            Byte currentStatus = instanceDO.getStatus();

            // DRAINING 状态的实例不处理（由优雅下线异步流程处理）
            if (INSTANCE_STATUS_DRAINING.equals(currentStatus)) {
                log.debug("[GatewayInstance] 跳过 DRAINING 状态实例同步 | instanceId: {}", instanceDO.getInstanceId());
                continue;
            }

            // MANUAL 主动下线的实例不自动恢复（用户意图）
            if (INSTANCE_STATUS_SHUTDOWN.equals(currentStatus) && "MANUAL".equals(offlineType)) {
                log.debug("[GatewayInstance] 跳过 MANUAL 下线实例自动恢复 | instanceId: {}", instanceDO.getInstanceId());
                continue;
            }

            if (registryMap.containsKey(instanceDO.getInstanceId())) {
                // 实例在注册中心，标记为在线
                if (!instanceDO.getStatus().equals(INSTANCE_STATUS_ONLINE)) {
                    // FAULT 被动下线的实例，如果恢复则自动标记为在线
                    instanceDO.setStatus(INSTANCE_STATUS_ONLINE);
                    instanceDO.setOnlineTime(LocalDateTime.now());
                    instanceDO.setOfflineType(null);  // 清空下线类型
                    instanceDO.setOfflineReason(null); // 清空下线原因
                    gatewayInstanceMapper.updateById(instanceDO);
                    log.info("[GatewayInstance] FAULT 下线实例自动恢复为在线 | instanceId: {}", instanceDO.getInstanceId());
                }
                registryMap.remove(instanceDO.getInstanceId());
            } else {
                // 实例不在注册中心
                if (INSTANCE_STATUS_ONLINE.equals(currentStatus)) {
                    // 在线实例变为离线，标记为 FAULT 被动下线
                    instanceDO.setStatus(INSTANCE_STATUS_OFFLINE);
                    instanceDO.setOfflineTime(LocalDateTime.now());
                    instanceDO.setOfflineType("FAULT");  // 被动下线
                    instanceDO.setOfflineReason("实例从注册中心消失，自动标记为故障离线");
                    gatewayInstanceMapper.updateById(instanceDO);
                    log.info("[GatewayInstance] 实例被动下线（FAULT） | instanceId: {}", instanceDO.getInstanceId());
                } else if (INSTANCE_STATUS_SHUTDOWN.equals(currentStatus) && "FAULT".equals(offlineType)) {
                    // FAULT 下线状态保持不变（等待恢复）
                    log.debug("[GatewayInstance] FAULT 下线实例等待恢复 | instanceId: {}", instanceDO.getInstanceId());
                }
            }
        }

        // 新增注册中心有但数据库没有的实例
        for (ServiceInstance instance : registryMap.values()) {
            GatewayInstanceDO newInstance = new GatewayInstanceDO();
            // 统一使用 serviceId:host:port 格式生成 instanceId
            String instanceId = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
            newInstance.setInstanceId(instanceId);
            newInstance.setServiceId(instance.getServiceId());
            newInstance.setHost(instance.getHost());
            newInstance.setPort(instance.getPort());
            newInstance.setUri(instance.getUri().toString());
            newInstance.setStatus(INSTANCE_STATUS_ONLINE);
            newInstance.setOnlineTime(LocalDateTime.now());
            gatewayInstanceMapper.insert(newInstance);
            log.info("[GatewayInstance] 新增网关实例 | instanceId: {}", instanceId);
        }
    }

    /**
     * 从注册中心查找实例
     *
     * @param instanceId 实例ID
     * @return 实例信息，不存在则返回null
     */
    private GatewayInstanceDO findInstanceFromRegistry(String instanceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
        for (ServiceInstance instance : instances) {
            // 统一使用 serviceId:host:port 格式比对
            String registryInstanceId = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
            if (registryInstanceId.equals(instanceId)) {
                GatewayInstanceDO instanceDO = new GatewayInstanceDO();
                instanceDO.setInstanceId(instanceId);
                instanceDO.setServiceId(instance.getServiceId());
                instanceDO.setHost(instance.getHost());
                instanceDO.setPort(instance.getPort());
                instanceDO.setUri(instance.getUri().toString());
                instanceDO.setStatus(INSTANCE_STATUS_ONLINE);
                return instanceDO;
            }
        }
        return null;
    }

    /**
     * 获取状态描述
     *
     * @param status 状态码
     * @return 状态描述
     */
    private String getStatusDesc(Byte status) {
        if (ObjectUtil.isNull(status)) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "在线";  // INSTANCE_STATUS_ONLINE
            case 1 -> "离线";  // INSTANCE_STATUS_OFFLINE
            case 2 -> "下线";  // INSTANCE_STATUS_SHUTDOWN
            case 3 -> "排空";  // INSTANCE_STATUS_DRAINING
            default -> "未知";
        };
    }

    // ==================== 实例管理新增方法 ====================

    @Override
    public ResponseDTO<QueryInstanceListRsp> queryInstanceList(QueryInstanceReq req) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();

            if (StrUtil.isNotBlank(req.getServiceId())) {
                queryWrapper.like(GatewayInstanceDO::getServiceId, req.getServiceId());
            }
            if (StrUtil.isNotBlank(req.getHost())) {
                queryWrapper.like(GatewayInstanceDO::getHost, req.getHost());
            }
            if (ObjectUtil.isNotNull(req.getStatus())) {
                queryWrapper.eq(GatewayInstanceDO::getStatus, req.getStatus());
            }

            // 排序：按更新时间降序
            queryWrapper.orderByDesc(GatewayInstanceDO::getUpdateTime);

            // 分页查询
            Page<GatewayInstanceDO> page = new Page<>(req.getPageNum(), req.getPageSize());
            Page<GatewayInstanceDO> resultPage = gatewayInstanceMapper.selectPage(page, queryWrapper);

            // 转换为 VO
            List<InstanceInfoVO> voList = resultPage.getRecords().stream()
                    .map(this::convertToInstanceInfoVO)
                    .collect(Collectors.toList());

            // 构建响应
            QueryInstanceListRsp rsp = new QueryInstanceListRsp();
            rsp.setRows(voList);
            rsp.setTotal((int) resultPage.getTotal());
            rsp.setPageNum(req.getPageNum());
            rsp.setPageSize(req.getPageSize());
            rsp.setPages((int) resultPage.getPages());

            log.info("[GatewayInstance] 分页查询实例列表成功 | total: {}, pageNum: {}, pageSize: {}",
                    resultPage.getTotal(), req.getPageNum(), req.getPageSize());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 查询实例列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("查询实例列表失败：" + e.getMessage(), e, QUERY_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<EmptyBody> deleteInstance(DeleteInstanceReq req) {
        try {
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(req.getId());
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            // 不允许删除在线实例
            if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())) {
                BlinkException.throwBusinessException(INSTANCE_ONLINE_CANNOT_DELETE);
            }

            gatewayInstanceMapper.deleteById(req.getId());

            log.info("[GatewayInstance] 删除实例成功 | id: {}, instanceId: {}", req.getId(), instanceDO.getInstanceId());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 删除实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("删除实例失败：" + e.getMessage(), e, DELETE_INSTANCE_FAILED);
        }
    }

    @Override
    public ResponseDTO<InstanceDetailRsp> getInstanceDetailWithMetrics(GetInstanceDetailReq req) {
        try {
            // 查询实例基本信息
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(req.getId());
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            InstanceDetailRsp rsp = new InstanceDetailRsp();

            // 基本信息
            InstanceInfoVO instanceInfo = convertToInstanceInfoVO(instanceDO);
            rsp.setInstanceInfo(instanceInfo);

            // 如果实例在线，从 Redis 获取实时监控指标
            if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())) {
                // 获取 JVM 指标
                JvmMetricsVO jvmMetrics = getJvmMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setJvmMetrics(jvmMetrics);

                // 获取 HTTP 指标
                HttpMetricsVO httpMetrics = getHttpMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setHttpMetrics(httpMetrics);

                // 获取健康状态详情（从 Actuator 实时获取）
                HealthDetailVO healthDetail = fetchHealthDetail(instanceDO.getUri());
                rsp.setHealthDetail(healthDetail);
            } else {
                // 离线实例，返回最近一次采集的指标（如果有）
                JvmMetricsVO jvmMetrics = getJvmMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setJvmMetrics(jvmMetrics);

                HttpMetricsVO httpMetrics = getHttpMetricsFromRedis(instanceDO.getInstanceId());
                rsp.setHttpMetrics(httpMetrics);

                // 健康状态标记为离线
                HealthDetailVO healthDetail = new HealthDetailVO();
                healthDetail.setStatus("OFFLINE");
                rsp.setHealthDetail(healthDetail);
            }

            log.info("[GatewayInstance] 获取实例详情成功 | id: {}", req.getId());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 获取实例详情失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取实例详情失败：" + e.getMessage(), e, GET_INSTANCE_DETAIL_FAILED);
        }
    }

    /**
     * 将 DO 转换为 InstanceInfoVO
     */
    private InstanceInfoVO convertToInstanceInfoVO(GatewayInstanceDO instanceDO) {
        InstanceInfoVO vo = BeanUtil.copyProperties(instanceDO, InstanceInfoVO.class);
        vo.setStatusDesc(getStatusDesc(instanceDO.getStatus()));
        return vo;
    }

    /**
     * 从 Redis 获取 JVM 监控指标
     */
    private JvmMetricsVO getJvmMetricsFromRedis(String instanceId) {
        try {
            String key = GATEWAY_METRICS_PREFIX + instanceId;
            Map<String, Object> metrics = redisClient.hGetStringMap(key);

            log.info("[GatewayInstance] 从 Redis 获取 JVM 指标 | key: {}, metrics size: {}", key, metrics != null ? metrics.size() : 0);

            if (CollUtil.isEmpty(metrics)) {
                log.warn("[GatewayInstance] Redis 中未找到 JVM 指标 | key: {}", key);
                return null;
            }

            JvmMetricsVO vo = new JvmMetricsVO();

            // 堆内存
            Long heapUsed = parseLongValue(metrics.get("heapUsed"));
            Long heapMax = parseLongValue(metrics.get("heapMax"));
            if (heapUsed != null && heapMax != null) {
                vo.setHeapUsed(heapUsed);
                vo.setHeapMax(heapMax);
                if (heapMax > 0) {
                    double percent = (double) heapUsed / heapMax * 100;
                    vo.setHeapUsagePercent(BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                }
            }

            // 非堆内存
            Long nonHeapUsed = parseLongValue(metrics.get("nonHeapUsed"));
            if (nonHeapUsed != null) {
                vo.setNonHeapUsed(nonHeapUsed);
            }

            // GC 指标
            vo.setYoungGcCount(parseLongValue(metrics.get("youngGcCount")));
            vo.setYoungGcTime(parseLongValue(metrics.get("youngGcTime")));
            vo.setOldGcCount(parseLongValue(metrics.get("oldGcCount")));
            vo.setOldGcTime(parseLongValue(metrics.get("oldGcTime")));

            // 线程指标
            vo.setLiveThreads(parseIntValue(metrics.get("liveThreads")));
            vo.setPeakThreads(parseIntValue(metrics.get("peakThreads")));
            vo.setDaemonThreads(parseIntValue(metrics.get("daemonThreads")));

            // 时间戳
            vo.setTimestamp(parseLongValue(metrics.get("timestamp")));

            log.info("[GatewayInstance] JVM 指标解析完成 | heapUsed: {}, heapMax: {}, heapUsage: {}%",
                    heapUsed, heapMax, vo.getHeapUsagePercent());

            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 从 Redis 获取 JVM 指标失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 Long 类型值（支持字符串和数字类型）
     */
    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String strValue = (String) value;
            // 去除可能的引号
            strValue = strValue.replace("\"", "");
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                log.warn("[GatewayInstance] 解析 Long 值失败 | value: {}", value);
                return null;
            }
        }
        return null;
    }

    /**
     * 解析 Integer 类型值（支持字符串和数字类型）
     */
    private Integer parseIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String strValue = (String) value;
            strValue = strValue.replace("\"", "");
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                log.warn("[GatewayInstance] 解析 Integer 值失败 | value: {}", value);
                return null;
            }
        }
        return null;
    }

    /**
     * 从 Redis 获取 HTTP 监控指标
     */
    private HttpMetricsVO getHttpMetricsFromRedis(String instanceId) {
        try {
            String key = GATEWAY_METRICS_PREFIX + instanceId;
            Map<String, Object> metrics = redisClient.hGetStringMap(key);

            if (CollUtil.isEmpty(metrics)) {
                return null;
            }

            HttpMetricsVO vo = new HttpMetricsVO();

            vo.setTotalRequests(parseLongValue(metrics.get("totalRequests")));
            vo.setSuccessRequests(parseLongValue(metrics.get("successRequests")));
            vo.setFailedRequests(parseLongValue(metrics.get("failedRequests")));
            vo.setAvgResponseTime(parseLongValue(metrics.get("avgResponseTime")));

            // 计算成功率
            if (vo.getTotalRequests() != null && vo.getTotalRequests() > 0) {
                long success = vo.getSuccessRequests() != null ? vo.getSuccessRequests() : 0;
                double rate = (double) success / vo.getTotalRequests() * 100;
                vo.setSuccessRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

            // 时间戳
            vo.setTimestamp(parseLongValue(metrics.get("timestamp")));

            log.info("[GatewayInstance] HTTP 指标解析完成 | totalRequests: {}, avgResponseTime: {} ms",
                    vo.getTotalRequests(), vo.getAvgResponseTime());

            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 从 Redis 获取 HTTP 指标失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Actuator 获取健康状态详情
     */
    @SuppressWarnings("unchecked")
    private HealthDetailVO fetchHealthDetail(String uri) {
        try {
            String healthUrl = uri + "/actuator/health";
            String response = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT_SECONDS))
                    .block();

            if (StrUtil.isBlank(response)) {
                return null;
            }

            Map<String, Object> healthMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            HealthDetailVO vo = new HealthDetailVO();
            vo.setStatus((String) healthMap.get("status"));

            // 解析各组件健康状态
            Object componentsObj = healthMap.get("components");
            if (ObjectUtil.isNotNull(componentsObj) && componentsObj instanceof Map) {
                Map<String, Object> components = (Map<String, Object>) componentsObj;
                List<ComponentHealthVO> componentList = new ArrayList<>();

                for (Map.Entry<String, Object> entry : components.entrySet()) {
                    ComponentHealthVO componentVO = new ComponentHealthVO();
                    componentVO.setName(entry.getKey());

                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> componentData = (Map<String, Object>) entry.getValue();
                        componentVO.setStatus((String) componentData.get("status"));
                        componentVO.setDetails((Map<String, Object>) componentData.get("details"));
                    }

                    componentList.add(componentVO);
                }

                vo.setComponents(componentList);
            }

            return vo;
        } catch (Exception e) {
            log.warn("[GatewayInstance] 获取健康状态详情失败 | uri: {}, error: {}", uri, e.getMessage());
            return null;
        }
    }
}