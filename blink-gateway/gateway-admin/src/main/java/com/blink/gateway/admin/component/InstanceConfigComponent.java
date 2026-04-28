package com.blink.gateway.admin.component;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.constants.RouteConstant;
import com.blink.gateway.admin.dto.vo.DynamicRouteConfigVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceConfigVO;
import com.blink.gateway.admin.dto.vo.InstanceConfigFileVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关实例配置文件操作组件
 * 基于 Spring Cloud Alibaba Nacos Config API 实现配置文件的读取、修改、保存
 *
 * @author binblink
 * @since 2026-04-26
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = false)
public class InstanceConfigComponent {

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String defaultGroup;

    /**
     * 配置读取超时时间（毫秒）
     */
    private static final long CONFIG_TIMEOUT_MS = 5000L;

    /**
     * 构建实例配置文件的 DataId
     *
     * @param instanceId 实例ID
     * @return DataId
     */
    public String buildDataId(String instanceId) {
        return RouteConstant.INSTANCE_CONFIG_PREFIX + instanceId + RouteConstant.INSTANCE_CONFIG_SUFFIX;
    }

    /**
     * 检查实例配置文件是否存在
     *
     * @param instanceId 实例ID
     * @return 是否存在
     */
    public boolean existsConfig(String instanceId) {
        return existsConfig(instanceId, defaultGroup);
    }

