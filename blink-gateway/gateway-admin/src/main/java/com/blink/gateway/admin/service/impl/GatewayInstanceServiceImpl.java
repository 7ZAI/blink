package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.GetGatewayInstanceDetailReq;
import com.blink.gateway.admin.dto.req.OfflineGatewayInstanceReq;
import com.blink.gateway.admin.dto.req.OnlineGatewayInstanceReq;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.service.GatewayInstanceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;

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

    private static final String GATEWAY_SERVICE_NAME = "gateway-reactive";

    private static final Byte STATUS_ONLINE = 0;
    private static final Byte STATUS_OFFLINE = 1;
    private static final Byte STATUS_SHUTDOWN = 2;

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
                vo.setStatus(STATUS_ONLINE);
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
                        vo.setStatus(STATUS_ONLINE);
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

            // 更新状态为下线
            instanceDO.setStatus(STATUS_SHUTDOWN);
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

            // 更新状态为在线
            instanceDO.setStatus(STATUS_ONLINE);
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

    @Override
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncInstanceStatus() {
        try {
            log.info("[GatewayInstance] 开始同步网关实例状态...");

            // 获取注册中心的所有实例
            List<ServiceInstance> registryInstances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            Map<String, ServiceInstance> registryMap = registryInstances.stream()
                    .collect(Collectors.toMap(ServiceInstance::getInstanceId, i -> i));

            // 查询数据库中的所有实例
            List<GatewayInstanceDO> dbInstances = gatewayInstanceMapper.selectList(null);

            // 更新数据库中在线实例的状态
            for (GatewayInstanceDO instanceDO : dbInstances) {
                if (instanceDO.getStatus().equals(STATUS_SHUTDOWN)) {
                    // 已手动下线的实例不处理
                    continue;
                }

                if (registryMap.containsKey(instanceDO.getInstanceId())) {
                    // 实例在注册中心，标记为在线
                    if (!instanceDO.getStatus().equals(STATUS_ONLINE)) {
                        instanceDO.setStatus(STATUS_ONLINE);
                        instanceDO.setOnlineTime(LocalDateTime.now());
                        gatewayInstanceMapper.updateById(instanceDO);
                    }
                    registryMap.remove(instanceDO.getInstanceId());
                } else {
                    // 实例不在注册中心，标记为离线
                    if (!instanceDO.getStatus().equals(STATUS_OFFLINE)) {
                        instanceDO.setStatus(STATUS_OFFLINE);
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
                newInstance.setStatus(STATUS_ONLINE);
                newInstance.setOnlineTime(LocalDateTime.now());
                gatewayInstanceMapper.insert(newInstance);
                log.info("[GatewayInstance] 新增网关实例 | instanceId: {}", instance.getInstanceId());
            }

            log.info("[GatewayInstance] 网关实例状态同步完成");
        } catch (Exception e) {
            log.error("[GatewayInstance] 同步网关实例状态失败 | error: {}", e.getMessage(), e);
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
                instanceDO.setStatus(STATUS_ONLINE);
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
            case 0 -> "在线";
            case 1 -> "离线";
            case 2 -> "下线";
            default -> "未知";
        };
    }
}