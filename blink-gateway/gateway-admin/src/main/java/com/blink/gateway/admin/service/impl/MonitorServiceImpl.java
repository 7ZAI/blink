package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.dto.req.GetGatewayMetricsReq;
import com.blink.gateway.admin.dto.req.QueryGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.QueryHealthStatusReq;
import com.blink.gateway.admin.dto.req.QueryStatisticsReq;
import com.blink.gateway.admin.dto.rsp.GatewayHealthStatusRsp;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.rsp.GatewayMetricsRsp;
import com.blink.gateway.admin.dto.rsp.GatewayStatisticsRsp;
import com.blink.gateway.admin.dto.vo.GatewayHealthStatusVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.vo.GatewayMetricsVO;
import com.blink.gateway.admin.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_INSTANCE_LIST_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GET_METRICS_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.GATEWAY_INSTANCE_NOT_EXIST;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_PREFIX;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_METRICS_SUMMARY;

/**
 * 网关监控服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private RedisClient redisClient;

    private static final String GATEWAY_SERVICE_NAME = "gateway-reactive";

    @Override
    public ResponseDTO<GatewayInstanceListRsp> getGatewayInstances(QueryGatewayInstanceReq req) {
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
                vo.setStatus((byte) 0);
                vo.setStatusDesc("在线");
                instanceList.add(vo);
            }

            GatewayInstanceListRsp rsp = new GatewayInstanceListRsp();
            rsp.setTotal(instanceList.size());
            rsp.setInstances(instanceList);

            log.info("[Monitor] 获取网关实例列表成功 | total: {}", instanceList.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Monitor] 获取网关实例列表失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关实例列表失败：" + e.getMessage(), e, GET_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    public ResponseDTO<GatewayStatisticsRsp> getStatistics(QueryStatisticsReq req) {
        try {
            // 从 Redis 读取汇总统计
            Map<String, Object> summary = redisClient.hGetStringMap(GATEWAY_METRICS_SUMMARY);

            GatewayStatisticsRsp statistics = new GatewayStatisticsRsp();

            if (MapUtil.isNotEmpty(summary)) {
                // 从 Redis 缓存读取
                statistics.setTotalInstances(getIntValue(summary, "totalInstances"));
                statistics.setHealthyInstances(getIntValue(summary, "healthyInstances"));
                statistics.setTotalRequests(getLongValue(summary, "totalRequests"));
                statistics.setSuccessRequests(getLongValue(summary, "totalSuccessRequests"));
                statistics.setFailedRequests(getLongValue(summary, "totalFailedRequests"));
                statistics.setAvgResponseTime(getLongValue(summary, "avgResponseTime"));

                log.debug("[Monitor] 从 Redis 读取统计数据成功");
            } else {
                // Redis 无数据时，从注册中心获取基础信息
                List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
                statistics.setTotalInstances(instances.size());
                statistics.setHealthyInstances(instances.size());
                statistics.setTotalRequests(0L);
                statistics.setSuccessRequests(0L);
                statistics.setFailedRequests(0L);
                statistics.setAvgResponseTime(0L);

                log.debug("[Monitor] Redis 无数据，使用注册中心信息");
            }

            log.info("[Monitor] 获取网关统计数据成功 | totalInstances: {}", statistics.getTotalInstances());

            return ResponseDTO.newSuccessInstance(statistics);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Monitor] 获取网关统计数据失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关统计数据失败：" + e.getMessage(), e, GET_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    public ResponseDTO<GatewayHealthStatusRsp> getHealthStatus(QueryHealthStatusReq req) {
        try {
            String instanceId = req.getInstanceId();

            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<GatewayHealthStatusVO> healthStatusList = new ArrayList<>();

            // 返回所有实例健康状态
            if (StrUtil.isBlank(instanceId)) {
                for (ServiceInstance instance : instances) {
                    GatewayHealthStatusVO healthStatus = new GatewayHealthStatusVO();
                    healthStatus.setInstanceId(instance.getInstanceId());
                    healthStatus.setHost(instance.getHost());
                    healthStatus.setPort(instance.getPort());
                    healthStatus.setStatus("UP");
                    healthStatusList.add(healthStatus);
                }

                GatewayHealthStatusRsp rsp = new GatewayHealthStatusRsp();
                rsp.setHealthStatusList(healthStatusList);

                log.info("[Monitor] 获取所有实例健康状态成功 | count: {}", healthStatusList.size());

                return ResponseDTO.newSuccessInstance(rsp);
            } else {
                // 返回指定实例健康状态
                for (ServiceInstance instance : instances) {
                    if (instance.getInstanceId().equals(instanceId)) {
                        GatewayHealthStatusVO healthStatus = new GatewayHealthStatusVO();
                        healthStatus.setInstanceId(instance.getInstanceId());
                        healthStatus.setHost(instance.getHost());
                        healthStatus.setPort(instance.getPort());
                        healthStatus.setStatus("UP");

                        GatewayHealthStatusRsp rsp = new GatewayHealthStatusRsp();
                        rsp.setHealthStatusList(List.of(healthStatus));

                        log.info("[Monitor] 获取指定实例健康状态成功 | instanceId: {}", instanceId);

                        return ResponseDTO.newSuccessInstance(rsp);
                    }
                }

                BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
            }

            GatewayHealthStatusRsp rsp = new GatewayHealthStatusRsp();
            rsp.setHealthStatusList(healthStatusList);
            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Monitor] 获取网关健康状态失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关健康状态失败：" + e.getMessage(), e, GET_INSTANCE_LIST_FAILED);
        }
    }

    @Override
    public ResponseDTO<GatewayMetricsRsp> getGatewayMetrics(GetGatewayMetricsReq req) {
        try {
            String instanceId = req.getInstanceId();
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);

            List<GatewayMetricsVO> metricsList = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                if (StrUtil.isNotBlank(instanceId) && !instance.getInstanceId().equals(instanceId)) {
                    continue;
                }

                // 从 Redis 读取指标数据
                String redisKey = GATEWAY_METRICS_PREFIX + instance.getInstanceId();
                Map<String, Object> metricsData = redisClient.hGetStringMap(redisKey);

                GatewayMetricsVO metrics = new GatewayMetricsVO();
                metrics.setInstanceId(instance.getInstanceId());
                metrics.setHost(instance.getHost());
                metrics.setPort(instance.getPort());

                if (MapUtil.isNotEmpty(metricsData)) {
                    // 从 Redis 缓存填充数据
                    metrics.setCpuUsage(getDoubleValue(metricsData, "cpuUsage"));
                    metrics.setMemoryUsage(calculateMemoryUsage(metricsData));
                    metrics.setTotalRequests(getLongValue(metricsData, "totalRequests"));
                    metrics.setSuccessRequests(getLongValue(metricsData, "successRequests"));
                    metrics.setFailedRequests(getLongValue(metricsData, "failedRequests"));
                    metrics.setAvgResponseTime(getLongValue(metricsData, "avgResponseTime"));
                    metrics.setActiveConnections(0); // 暂不支持
                    metrics.setTimestamp(getLongValue(metricsData, "timestamp"));

                    log.debug("[Monitor] 从 Redis 读取实例指标 | instanceId: {}", instance.getInstanceId());
                } else {
                    // Redis 无数据时返回默认值
                    metrics.setCpuUsage(0.0);
                    metrics.setMemoryUsage(0.0);
                    metrics.setTotalRequests(0L);
                    metrics.setSuccessRequests(0L);
                    metrics.setFailedRequests(0L);
                    metrics.setAvgResponseTime(0L);
                    metrics.setActiveConnections(0);
                    metrics.setTimestamp(System.currentTimeMillis());

                    log.debug("[Monitor] Redis 无数据，使用默认值 | instanceId: {}", instance.getInstanceId());
                }

                metricsList.add(metrics);
            }

            GatewayMetricsRsp rsp = new GatewayMetricsRsp();
            rsp.setMetricsList(metricsList);

            log.info("[Monitor] 获取网关指标成功 | count: {}", metricsList.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Monitor] 获取网关指标失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("获取网关指标失败：" + e.getMessage(), e, GET_METRICS_FAILED);
        }
    }

    /**
     * 计算内存使用率
     */
    private Double calculateMemoryUsage(Map<String, Object> metricsData) {
        Long memoryUsed = getLongValue(metricsData, "memoryUsed");
        Long memoryMax = getLongValue(metricsData, "memoryMax");
        if (ObjectUtil.isNotNull(memoryUsed) && ObjectUtil.isNotNull(memoryMax) && memoryMax > 0) {
            return (memoryUsed * 100.0) / memoryMax;
        }
        return 0.0;
    }

    /**
     * 从 Map 中获取 Integer 值
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 从 Map 中获取 Long 值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * 从 Map 中获取 Double 值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}