    /**
     * 检查实例配置文件是否存在
     *
     * @param instanceId 实例ID
     * @param group      配置分组
     * @return 是否存在
     */
    public boolean existsConfig(String instanceId, String group) {
        try {
            String dataId = buildDataId(instanceId);
            String content = getConfigService().getConfig(dataId, group, CONFIG_TIMEOUT_MS);
            return StrUtil.isNotBlank(content);
        } catch (NacosException e) {
            log.warn("[InstanceConfig] 检查配置文件存在性失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取实例配置文件
     *
     * @param instanceId 实例ID
     * @return 配置文件 VO
     */
    public InstanceConfigFileVO getConfig(String instanceId) {
        return getConfig(instanceId, defaultGroup);
    }

    /**
     * 获取实例配置文件
     *
     * @param instanceId 实例ID
     * @param group      配置分组
     * @return 配置文件 VO
     */
    public InstanceConfigFileVO getConfig(String instanceId, String group) {
        InstanceConfigFileVO result = new InstanceConfigFileVO();
        result.setInstanceId(instanceId);
        result.setGroup(group);
        result.setDataId(buildDataId(instanceId));

        try {
            ConfigService configService = getConfigService();
            String content = configService.getConfig(result.getDataId(), group, CONFIG_TIMEOUT_MS);

            if (StrUtil.isBlank(content)) {
                result.setExists(false);
                result.setSourceDesc("配置文件不存在");
                log.debug("[InstanceConfig] 实例配置文件不存在 | instanceId: {}, dataId: {}", instanceId, result.getDataId());
                return result;
            }

            result.setExists(true);
            result.setContent(content);
            result.setSourceDesc("配置文件");

            // 解析配置
            GatewayInstanceConfigVO configVO = parseConfig(content);
            result.setConfig(configVO);

            log.debug("[InstanceConfig] 获取实例配置文件成功 | instanceId: {}, dataId: {}", instanceId, result.getDataId());

        } catch (NacosException e) {
            log.error("[InstanceConfig] 获取实例配置文件失败 | instanceId: {}, error: {}", instanceId, e.getMessage(), e);
            result.setExists(false);
            result.setSourceDesc("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取配置原始内容
     *
     * @param instanceId 实例ID
     * @return 配置内容，不存在返回 null
     */
    public String getConfigContent(String instanceId) {
        return getConfigContent(instanceId, defaultGroup);
    }

    /**
     * 获取配置原始内容
     *
     * @param instanceId 实例ID
     * @param group      配置分组
     * @return 配置内容，不存在返回 null
     */
    public String getConfigContent(String instanceId, String group) {
        try {
            String dataId = buildDataId(instanceId);
            return getConfigService().getConfig(dataId, group, CONFIG_TIMEOUT_MS);
        } catch (NacosException e) {
            log.warn("[InstanceConfig] 获取配置内容失败 | instanceId: {}, error: {}", instanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 保存配置文件内容
     *
     * @param instanceId 实例ID
     * @param content    配置内容（YAML 格式）
     * @return 是否成功
     */
    public boolean saveConfig(String instanceId, String content) {
        return saveConfig(instanceId, defaultGroup, content);
    }

    /**
     * 保存配置文件内容
     *
     * @param instanceId 实例ID
     * @param group      配置分组
     * @param content    配置内容（YAML 格式）
     * @return 是否成功
     */
    public boolean saveConfig(String instanceId, String group, String content) {
        try {
            String dataId = buildDataId(instanceId);
            ConfigService configService = getConfigService();

            boolean success = configService.publishConfig(dataId, group, content, "yaml");

            if (success) {
                log.info("[InstanceConfig] 保存实例配置文件成功 | instanceId: {}, dataId: {}, group: {}",
                    instanceId, dataId, group);
            } else {
                log.warn("[InstanceConfig] 保存实例配置文件失败 | instanceId: {}, dataId: {}", instanceId, dataId);
            }

            return success;

        } catch (NacosException e) {
            log.error("[InstanceConfig] 保存实例配置文件异常 | instanceId: {}, error: {}", instanceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新动态路由配置
     * 如果配置文件不存在，则创建默认配置
     *
     * @param instanceId      实例ID
     * @param routeMode       路由模式（redis/nacos）
     * @param routeGroup      路由分组
     * @param redisSuffix     Redis 路由后缀
     * @param nacosDataId     Nacos DataId
     * @param nacosGroup      Nacos Group
     * @return 是否成功
     */
    public boolean updateDynamicRouteConfig(String instanceId, String routeMode, String routeGroup,
                                            String redisSuffix, String nacosDataId, String nacosGroup) {
        try {
            // 获取现有配置
            String existingContent = getConfigContent(instanceId);

            Yaml yaml = createYaml();
            Map<String, Object> configMap;

            if (StrUtil.isNotBlank(existingContent)) {
                // 解析现有配置
                configMap = yaml.load(existingContent);
                if (configMap == null) {
                    configMap = new LinkedHashMap<>();
                }
            } else {
                // 创建默认配置结构
                configMap = createDefaultConfigMap();
            }

            // 更新 dynamicRoute 配置
            updateDynamicRouteInMap(configMap, routeMode, routeGroup, redisSuffix, nacosDataId, nacosGroup);

            // 转换为 YAML 字符串
            String newContent = yaml.dump(configMap);

            // 保存配置
            return saveConfig(instanceId, newContent);

        } catch (Exception e) {
            log.error("[InstanceConfig] 更新动态路由配置失败 | instanceId: {}, error: {}", instanceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除实例配置文件
     *
     * @param instanceId 实例ID
     * @return 是否成功
     */
    public boolean deleteConfig(String instanceId) {
        return deleteConfig(instanceId, defaultGroup);
    }

    /**
     * 删除实例配置文件
     *
     * @param instanceId 实例ID
     * @param group      配置分组
     * @return 是否成功
     */
    public boolean deleteConfig(String instanceId, String group) {
        try {
            String dataId = buildDataId(instanceId);
            ConfigService configService = getConfigService();

            boolean success = configService.removeConfig(dataId, group);

            if (success) {
                log.info("[InstanceConfig] 删除实例配置文件成功 | instanceId: {}, dataId: {}", instanceId, dataId);
            } else {
                log.warn("[InstanceConfig] 删除实例配置文件失败 | instanceId: {}, dataId: {}", instanceId, dataId);
            }

            return success;

        } catch (NacosException e) {
            log.error("[InstanceConfig] 删除实例配置文件异常 | instanceId: {}, error: {}", instanceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析配置内容为 VO 对象
     *
     * @param content YAML 配置内容
     * @return 配置 VO
     */
    @SuppressWarnings("unchecked")
    public GatewayInstanceConfigVO parseConfig(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }

        try {
            Yaml yaml = createYaml();
            Map<String, Object> configMap = yaml.load(content);

            if (configMap == null) {
                return null;
            }

            GatewayInstanceConfigVO vo = new GatewayInstanceConfigVO();

            // 提取 blink.gateway 配置
            Map<String, Object> blinkMap = (Map<String, Object>) configMap.get("blink");
            if (blinkMap == null) {
                return vo;
            }

            Map<String, Object> gatewayMap = (Map<String, Object>) blinkMap.get("gateway");
            if (gatewayMap == null) {
                return vo;
            }

            // 解析基本字段
            vo.setInstanceId((String) gatewayMap.get("instance-id"));
            vo.setInstanceEnabled((Boolean) gatewayMap.get("instance-enabled"));
            vo.setMaintenanceMode((Boolean) gatewayMap.get("maintenance-mode"));
            vo.setInstanceWeight((Integer) gatewayMap.get("instance-weight"));
            vo.setLocalCacheEnable((Boolean) gatewayMap.get("localCacheEnable"));
            vo.setEventStreamEnable((Boolean) gatewayMap.get("eventStreamEnable"));

            // 解析 dynamicRoute
            Map<String, Object> dynamicRouteMap = (Map<String, Object>) gatewayMap.get("dynamicRoute");
            if (dynamicRouteMap != null) {
                DynamicRouteConfigVO dynamicRouteVO = new DynamicRouteConfigVO();
                dynamicRouteVO.setMode((String) dynamicRouteMap.get("mode"));
                dynamicRouteVO.setGroup((String) dynamicRouteMap.get("group"));

                // Redis 配置
                Map<String, Object> redisMap = (Map<String, Object>) dynamicRouteMap.get("redis");
                if (redisMap != null) {
                    DynamicRouteConfigVO.RedisConfigVO redisVO = new DynamicRouteConfigVO.RedisConfigVO();
                    redisVO.setRouteSuffix((String) redisMap.get("routeSuffix"));
                    dynamicRouteVO.setRedis(redisVO);
                }

                // Nacos 配置
                Map<String, Object> nacosMap = (Map<String, Object>) dynamicRouteMap.get("nacos");
                if (nacosMap != null) {
                    DynamicRouteConfigVO.NacosConfigVO nacosVO = new DynamicRouteConfigVO.NacosConfigVO();
                    nacosVO.setDataId((String) nacosMap.get("dataId"));
                    nacosVO.setGroup((String) nacosMap.get("group"));
                    dynamicRouteVO.setNacos(nacosVO);
                }

                vo.setDynamicRoute(dynamicRouteVO);
            }

            // 解析 signature
            Map<String, Object> signatureMap = (Map<String, Object>) gatewayMap.get("signature");
            if (signatureMap != null) {
                GatewayInstanceConfigVO.SignatureConfigVO signatureVO = new GatewayInstanceConfigVO.SignatureConfigVO();
                signatureVO.setEnable((Boolean) signatureMap.get("enable"));
                vo.setSignature(signatureVO);
            }

            // 解析 replayDefend
            Map<String, Object> replayDefendMap = (Map<String, Object>) gatewayMap.get("replay-defend");
            if (replayDefendMap != null) {
                GatewayInstanceConfigVO.ReplayDefendConfigVO replayDefendVO = new GatewayInstanceConfigVO.ReplayDefendConfigVO();
                replayDefendVO.setEnable((Boolean) replayDefendMap.get("enable"));
                vo.setReplayDefend(replayDefendVO);
            }

            return vo;

        } catch (Exception e) {
            log.warn("[InstanceConfig] 解析配置内容失败 | error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 ConfigService
     */
    private ConfigService getConfigService() throws NacosException {
        return nacosConfigManager.getConfigService();
    }

    /**
     * 创建 YAML 解析器
     */
    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }

    /**
     * 创建默认配置 Map
     */
    private Map<String, Object> createDefaultConfigMap() {
        Map<String, Object> configMap = new LinkedHashMap<>();

        Map<String, Object> blinkMap = new LinkedHashMap<>();
        Map<String, Object> gatewayMap = new LinkedHashMap<>();

        // 默认配置
        gatewayMap.put("instance-enabled", true);
        gatewayMap.put("maintenance-mode", false);
        gatewayMap.put("instance-weight", 100);
        gatewayMap.put("localCacheEnable", true);
        gatewayMap.put("eventStreamEnable", true);

        // 默认动态路由配置
        Map<String, Object> dynamicRouteMap = new LinkedHashMap<>();
        dynamicRouteMap.put("mode", "redis");
        dynamicRouteMap.put("group", "default");

        Map<String, Object> redisMap = new LinkedHashMap<>();
        redisMap.put("routeSuffix", "default");
        dynamicRouteMap.put("redis", redisMap);

        gatewayMap.put("dynamicRoute", dynamicRouteMap);

        // 默认签名配置
        Map<String, Object> signatureMap = new LinkedHashMap<>();
        signatureMap.put("enable", true);
        gatewayMap.put("signature", signatureMap);

        // 默认重放防御配置
        Map<String, Object> replayDefendMap = new LinkedHashMap<>();
        replayDefendMap.put("enable", true);
        gatewayMap.put("replay-defend", replayDefendMap);

        blinkMap.put("gateway", gatewayMap);
        configMap.put("blink", blinkMap);

        return configMap;
    }

    /**
     * 更新 Map 中的 dynamicRoute 配置
     */
    @SuppressWarnings("unchecked")
    private void updateDynamicRouteInMap(Map<String, Object> configMap, String routeMode, String routeGroup,
                                         String redisSuffix, String nacosDataId, String nacosGroup) {
        Map<String, Object> blinkMap = (Map<String, Object>) configMap.computeIfAbsent("blink", k -> new LinkedHashMap<>());
        Map<String, Object> gatewayMap = (Map<String, Object>) blinkMap.computeIfAbsent("gateway", k -> new LinkedHashMap<>());

        Map<String, Object> dynamicRouteMap = (Map<String, Object>) gatewayMap.computeIfAbsent("dynamicRoute", k -> new LinkedHashMap<>());

        // 更新基本配置
        dynamicRouteMap.put("mode", routeMode);
        dynamicRouteMap.put("group", routeGroup);

        // 更新 Redis 配置
        if ("redis".equals(routeMode) || StrUtil.isNotBlank(redisSuffix)) {
            Map<String, Object> redisMap = (Map<String, Object>) dynamicRouteMap.computeIfAbsent("redis", k -> new LinkedHashMap<>());
            if (StrUtil.isNotBlank(redisSuffix)) {
                redisMap.put("routeSuffix", redisSuffix);
            }
        }

        // 更新 Nacos 配置
        if ("nacos".equals(routeMode) || StrUtil.isNotBlank(nacosDataId)) {
            Map<String, Object> nacosMap = (Map<String, Object>) dynamicRouteMap.computeIfAbsent("nacos", k -> new LinkedHashMap<>());
            if (StrUtil.isNotBlank(nacosDataId)) {
                nacosMap.put("dataId", nacosDataId);
            }
            if (StrUtil.isNotBlank(nacosGroup)) {
                nacosMap.put("group", nacosGroup);
            }
        }
    }
}
