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
import com.blink.framework.redis.mq.StreamMessage;
import com.blink.framework.redis.lock.DistributedLockClient;
import com.blink.gateway.admin.component.InstanceConfigComponent;
import com.blink.gateway.admin.component.NacosConfigComponent;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.dto.InstanceInstanceConfig;
import com.blink.gateway.dto.InstanceOfflineMsg;
import com.blink.gateway.dto.InstanceOnlineMsg;
import com.blink.gateway.admin.dto.req.DeleteInstanceReq;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.GetInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryInstanceReq;
import com.blink.gateway.admin.dto.req.SwitchInstanceGroupReq;
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
import com.blink.gateway.admin.entity.GatewayRouteGroupDO;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.mapper.GatewayRouteGroupMapper;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ALREADY_OFFLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ALREADY_ONLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_DRAINING;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_FAULT_OFFLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ONLINE_CANNOT_DELETE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ONLINE_CANNOT_SWITCH;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_STATUS_INVALID;
import static com.blink.gateway.admin.constants.ErrCodeConstant.LAST_INSTANCE_CANNOT_OFFLINE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_NOT_READY;
import static com.blink.gateway.admin.constants.ErrCodeConstant.OFFLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ONLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.QUERY_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.TARGET_GROUP_CONFIG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_CONFIG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SWITCH_GROUP_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_STREAM_EVENT;
import static com.blink.gateway.admin.constants.ScheduleConstant.INSTANCE_SYNC_CRON;

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
    private GatewayRouteGroupMapper gatewayRouteGroupMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private DistributedLockClient distributedLockClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private NamingMaintainService namingMaintainService;

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private InstanceConfigComponent instanceConfigComponent;

    @Value("${spring.cloud.nacos.discovery.namespace:public}")
    private String namespaceId;

    @Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}")
    private String groupName;

    /**
     * 流量排空等待时间（秒）
     */
    @Value("${blink.gateway.instance.drain-wait-seconds:30}")
    private Integer drainWaitSeconds;

    /**
     * 默认分组标识
     */
    @Value("${blink.gateway.instance.default-group-key:default}")
    private String defaultGroupKey;

    /**
     * 网关服务名称（用于从 Nacos 获取实例列表）
     */
    @Value("${blink.gateway.instance.gateway-service-name:gateway-app}")
    private String gatewayServiceName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取网关实例列表（已废弃）
     *
     * @deprecated 该接口只查询 Nacos 注册中心，无法看到下线实例。
     *             请使用 {@link #queryInstanceList} 接口，支持从数据库查询所有实例状态。
     * @return 实例列表
     */
    @Deprecated
    @Override
    public ResponseDTO<GatewayInstanceListRsp> getGatewayInstances() {
        log.warn("[GatewayInstance] getGatewayInstances 接口已废弃，请使用 queryInstanceList");
        BlinkException.throwBusinessException(INSTANCE_STATUS_INVALID);
        return null; // unreachable，但需要满足编译器要求
    }

    @Override
    public ResponseDTO<GatewayInstanceVO> getGatewayInstanceDetail(GetGatewayInstanceDetailReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 根据 instanceId 字段查询实例（不能用 selectById，因为主键是 id）
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

            if (ObjectUtil.isNull(instanceDO)) {
                // 尝试从注册中心获取
                List<ServiceInstance> instances = discoveryClient.getInstances(gatewayServiceName);
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
    public ResponseDTO<EmptyBody> offlineInstance(OfflineGatewayInstanceReq req) {
        String instanceId = req.getInstanceId();
        String lockKey = "instance:offline:" + instanceId;

        return distributedLockClient.executeWithLock(lockKey, () -> doOfflineInstance(req));
    }

    /**
     * 执行实例下线操作（已在分布式锁保护下）
     *
     * @param req 下线请求参数
     * @return 操作结果
     */
    private ResponseDTO<EmptyBody> doOfflineInstance(OfflineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 根据 instanceId 字段查询实例（不能用 selectById，因为主键是 id）
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

            if (ObjectUtil.isNull(instanceDO)) {
                // 如果数据库中没有，尝试从注册中心查找
                instanceDO = findInstanceFromRegistry(instanceId);
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }
            }

            // 状态校验：只允许 ONLINE(0) 状态下线
            Byte status = instanceDO.getStatus();
            if (INSTANCE_STATUS_SHUTDOWN.equals(status)) {
                log.warn("[GatewayInstance] 实例已下线，拒绝下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_ALREADY_OFFLINE);
            }
            if (INSTANCE_STATUS_OFFLINE.equals(status)) {
                log.warn("[GatewayInstance] 实例故障离线，拒绝下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_FAULT_OFFLINE);
            }
            if (INSTANCE_STATUS_DRAINING.equals(status)) {
                log.warn("[GatewayInstance] 实例排空中，拒绝重复下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_DRAINING);
            }

            // 发送强制下线指令到 Redis Stream（在事务外执行）
            String targetInstance = instanceDO.getHost() + ":" + instanceDO.getPort();
            sendOfflineMessage(targetInstance, "FORCE", 0, req.getReason());
            log.info("[GatewayInstance] 强制下线指令已发送到 Redis Stream | targetInstance: {}", targetInstance);

            // 通过 Nacos API 设置实例 enabled=false（HTTP 请求，在事务外执行）
            try {
                updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), false);
            } catch (Exception e) {
                log.warn("[GatewayInstance] Nacos 实例禁用失败，依赖 Redis Stream 指令 | instanceId: {}, error: {}", instanceId, e.getMessage());
            }

            // 数据库更新操作（在事务内执行）
            final Integer instanceDbId = instanceDO.getId();
            final String offlineReason = req.getReason();
            transactionTemplate.executeWithoutResult(s -> {
                GatewayInstanceDO updateDO = new GatewayInstanceDO();
                updateDO.setId(instanceDbId);
                updateDO.setStatus(INSTANCE_STATUS_SHUTDOWN);
                updateDO.setOfflineTime(LocalDateTime.now());
                updateDO.setOfflineReason(offlineReason);
                updateDO.setOfflineType("MANUAL");
                gatewayInstanceMapper.updateById(updateDO);
            });

            log.info("[GatewayInstance] 网关实例强制下线成功 | instanceId: {}, reason: {}", instanceId, req.getReason());

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 下线网关实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("下线网关实例失败：" + e.getMessage(), e, OFFLINE_INSTANCE_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> gracefulOfflineInstance(OfflineGatewayInstanceReq req) {
        String instanceId = req.getInstanceId();
        String lockKey = "instance:offline:" + instanceId;

        return distributedLockClient.executeWithLock(lockKey, () -> doGracefulOfflineInstance(req));
    }

    /**
     * 执行优雅下线操作（已在分布式锁保护下）
     *
     * @param req 下线请求参数
     * @return 操作结果
     */
    private ResponseDTO<EmptyBody> doGracefulOfflineInstance(OfflineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 根据 instanceId 字段查询实例（不能用 selectById，因为主键是 id）
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

            if (ObjectUtil.isNull(instanceDO)) {
                instanceDO = findInstanceFromRegistry(instanceId);
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }
            }

            // 状态校验：只允许 ONLINE(0) 状态优雅下线
            Byte status = instanceDO.getStatus();
            if (INSTANCE_STATUS_SHUTDOWN.equals(status)) {
                log.warn("[GatewayInstance] 实例已下线，拒绝优雅下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_ALREADY_OFFLINE);
            }
            if (INSTANCE_STATUS_OFFLINE.equals(status)) {
                log.warn("[GatewayInstance] 实例故障离线，拒绝优雅下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_FAULT_OFFLINE);
            }
            if (INSTANCE_STATUS_DRAINING.equals(status)) {
                log.warn("[GatewayInstance] 实例排空中，拒绝重复优雅下线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_DRAINING);
            }

            int waitSeconds = ObjectUtil.isNotNull(drainWaitSeconds) ? drainWaitSeconds : DEFAULT_DRAIN_WAIT_SECONDS;
            log.info("[GatewayInstance] 开始优雅下线流程 | instanceId: {}, drainWaitSeconds: {}s", instanceId, waitSeconds);

            // 提取 lambda 需要的 final 变量
            final String host = instanceDO.getHost();
            final Integer port = instanceDO.getPort();
            final Integer dbId = instanceDO.getId();
            final String reason = req.getReason();

            // 第一步：更新数据库状态为 DRAINING（在事务内执行）
            transactionTemplate.executeWithoutResult(s -> {
                GatewayInstanceDO updateDO = new GatewayInstanceDO();
                updateDO.setId(dbId);
                updateDO.setStatus(INSTANCE_STATUS_DRAINING);
                updateDO.setOfflineReason(reason);
                updateDO.setOfflineType("DRAINING");
                gatewayInstanceMapper.updateById(updateDO);
            });
            log.info("[GatewayInstance] 实例状态更新为 DRAINING | instanceId: {}", instanceId);

            // 第二步：发送下线指令到 Redis Stream（在事务外执行）
            String targetInstance = host + ":" + port;
            sendOfflineMessage(targetInstance, "GRACEFUL", waitSeconds, reason);
            log.info("[GatewayInstance] 下线指令已发送到 Redis Stream | targetInstance: {}, type: GRACEFUL", targetInstance);

            // 第三步：等待排空时间（异步执行，不阻塞请求）
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    log.info("[GatewayInstance] 等待流量排空... | instanceId: {}, waitSeconds: {}s", instanceId, waitSeconds);
                    Thread.sleep(waitSeconds * 1000L);

                    // 第四步：排空完成后，尝试设置 Nacos enabled=false（HTTP 请求）
                    try {
                        updateInstanceEnabled(host, port, false);
                        log.info("[GatewayInstance] Nacos 实例已禁用 | instanceId: {}", instanceId);
                    } catch (Exception e) {
                        log.warn("[GatewayInstance] Nacos 实例禁用失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
                    }

                    // 第五步：更新数据库状态为 SHUTDOWN（异步线程中独立事务）
                    transactionTemplate.executeWithoutResult(s -> {
                        GatewayInstanceDO updateDO = new GatewayInstanceDO();
                        updateDO.setId(dbId);
                        updateDO.setStatus(INSTANCE_STATUS_SHUTDOWN);
                        updateDO.setOfflineTime(LocalDateTime.now());
                        updateDO.setOfflineType("MANUAL");
                        gatewayInstanceMapper.updateById(updateDO);
                    });

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
    public ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req) {
        String instanceId = req.getInstanceId();
        String lockKey = "instance:online:" + instanceId;

        return distributedLockClient.executeWithLock(lockKey, () -> doOnlineInstance(instanceId));
    }

    /**
     * 执行实例上线操作（已在分布式锁保护下）
     *
     * @param instanceId 实例ID
     * @return 操作结果
     */
    private ResponseDTO<EmptyBody> doOnlineInstance(String instanceId) {
        try {
            // 根据 instanceId 字段查询实例（不能用 selectById，因为主键是 id）
            LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            // 状态校验：只允许 OFFLINE(1) 和 SHUTDOWN(2) 状态上线
            Byte status = instanceDO.getStatus();
            if (INSTANCE_STATUS_ONLINE.equals(status)) {
                log.warn("[GatewayInstance] 实例已在线，拒绝上线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_ALREADY_ONLINE);
            }
            if (INSTANCE_STATUS_DRAINING.equals(status)) {
                log.warn("[GatewayInstance] 实例排空中，拒绝上线操作 | instanceId: {}, status: {}", instanceId, status);
                BlinkException.throwBusinessException(INSTANCE_DRAINING);
            }

            // 健康检查预检：验证实例是否真正可用（HTTP 请求，在事务外执行）
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

            // 发送上线指令到 Redis Stream（在事务外执行）
            String targetInstance = instanceDO.getHost() + ":" + instanceDO.getPort();
            sendOnlineMessage(targetInstance);
            log.info("[GatewayInstance] 上线指令已发送到 Redis Stream | targetInstance: {}", targetInstance);

            // 通过 Nacos API 设置实例 enabled=true（HTTP 请求，在事务外执行）
            try {
                updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), true);
                log.info("[GatewayInstance] Nacos 实例已启用 | instanceId: {}", instanceId);
            } catch (Exception e) {
                log.warn("[GatewayInstance] Nacos 实例启用失败，依赖 Redis Stream 指令 | instanceId: {}, error: {}", instanceId, e.getMessage());
            }

            // 数据库更新操作（在事务内执行）
            final Integer instanceDbId = instanceDO.getId();
            transactionTemplate.executeWithoutResult(status1 -> {
                GatewayInstanceDO updateDO = new GatewayInstanceDO();
                updateDO.setId(instanceDbId);
                updateDO.setStatus(INSTANCE_STATUS_ONLINE);
                updateDO.setOnlineTime(LocalDateTime.now());
                updateDO.setOfflineReason(null);
                updateDO.setOfflineType(null);
                gatewayInstanceMapper.updateById(updateDO);
            });

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
     * 通过 Nacos SDK 更新实例的 enabled 状态
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

            namingMaintainService.updateInstance(gatewayServiceName, groupName, instance);

            log.info("[GatewayInstance] Nacos 实例状态更新成功 | ip: {}, port: {}, enabled: {}", ip, port, enabled);
        } catch (NacosException e) {
            log.error("[GatewayInstance] Nacos 实例状态更新失败 | ip: {}, port: {}, enabled: {}, error: {}",
                    ip, port, enabled, e.getMessage(), e);
            throw new BlinkException("Nacos 实例状态更新失败：" + e.getMessage(), e,
                    enabled ? ONLINE_INSTANCE_FAILED : OFFLINE_INSTANCE_FAILED);
        }
    }

    /**
     * 通过 Nacos SDK 更新实例的权重
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

            namingMaintainService.updateInstance(gatewayServiceName, groupName, instance);

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
        List<ServiceInstance> registryInstances = discoveryClient.getInstances(gatewayServiceName);
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
            // 统一使用 serviceId:host:port 格式生成 instanceId
            String instanceId = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();

            // 插入前检查是否已存在（防止重复插入）
            LambdaQueryWrapper<GatewayInstanceDO> existQuery = new LambdaQueryWrapper<>();
            existQuery.eq(GatewayInstanceDO::getInstanceId, instanceId);
            GatewayInstanceDO existInstance = gatewayInstanceMapper.selectOne(existQuery);

            if (ObjectUtil.isNotNull(existInstance)) {
                // 已存在，更新状态为在线（如果当前不是在线状态）
                if (!INSTANCE_STATUS_ONLINE.equals(existInstance.getStatus())) {
                    existInstance.setStatus(INSTANCE_STATUS_ONLINE);
                    existInstance.setOnlineTime(LocalDateTime.now());
                    existInstance.setOfflineType(null);
                    existInstance.setOfflineReason(null);
                    gatewayInstanceMapper.updateById(existInstance);
                    log.info("[GatewayInstance] 实例已存在，更新状态为在线 | instanceId: {}", instanceId);
                } else {
                    log.debug("[GatewayInstance] 实例已存在且状态为在线，跳过 | instanceId: {}", instanceId);
                }
                continue;
            }

            // 不存在，新增实例
            GatewayInstanceDO newInstance = new GatewayInstanceDO();
            newInstance.setInstanceId(instanceId);
            newInstance.setServiceId(instance.getServiceId());
            newInstance.setHost(instance.getHost());
            newInstance.setPort(instance.getPort());
            newInstance.setUri(instance.getUri().toString());
            newInstance.setStatus(INSTANCE_STATUS_ONLINE);
            newInstance.setOnlineTime(LocalDateTime.now());

            // 从实例配置文件或 metadata 获取分组配置，storageMode 从路由分组获取
            InstanceInstanceConfig instanceConfig = getInstanceConfig(instanceId, instance.getMetadata());
            newInstance.setGroupKey(instanceConfig.getGroupKey());

            gatewayInstanceMapper.insert(newInstance);
            log.info("[GatewayInstance] 新增网关实例 | instanceId: {}, groupKey: {}, configSource: {}",
                instanceId, instanceConfig.getGroupKey(), instanceConfig.getSource());
        }
    }

    /**
     * 从注册中心查找实例
     *
     * @param instanceId 实例ID
     * @return 实例信息，不存在则返回null
     */
    private GatewayInstanceDO findInstanceFromRegistry(String instanceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(gatewayServiceName);
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
     * 获取实例配置
     * 优先级：配置文件 > metadata > 默认值
     *
     * @param instanceId 实例ID
     * @param metadata 实例元数据（可能为空）
     * @return 实例配置
     */
    private InstanceInstanceConfig getInstanceConfig(String instanceId, Map<String, String> metadata) {
        InstanceInstanceConfig config = new InstanceInstanceConfig();

        // 1. 尝试从配置文件获取
        InstanceInstanceConfig fileConfig = getInstanceConfigFromFile(instanceId);
        if (fileConfig != null) {
            return fileConfig;
        }

        // 2. 从 metadata 获取（只获取 groupKey，storageMode 从路由分组获取）
        if (metadata != null) {
            String groupKey = metadata.get("groupKey");

            if (StrUtil.isNotBlank(groupKey)) {
                config.setGroupKey(groupKey);
                config.setSource("metadata");
                return config;
            }
        }

        // 3. 使用默认值
        return InstanceInstanceConfig.createDefault(defaultGroupKey);
    }

    /**
     * 从 Nacos 配置文件获取实例配置
     * 配置文件命名规则：gateway-instance-{instanceId}.yaml
     *
     * @param instanceId 实例ID
     * @return 实例配置，获取失败返回 null
     */
    private InstanceInstanceConfig getInstanceConfigFromFile(String instanceId) {
        if (instanceConfigComponent == null) {
            log.debug("[GatewayInstance] InstanceConfigComponent 未注入，无法从配置文件获取实例配置");
            return null;
        }

        try {
            // 使用 InstanceConfigComponent 获取配置
            String content = instanceConfigComponent.getConfigContent(instanceId);

            if (StrUtil.isBlank(content)) {
                log.debug("[GatewayInstance] 实例配置文件不存在 | instanceId: {}", instanceId);
                return null;
            }

            // 解析 YAML 配置
            Yaml yaml = new Yaml();
            Map<String, Object> configMap = yaml.load(content);

            // 提取 blink.gateway 配置（实际配置格式）
            Map<String, Object> gatewayConfigMap = getNestedMap(configMap,
                RouteConstant.INSTANCE_CONFIG_PATH.split("\\."));

            if (gatewayConfigMap == null || gatewayConfigMap.isEmpty()) {
                log.debug("[GatewayInstance] 配置文件中未找到实例配置 | instanceId: {}, path: {}",
                    instanceId, RouteConstant.INSTANCE_CONFIG_PATH);
                return null;
            }

            // 解析 dynamicRoute 配置
            Object dynamicRouteObj = gatewayConfigMap.get(RouteConstant.INSTANCE_CONFIG_DYNAMIC_ROUTE);
            if (!(dynamicRouteObj instanceof Map)) {
                log.debug("[GatewayInstance] 配置文件中未找到 dynamicRoute 配置 | instanceId: {}", instanceId);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> dynamicRouteMap = (Map<String, Object>) dynamicRouteObj;

            // 解析配置（只获取 groupKey，storageMode 从路由分组获取）
            InstanceInstanceConfig config = new InstanceInstanceConfig();

            // groupKey 从 dynamicRoute.group 获取
            Object group = dynamicRouteMap.get(RouteConstant.INSTANCE_CONFIG_ROUTE_GROUP);
            if (group != null) {
                config.setGroupKey(group.toString());
            }

            // 如果 groupKey 为空，返回 null
            if (StrUtil.isBlank(config.getGroupKey())) {
                return null;
            }

            config.setSource("config_file");

            log.info("[GatewayInstance] 从配置文件获取实例配置成功 | instanceId: {}, groupKey: {}",
                instanceId, config.getGroupKey());

            return config;

        } catch (Exception e) {
            log.warn("[GatewayInstance] 解析实例配置文件失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 从嵌套 Map 中获取指定路径的 Map
     *
     * @param map 源 Map
     * @param keys 路径数组
     * @return 目标 Map，不存在返回 null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String[] keys) {
        if (map == null || keys == null || keys.length == 0) {
            return null;
        }

        Map<String, Object> current = map;
        for (String key : keys) {
            Object value = current.get(key);
            if (!(value instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) value;
        }
        return current;
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
            if (StrUtil.isNotBlank(req.getGroupKey())) {
                queryWrapper.eq(GatewayInstanceDO::getGroupKey, req.getGroupKey());
            }

            // 排序：按更新时间降序
            queryWrapper.orderByDesc(GatewayInstanceDO::getUpdateTime);

            // 分页查询
            Page<GatewayInstanceDO> page = new Page<>(req.getPageNum(), req.getPageSize());
            Page<GatewayInstanceDO> resultPage = gatewayInstanceMapper.selectPage(page, queryWrapper);

            // 获取 Nacos 注册中心的实例列表（用于实时状态补充）
            Set<String> registryInstanceIds = getRegistryInstanceIds();

            // 转换为 VO（补充实时状态和冲突检测）
            List<InstanceInfoVO> voList = resultPage.getRecords().stream()
                    .map(db -> convertToInstanceInfoVOWithRegistry(db, registryInstanceIds))
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
    private InstanceInfoVO convertToInstanceInfoVO(GatewayInstanceDO doct) {
        InstanceInfoVO vo = BeanUtil.copyProperties(doct, InstanceInfoVO.class);
        vo.setStatusDesc(getStatusDesc(doct.getStatus()));
        vo.setGroupKey(doct.getGroupKey());
        // storageMode 不再存储在实例中，需要时从路由分组获取
        return vo;
    }

    /**
     * 获取 Nacos 注册中心的实例 ID 集合
     *
     * @return 实例 ID 集合（格式：serviceId:host:port）
     */
    private Set<String> getRegistryInstanceIds() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(gatewayServiceName);
            return instances.stream()
                    .map(i -> i.getServiceId() + ":" + i.getHost() + ":" + i.getPort())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("[GatewayInstance] 获取 Nacos 实例列表失败 | error: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * 将 DO 转换为 InstanceInfoVO，补充 Nacos 实时状态和冲突检测
     *
     * @param dbInstance         数据库实例
     * @param registryInstanceIds Nacos 实例 ID 集合
     * @return 补充实时状态的 VO
     */
    private InstanceInfoVO convertToInstanceInfoVOWithRegistry(GatewayInstanceDO dbInstance, Set<String> registryInstanceIds) {
        InstanceInfoVO vo = BeanUtil.copyProperties(dbInstance, InstanceInfoVO.class);
        vo.setStatusDesc(getStatusDesc(dbInstance.getStatus()));
        vo.setGroupKey(dbInstance.getGroupKey());
        // storageMode 不再存储在实例中，需要时从路由分组获取

        // 检查是否在注册中心
        boolean inRegistry = registryInstanceIds.contains(dbInstance.getInstanceId());
        vo.setInRegistry(inRegistry);

        // 确定健康状态（基于数据库状态和注册中心状态）
        String healthStatus = determineHealthStatus(dbInstance.getStatus(), inRegistry);
        vo.setHealthStatus(healthStatus);

        // 状态冲突检测（基于健康状态）
        String statusConflict = detectStatusConflict(dbInstance.getStatus(), inRegistry, healthStatus);
        vo.setStatusConflict(statusConflict);

        return vo;
    }

    /**
     * 确定实例健康状态
     *
     * @param dbStatus   数据库状态
     * @param inRegistry 是否在注册中心
     * @return 健康状态（UP/DOWN/OFFLINE/DRAINING）
     */
    private String determineHealthStatus(Byte dbStatus, boolean inRegistry) {
        // 排空状态 -> DRAINING（正在优雅下线）
        if (INSTANCE_STATUS_DRAINING.equals(dbStatus)) {
            return "DRAINING";
        }

        // 在注册中心存在 -> UP（Nacos 已做健康检查）
        if (inRegistry) {
            return "UP";
        }

        // 不在注册中心，根据数据库状态判断
        if (INSTANCE_STATUS_ONLINE.equals(dbStatus)) {
            // 在线但不在注册中心 -> DOWN
            return "DOWN";
        }

        // 离线、下线状态且不在注册中心 -> OFFLINE
        return "OFFLINE";
    }

    /**
     * 检测数据库状态与实际状态冲突
     *
     * @param dbStatus    数据库状态
     * @param inRegistry  是否在注册中心
     * @param healthStatus 健康状态
     * @return 冲突提示信息，无冲突返回 null
     */
    private String detectStatusConflict(Byte dbStatus, boolean inRegistry, String healthStatus) {
        // 数据库显示在线，但健康状态非 UP
        if (INSTANCE_STATUS_ONLINE.equals(dbStatus) && !"UP".equals(healthStatus)) {
            return "数据库状态与注册中心不一致：数据库显示在线，但实例未注册";
        }

        // 数据库显示下线，但实例健康在线（已在注册中心）
        if (INSTANCE_STATUS_SHUTDOWN.equals(dbStatus) && "UP".equals(healthStatus)) {
            return "实例已恢复运行，但数据库仍显示下线，请执行上线操作";
        }

        // 数据库显示离线(FAULT)，但实例已恢复健康
        if (INSTANCE_STATUS_OFFLINE.equals(dbStatus) && "UP".equals(healthStatus)) {
            return "实例已自动恢复，建议手动执行上线操作";
        }

        return null;
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
     * 使用 RestTemplate 进行同步 HTTP 请求，避免 WebClient 的异步分发导致 SaToken 上下文丢失
     */
    @SuppressWarnings("unchecked")
    private HealthDetailVO fetchHealthDetail(String uri) {
        try {
            // URI 白名单校验（防止 SSRF）
            if (!isUriAllowed(uri)) {
                log.warn("[GatewayInstance] URI 不在白名单范围内，拒绝健康检查 | uri: {}", uri);
                return null;
            }

            String healthUrl = uri + "/actuator/health";
            // 使用 RestTemplate 同步请求，避免 WebClient 异步问题
            String response = restTemplate.getForObject(healthUrl, String.class);

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

    /**
     * 校验 URI 是否允许访问（防止 SSRF 攻击）
     * <p>
     * 禁止访问以下地址：
     * - localhost (127.0.0.0/8)
     * - 0.0.0.0 (本网络)
     * - 内部元数据地址 (169.254.0.0/16)
     * </p>
     *
     * @param uri 要校验的 URI
     * @return 允许访问返回 true，否则返回 false
     */
    private boolean isUriAllowed(String uri) {
        if (StrUtil.isBlank(uri)) {
            return false;
        }

        try {
            // 解析 URI 获取主机地址
            java.net.URI parsedUri = new java.net.URI(uri);
            String host = parsedUri.getHost();

            if (StrUtil.isBlank(host)) {
                log.warn("[GatewayInstance] URI 解析失败，缺少主机地址 | uri: {}", uri);
                return false;
            }

            // 禁止 localhost 相关地址
            if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.") || "0.0.0.0".equals(host)) {
                log.warn("[GatewayInstance] 禁止访问 localhost 地址 | host: {}", host);
                return false;
            }

            // 禁止元数据服务地址（AWS/GCP 等云环境）
            if (host.startsWith("169.254.")) {
                log.warn("[GatewayInstance] 禁止访问元数据服务地址 | host: {}", host);
                return false;
            }

            // 如果是 IP 地址，校验是否为私有地址的特殊情况
            if (isIpAddress(host)) {
                // 允许私有网络地址（网关实例通常在私有网络中）
                // 但禁止特殊的内网保留地址
                if (host.startsWith("0.")) {
                    log.warn("[GatewayInstance] 禁止访问本网络地址 | host: {}", host);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("[GatewayInstance] URI 解析异常 | uri: {}, error: {}", uri, e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否为 IP 地址格式
     *
     * @param host 主机名或 IP 地址
     * @return 是 IP 地址返回 true
     */
    private boolean isIpAddress(String host) {
        // IPv4 格式校验
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (host.matches(ipv4Pattern)) {
            return true;
        }
        // IPv6 格式校验（简化版）
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^::1$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$";
        return host.matches(ipv6Pattern);
    }

    // ==================== Redis Stream 消息发送 ====================

    /**
     * 消息类型：实例下线
     */
    private static final String MSG_TYPE_INSTANCE_OFFLINE = "INSTANCE_OFFLINE";

    /**
     * 消息类型：实例上线
     */
    private static final String MSG_TYPE_INSTANCE_ONLINE = "INSTANCE_ONLINE";

    /**
     * 发送实例下线指令到 Redis Stream
     *
     * @param targetInstance 目标实例标识（格式：host:port）
     * @param offlineType    下线类型：GRACEFUL / FORCE
     * @param waitSeconds    排空等待时间（秒）
     * @param reason         下线原因
     */
    private void sendOfflineMessage(String targetInstance, String offlineType, int waitSeconds, String reason) {
        try {
            // 构建下线消息
            InstanceOfflineMsg offlineMsg = new InstanceOfflineMsg();
            offlineMsg.setTargetInstance(targetInstance);
            offlineMsg.setOfflineType(offlineType);
            offlineMsg.setDrainWaitSeconds(waitSeconds);
            offlineMsg.setReason(reason);
            offlineMsg.setCommandId(java.util.UUID.randomUUID().toString());

            // 包装为 StreamMessage
            StreamMessage<InstanceOfflineMsg> message = StreamMessage.of(
                    GATEWAY_STREAM_EVENT,
                    MSG_TYPE_INSTANCE_OFFLINE,
                    offlineMsg
            );

            // 发送到 Redis Stream
            String messageId = redisClient.xAdd(GATEWAY_STREAM_EVENT, message);
            log.info("[GatewayInstance] 下线指令发送成功 | messageId: {}, target: {}, type: {}",
                    messageId, targetInstance, offlineType);
        } catch (Exception e) {
            log.error("[GatewayInstance] 下线指令发送失败 | target: {}, error: {}", targetInstance, e.getMessage(), e);
            throw new BlinkException("发送下线指令失败：" + e.getMessage(), e, OFFLINE_INSTANCE_FAILED);
        }
    }

    /**
     * 发送实例上线指令到 Redis Stream
     *
     * @param targetInstance 目标实例标识（格式：host:port）
     */
    private void sendOnlineMessage(String targetInstance) {
        try {
            // 构建上线消息
            InstanceOnlineMsg onlineMsg = new InstanceOnlineMsg();
            onlineMsg.setTargetInstance(targetInstance);
            onlineMsg.setCommandId(java.util.UUID.randomUUID().toString());

            // 包装为 StreamMessage
            StreamMessage<InstanceOnlineMsg> message = StreamMessage.of(
                    GATEWAY_STREAM_EVENT,
                    MSG_TYPE_INSTANCE_ONLINE,
                    onlineMsg
            );

            // 发送到 Redis Stream
            String messageId = redisClient.xAdd(GATEWAY_STREAM_EVENT, message);
            log.info("[GatewayInstance] 上线指令发送成功 | messageId: {}, target: {}", messageId, targetInstance);
        } catch (Exception e) {
            log.error("[GatewayInstance] 上线指令发送失败 | target: {}, error: {}", targetInstance, e.getMessage(), e);
            throw new BlinkException("发送上线指令失败：" + e.getMessage(), e, ONLINE_INSTANCE_FAILED);
        }
    }

    // ==================== 实例分组切换 ====================

    @Override
    public ResponseDTO<EmptyBody> switchInstanceGroup(SwitchInstanceGroupReq req) {
        String instanceId = req.getInstanceId();
        String targetGroupKey = req.getTargetGroupKey();

        // 1. 查询实例信息（事务外）
        LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
        GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

        if (ObjectUtil.isNull(instanceDO)) {
            BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
        }

        // 2. 校验实例状态（必须是离线或下线状态）
        if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())
                || INSTANCE_STATUS_DRAINING.equals(instanceDO.getStatus())) {
            log.warn("[GatewayInstance] 在线实例不允许切换分组 | instanceId: {}, status: {}",
                    instanceId, instanceDO.getStatus());
            BlinkException.throwBusinessException(INSTANCE_ONLINE_CANNOT_SWITCH);
        }

        // 3. 从目标分组获取存储方式，并校验目标分组配置是否存在（事务外）
        String targetStorageMode = getStorageModeByGroupKey(targetGroupKey);
        validateTargetGroupConfig(targetGroupKey, targetStorageMode);

        // 4. 记录旧分组用于日志
        String oldGroupKey = instanceDO.getGroupKey();

        // 5. 先更新数据库（事务内）
        final Integer instanceDbId = instanceDO.getId();
        transactionTemplate.executeWithoutResult(s -> {
            GatewayInstanceDO updateDO = new GatewayInstanceDO();
            updateDO.setId(instanceDbId);
            updateDO.setGroupKey(targetGroupKey);
            gatewayInstanceMapper.updateById(updateDO);
        });

        log.info("[GatewayInstance] 实例分组数据库更新成功 | instanceId: {}, oldGroup: {}, newGroup: {}",
                instanceId, oldGroupKey, targetGroupKey);

        // 6. 更新 Nacos 配置文件中的分组（事务外，数据库更新成功后执行）
        try {
            updateInstanceGroupConfig(instanceId, targetGroupKey);
            log.info("[GatewayInstance] Nacos 配置更新成功 | instanceId: {}, newGroup: {}", instanceId, targetGroupKey);
        } catch (Exception e) {
            // Nacos 更新失败，回滚数据库（手动回滚）
            log.error("[GatewayInstance] Nacos 配置更新失败，回滚数据库 | instanceId: {}, error: {}", instanceId, e.getMessage());
            transactionTemplate.executeWithoutResult(s -> {
                GatewayInstanceDO rollbackDO = new GatewayInstanceDO();
                rollbackDO.setId(instanceDbId);
                rollbackDO.setGroupKey(oldGroupKey);
                gatewayInstanceMapper.updateById(rollbackDO);
            });
            throw e;
        }

        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据分组标识获取存储方式
     * 优先从指定的路由分组获取，如果分组不存在则使用默认分组
     *
     * @param groupKey 分组标识
     * @return 存储方式：nacos/redis
     */
    private String getStorageModeByGroupKey(String groupKey) {
        // 尝试从路由分组获取
        LambdaQueryWrapper<GatewayRouteGroupDO> groupQuery = new LambdaQueryWrapper<>();
        groupQuery.eq(GatewayRouteGroupDO::getGroupKey, groupKey);
        GatewayRouteGroupDO group = gatewayRouteGroupMapper.selectOne(groupQuery);

        if (group != null && StrUtil.isNotBlank(group.getStorageMode())) {
            return group.getStorageMode();
        }

        // 使用默认分组的存储方式
        LambdaQueryWrapper<GatewayRouteGroupDO> defaultQuery = new LambdaQueryWrapper<>();
        defaultQuery.eq(GatewayRouteGroupDO::getGroupKey, defaultGroupKey);
        GatewayRouteGroupDO defaultGroup = gatewayRouteGroupMapper.selectOne(defaultQuery);

        if (defaultGroup != null && StrUtil.isNotBlank(defaultGroup.getStorageMode())) {
            return defaultGroup.getStorageMode();
        }

        // 最终兜底：返回 nacos
        return "nacos";
    }

    /**
     * 校验目标分组配置是否存在
     *
     * @param targetGroupKey 目标分组标识
     * @param storageMode    存储模式
     */
    private void validateTargetGroupConfig(String targetGroupKey, String storageMode) {
        // 如果是默认分组，直接通过
        if ("default".equals(targetGroupKey)) {
            return;
        }

        // 根据存储模式校验配置是否存在
        if ("redis".equals(storageMode)) {
            // Redis 模式：检查 Key 是否存在
            String routeKey = "blink:gateway:routes:" + targetGroupKey + ":default";
            boolean exists = redisClient.exists(routeKey);
            if (!exists) {
                log.warn("[GatewayInstance] 目标分组配置不存在 | storageMode: redis, routeKey: {}", routeKey);
                BlinkException.throwBusinessException(TARGET_GROUP_CONFIG_NOT_EXIST);
            }
        } else if ("nacos".equals(storageMode)) {
            // Nacos 模式：检查配置文件是否存在
            String dataId = "gateway-routes-" + targetGroupKey + ".json";
            String config = nacosConfigComponent.getConfig(dataId, "DEFAULT_GROUP");
            if (StrUtil.isBlank(config)) {
                log.warn("[GatewayInstance] 目标分组配置不存在 | storageMode: nacos, dataId: {}", dataId);
                BlinkException.throwBusinessException(TARGET_GROUP_CONFIG_NOT_EXIST);
            }
        }

        log.info("[GatewayInstance] 目标分组配置校验通过 | targetGroupKey: {}, storageMode: {}",
                targetGroupKey, storageMode);
    }

    /**
     * 更新实例配置文件中的分组字段
     *
     * @param instanceId     实例ID
     * @param targetGroupKey 目标分组标识
     */
    private void updateInstanceGroupConfig(String instanceId, String targetGroupKey) {
        String dataId = "blink-gateway-" + instanceId + ".yaml";
        String group = "DEFAULT_GROUP";

        // 1. 获取当前配置
        String configContent = nacosConfigComponent.getConfig(dataId, group);
        if (StrUtil.isBlank(configContent)) {
            log.warn("[GatewayInstance] 实例配置文件不存在 | dataId: {}", dataId);
            BlinkException.throwBusinessException(INSTANCE_CONFIG_NOT_EXIST);
        }

        // 2. 解析 YAML 并修改 group 字段
        String updatedConfig = updateYamlGroupField(configContent, targetGroupKey);

        // 3. 发布更新后的配置
        nacosConfigComponent.configPublisher(dataId, group, updatedConfig);

        log.info("[GatewayInstance] Nacos 配置更新成功 | dataId: {}, newGroup: {}", dataId, targetGroupKey);
    }

    /**
     * 更新 YAML 配置中的 group 字段
     *
     * @param yamlContent    原始 YAML 内容
     * @param targetGroupKey 目标分组标识
     * @return 更新后的 YAML 内容
     */
    @SuppressWarnings("unchecked")
    private String updateYamlGroupField(String yamlContent, String targetGroupKey) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(yamlContent);

            if (yamlMap == null) {
                yamlMap = new LinkedHashMap<>();
            }

            // 获取或创建 blink.gateway.dynamicRoute 路径
            Map<String, Object> blinkMap = (Map<String, Object>) yamlMap.computeIfAbsent("blink", k -> new LinkedHashMap<>());
            Map<String, Object> gatewayMap = (Map<String, Object>) blinkMap.computeIfAbsent("gateway", k -> new LinkedHashMap<>());
            Map<String, Object> dynamicRouteMap = (Map<String, Object>) gatewayMap.computeIfAbsent("dynamicRoute", k -> new LinkedHashMap<>());

            // 更新 group 字段
            dynamicRouteMap.put("group", targetGroupKey);

            // 重新序列化为 YAML
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml outputYaml = new Yaml(options);

            return outputYaml.dump(yamlMap);
        } catch (Exception e) {
            log.error("[GatewayInstance] 解析 YAML 配置失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("解析配置文件失败：" + e.getMessage(), e, SWITCH_GROUP_FAILED);
        }
    }
}