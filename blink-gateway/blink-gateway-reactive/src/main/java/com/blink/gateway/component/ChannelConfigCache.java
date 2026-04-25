package com.blink.gateway.component;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 * 渠道配置本地缓存
 * 从 Nacos 加载渠道配置并缓存到本地
 * 注意：渠道配置不加密，仅包含业务字段
 *
 * @author binblink
 */
@Component
@Slf4j
public class ChannelConfigCache {

    @Resource
    private NacosConfigManager nacosConfigManager;

    /**
     * 渠道配置缓存
     * key: appKey, value: 渠道信息
     */
    private final Map<String, ChannelInfoRedisDO> channelInfoMap = new ConcurrentHashMap<>();

    /**
     * 启动时从 Nacos 加载配置
     */
    @PostConstruct
    public void init() {
        try {
            loadConfigFromNacos();
            log.info("[ChannelConfigCache] 初始化成功 | count: {}", channelInfoMap.size());
        } catch (Exception e) {
            log.error("[ChannelConfigCache] 初始化失败 | error: {}", e.getMessage(), e);
            // 初始化失败不影响启动，后续可通过刷新机制加载
        }
    }

    /**
     * 从 Nacos 加载配置（不加密）
     */
    private void loadConfigFromNacos() {
        String configStr = null;
        try {
            configStr = nacosConfigManager.getConfigService().getConfig(
                    CHANNEL_CONFIG_DATA_ID, CHANNEL_CONFIG_GROUP, NACOS_CHANNEL_CONFIG_TIMEOUT_MS
            );
        } catch (Exception e) {
            log.error("[ChannelConfigCache] 获取配置失败 | error: {}", e.getMessage(), e);
            throw new BlinkException(e, e.getMessage());
        }

        // 配置为空，跳过加载
        if (StrUtil.isBlank(configStr)) {
            log.warn("[ChannelConfigCache] Nacos 配置为空，跳过加载 | dataId: {}", CHANNEL_CONFIG_DATA_ID);
            return;
        }

        // 解析 JSON（不解密）
        List<Map<String, Object>> configList = JacksonUtil.fromJsonToList(configStr, Map.class);
        if (configList == null || configList.isEmpty()) {
            log.warn("[ChannelConfigCache] 配置内容为空");
            return;
        }

        // 清空旧缓存
        channelInfoMap.clear();

        // 加载到缓存
        for (Map<String, Object> configMap : configList) {
            ChannelInfoRedisDO channelInfo = new ChannelInfoRedisDO();
            channelInfo.setChannelId((String) configMap.get("channelId"));
            channelInfo.setChannelName((String) configMap.get("channelName"));
            channelInfo.setAppKey((String) configMap.get("appKey"));
            channelInfo.setRelaUserId((String) configMap.get("relaUserId"));
            channelInfo.setAccessToken((String) configMap.get("accessToken"));

            // 处理 Byte 类型字段
            channelInfo.setEnable(parseByte(configMap.get("enable")));
            channelInfo.setEncryptionSwitch(parseByte(configMap.get("encryptionSwitch")));
            channelInfo.setTokenType(parseByte(configMap.get("tokenType")));
            channelInfo.setAuthoritySwitch(parseByte(configMap.get("authoritySwitch")));

            channelInfo.setRemark((String) configMap.get("remark"));

            if (channelInfo.getAppKey() != null) {
                channelInfoMap.put(channelInfo.getAppKey(), channelInfo);
            }
        }

        log.info("[ChannelConfigCache] 加载配置成功 | count: {}", channelInfoMap.size());
    }

