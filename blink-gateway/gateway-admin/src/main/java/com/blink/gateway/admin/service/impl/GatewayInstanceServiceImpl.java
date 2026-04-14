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
import com.blink.gateway.admin.dto.req.SaveInstanceReq;
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

import static com.blink.gateway.admin.constants.ConfigValueConstant.HEALTH_CHECK_TIMEOUT_SECONDS;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_OFFLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_ONLINE;
import static com.blink.gateway.admin.constants.ConfigValueConstant.INSTANCE_STATUS_SHUTDOWN;
import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_INSTANCE_DETAIL_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GATEWAY_INSTANCE_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ALREADY_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ONLINE_CANNOT_DELETE;
import static com.blink.gateway.admin.constants.ErrCodeConstant.OFFLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ONLINE_INSTANCE_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.QUERY_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SAVE_INSTANCE_FAILED;
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
                vo.setInstanceId(instance.getInstanceId());
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
                    if (instance.getInstanceId().equals(instanceId)) {
                        GatewayInstanceVO vo = new GatewayInstanceVO();
                        vo.setInstanceId(instance.getInstanceId());
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

            // 通过 Nacos API 设置实例 enabled=false
            updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), false);

            // 更新数据库状态为下线
            instanceDO.setStatus(INSTANCE_STATUS_SHUTDOWN);
            instanceDO.setOfflineTime(LocalDateTime.now());
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
    public ResponseDTO<EmptyBody> onlineInstance(OnlineGatewayInstanceReq req) {
        try {
            String instanceId = req.getInstanceId();

            // 查询实例
            GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectById(instanceId);
            if (ObjectUtil.isNull(instanceDO)) {
                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            // 通过 Nacos API 设置实例 enabled=true
            updateInstanceEnabled(instanceDO.getHost(), instanceDO.getPort(), true);

            // 更新数据库状态为在线
            instanceDO.setStatus(INSTANCE_STATUS_ONLINE);
            instanceDO.setOnlineTime(LocalDateTime.now());
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
        Map<String, ServiceInstance> registryMap = registryInstances.stream()
                .collect(Collectors.toMap(ServiceInstance::getInstanceId, i -> i));

        // 查询数据库中的所有实例
        List<GatewayInstanceDO> dbInstances = gatewayInstanceMapper.selectList(null);

        // 更新数据库中在线实例的状态
        for (GatewayInstanceDO instanceDO : dbInstances) {
            if (instanceDO.getStatus().equals(INSTANCE_STATUS_SHUTDOWN)) {
                // 已手动下线的实例不处理
                continue;
            }

            if (registryMap.containsKey(instanceDO.getInstanceId())) {
                // 实例在注册中心，标记为在线
                if (!instanceDO.getStatus().equals(INSTANCE_STATUS_ONLINE)) {
                    instanceDO.setStatus(INSTANCE_STATUS_ONLINE);
                    instanceDO.setOnlineTime(LocalDateTime.now());
                    gatewayInstanceMapper.updateById(instanceDO);
                }
                registryMap.remove(instanceDO.getInstanceId());
            } else {
                // 实例不在注册中心，标记为离线
                if (!instanceDO.getStatus().equals(INSTANCE_STATUS_OFFLINE)) {
                    instanceDO.setStatus(INSTANCE_STATUS_OFFLINE);
                    instanceDO.setOfflineTime(LocalDateTime.now());
                    gatewayInstanceMapper.updateById(instanceDO);
                }
            }
        }

        // 新增注册中心有但数据库没有的实例
        for (ServiceInstance instance : registryMap.values()) {
            GatewayInstanceDO newInstance = new GatewayInstanceDO();
            newInstance.setInstanceId(instance.getInstanceId());
            newInstance.setServiceId(instance.getServiceId());
            newInstance.setHost(instance.getHost());
            newInstance.setPort(instance.getPort());
            newInstance.setUri(instance.getUri().toString());
            newInstance.setStatus(INSTANCE_STATUS_ONLINE);
            newInstance.setOnlineTime(LocalDateTime.now());
            gatewayInstanceMapper.insert(newInstance);
            log.info("[GatewayInstance] 新增网关实例 | instanceId: {}", instance.getInstanceId());
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
            if (instance.getInstanceId().equals(instanceId)) {
                GatewayInstanceDO instanceDO = new GatewayInstanceDO();
                instanceDO.setInstanceId(instance.getInstanceId());
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
    public ResponseDTO<EmptyBody> saveInstance(SaveInstanceReq req) {
        try {
            GatewayInstanceDO instanceDO;

            if (ObjectUtil.isNull(req.getId())) {
                // 新增实例
                instanceDO = new GatewayInstanceDO();

                // 检查是否已存在相同 host:port 的实例
                LambdaQueryWrapper<GatewayInstanceDO> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(GatewayInstanceDO::getHost, req.getHost())
                        .eq(GatewayInstanceDO::getPort, req.getPort());
                GatewayInstanceDO existInstance = gatewayInstanceMapper.selectOne(checkWrapper);
                if (ObjectUtil.isNotNull(existInstance)) {
                    BlinkException.throwBusinessException(INSTANCE_ALREADY_EXIST);
                }

                // 生成 instanceId
                String instanceId = req.getServiceId() + ":" + req.getHost() + ":" + req.getPort();
                instanceDO.setInstanceId(instanceId);
                instanceDO.setStatus(INSTANCE_STATUS_OFFLINE);

                log.info("[GatewayInstance] 新增实例 | instanceId: {}", instanceId);
            } else {
                // 编辑实例
                instanceDO = gatewayInstanceMapper.selectById(req.getId());
                if (ObjectUtil.isNull(instanceDO)) {
                    BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
                }

                // 检查 host:port 是否与其他实例冲突
                LambdaQueryWrapper<GatewayInstanceDO> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(GatewayInstanceDO::getHost, req.getHost())
                        .eq(GatewayInstanceDO::getPort, req.getPort())
                        .ne(GatewayInstanceDO::getId, req.getId());
                GatewayInstanceDO conflictInstance = gatewayInstanceMapper.selectOne(checkWrapper);
                if (ObjectUtil.isNotNull(conflictInstance)) {
                    BlinkException.throwBusinessException(INSTANCE_ALREADY_EXIST);
                }

                log.info("[GatewayInstance] 编辑实例 | id: {}", req.getId());
            }

            // 设置通用字段
            instanceDO.setServiceId(req.getServiceId());
            instanceDO.setHost(req.getHost());
            instanceDO.setPort(req.getPort());
            instanceDO.setUri("http://" + req.getHost() + ":" + req.getPort());
            instanceDO.setMetadata(req.getMetadata());

            // 保存或更新
            if (ObjectUtil.isNull(req.getId())) {
                gatewayInstanceMapper.insert(instanceDO);
            } else {
                gatewayInstanceMapper.updateById(instanceDO);
            }

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GatewayInstance] 保存实例失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("保存实例失败：" + e.getMessage(), e, SAVE_INSTANCE_FAILED);
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