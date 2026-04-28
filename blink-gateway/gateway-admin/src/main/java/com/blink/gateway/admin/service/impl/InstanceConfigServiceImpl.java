package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.component.InstanceConfigComponent;
import com.blink.gateway.admin.dto.req.GetInstanceConfigReq;
import com.blink.gateway.admin.dto.req.SaveInstanceConfigContentReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceConfigReq;
import com.blink.gateway.admin.dto.vo.InstanceConfigFileVO;
import com.blink.gateway.admin.service.InstanceConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_CONFIG_NOT_EXIST;

/**
 * 实例配置文件服务实现
 *
 * @author binblink
 * @since 2026-04-26
 */
@Service
@Slf4j
public class InstanceConfigServiceImpl implements InstanceConfigService {

    @Resource
    private InstanceConfigComponent instanceConfigComponent;

    @Override
    public ResponseDTO<InstanceConfigFileVO> getInstanceConfig(GetInstanceConfigReq req) {
        InstanceConfigFileVO configVO = instanceConfigComponent.getConfig(req.getInstanceId());

        log.info("[InstanceConfig] 获取实例配置文件 | instanceId: {}, exists: {}",
            req.getInstanceId(), configVO.getExists());

        return ResponseDTO.newSuccessInstance(configVO);
    }

    @Override
    public ResponseDTO<EmptyBody> updateInstanceConfig(UpdateInstanceConfigReq req) {
        // 参数校验
        if (!"redis".equals(req.getRouteMode()) && !"nacos".equals(req.getRouteMode())) {
            BlinkException.throwBusinessException("路由模式只能是 redis 或 nacos");
        }

        // 更新配置
        boolean success = instanceConfigComponent.updateDynamicRouteConfig(
            req.getInstanceId(),
            req.getRouteMode(),
            req.getRouteGroup(),
            req.getRedisRouteSuffix(),
            req.getNacosDataId(),
            req.getNacosGroup()
        );

        if (!success) {
            BlinkException.throwBusinessException("更新实例配置文件失败");
        }

        log.info("[InstanceConfig] 更新实例配置文件成功 | instanceId: {}, routeMode: {}, routeGroup: {}",
            req.getInstanceId(), req.getRouteMode(), req.getRouteGroup());

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<EmptyBody> saveInstanceConfigContent(SaveInstanceConfigContentReq req) {
        // 校验 YAML 格式
        if (!isValidYaml(req.getContent())) {
            BlinkException.throwBusinessException("配置内容格式错误，不是有效的 YAML 格式");
        }

        // 保存配置
        boolean success = instanceConfigComponent.saveConfig(req.getInstanceId(), req.getContent());

        if (!success) {
            BlinkException.throwBusinessException("保存实例配置文件失败");
        }

        log.info("[InstanceConfig] 保存实例配置文件成功 | instanceId: {}, remark: {}",
            req.getInstanceId(), req.getRemark());

        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 校验 YAML 格式是否有效
     */
    private boolean isValidYaml(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            yaml.load(content);
            return true;
        } catch (Exception e) {
            log.warn("[InstanceConfig] YAML 格式校验失败 | error: {}", e.getMessage());
            return false;
        }
    }
}