    /**
     * 解析 Byte 类型
     */
    private Byte parseByte(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Byte) {
            return (Byte) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).byteValue();
        }
        if (value instanceof String) {
            try {
                return Byte.parseByte((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取渠道信息
     *
     * @param appKey 渠道标识
     * @return 渠道信息，不存在返回 null
     */
    public ChannelInfoRedisDO getChannelInfo(String appKey) {
        return channelInfoMap.get(appKey);
    }

    /**
     * 全量刷新配置（从 Nacos 重新加载）
     */
    public void refreshAll() {
        log.info("[ChannelConfigCache] 开始全量刷新配置");
        try {
            loadConfigFromNacos();
            log.info("[ChannelConfigCache] 全量刷新成功 | count: {}", channelInfoMap.size());
        } catch (Exception e) {
            log.error("[ChannelConfigCache] 全量刷新失败 | error: {}", e.getMessage(), e);
        }
    }

    /**
     * 增量刷新单个渠道
     * 从 Nacos 重新加载指定渠道的配置
     *
     * @param appKey 渠道标识
     */
    public void refreshSingle(String appKey) {
        log.info("[ChannelConfigCache] 开始增量刷新 | appKey: {}", appKey);
        try {
            // 从 Nacos 获取最新配置
            String configStr = nacosConfigManager.getConfigService().getConfig(
                    CHANNEL_CONFIG_DATA_ID, CHANNEL_CONFIG_GROUP, NACOS_CHANNEL_CONFIG_TIMEOUT_MS
            );

            if (StrUtil.isBlank(configStr)) {
                log.warn("[ChannelConfigCache] Nacos 配置为空，跳过刷新 | appKey: {}", appKey);
                return;
            }

            // 解析 JSON（不解密）
            List<Map<String, Object>> configList = JacksonUtil.fromJsonToList(configStr, Map.class);
            if (configList == null) {
                log.warn("[ChannelConfigCache] 配置内容为空，跳过刷新 | appKey: {}", appKey);
                return;
            }

            // 查找并更新指定渠道
            for (Map<String, Object> configMap : configList) {
                String key = (String) configMap.get("appKey");
                if (appKey.equals(key)) {
                    ChannelInfoRedisDO channelInfo = new ChannelInfoRedisDO();
                    channelInfo.setChannelId((String) configMap.get("channelId"));
                    channelInfo.setChannelName((String) configMap.get("channelName"));
                    channelInfo.setAppKey(appKey);
                    channelInfo.setRelaUserId((String) configMap.get("relaUserId"));
                    channelInfo.setAccessToken((String) configMap.get("accessToken"));
                    channelInfo.setEnable(parseByte(configMap.get("enable")));
                    channelInfo.setEncryptionSwitch(parseByte(configMap.get("encryptionSwitch")));
                    channelInfo.setTokenType(parseByte(configMap.get("tokenType")));
                    channelInfo.setAuthoritySwitch(parseByte(configMap.get("authoritySwitch")));
                    channelInfo.setRemark((String) configMap.get("remark"));

                    channelInfoMap.put(appKey, channelInfo);
                    log.info("[ChannelConfigCache] 增量刷新成功 | appKey: {}, channelName: {}", appKey, channelInfo.getChannelName());
                    return;
                }
            }

            log.warn("[ChannelConfigCache] 未找到渠道配置 | appKey: {}", appKey);

        } catch (Exception e) {
            log.error("[ChannelConfigCache] 增量刷新失败 | appKey: {}, error: {}", appKey, e.getMessage(), e);
        }
    }

    /**
     * 删除本地缓存
     *
     * @param appKey 渠道标识
     */
    public void evict(String appKey) {
        ChannelInfoRedisDO removed = channelInfoMap.remove(appKey);
        if (removed != null) {
            log.info("[ChannelConfigCache] 删除缓存成功 | appKey: {}", appKey);
        } else {
            log.warn("[ChannelConfigCache] 缓存不存在，无需删除 | appKey: {}", appKey);
        }
    }

    /**
     * 获取所有渠道信息
     *
     * @return 渠道信息列表
     */
    public List<ChannelInfoRedisDO> getAllChannelInfo() {
        return new ArrayList<>(channelInfoMap.values());
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的渠道数量
     */
    public int size() {
        return channelInfoMap.size();
    }
}
