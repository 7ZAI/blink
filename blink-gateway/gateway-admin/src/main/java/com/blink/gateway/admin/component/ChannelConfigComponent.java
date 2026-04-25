package com.blink.gateway.admin.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blink.framework.common.data.ChannelInfoNacosDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.NacosConfigConstant.*;

/**
 * 渠道配置管理组件
 * 负责管理渠道信息配置，配置存储在 Nacos 配置中心
 * 注意：渠道配置不加密，仅包含业务字段，敏感信息（密钥）单独存储在 secretConfig.json
 *
 * @author binblink
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = false)
public class ChannelConfigComponent implements CommandLineRunner {

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private GaChannelMapper channelMapper;

    /**
     * 渠道配置缓存
     */
    private final Map<String, ChannelInfoNacosDO> CACHE = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        String configStr = nacosConfigComponent.getConfig(CHANNEL_CONFIG_DATA_ID, CHANNEL_CONFIG_GROUP);

        // 配置存在则加载到缓存
        if (StrUtil.isNotBlank(configStr)) {
            List<ChannelInfoNacosDO> channelInfoList = Optional.ofNullable(
                    JacksonUtil.fromJsonToList(configStr, ChannelInfoNacosDO.class)
            ).orElseGet(Collections::emptyList);
            refreshCache(channelInfoList);

            log.info("[ChannelConfig] 加载渠道配置成功 | count: {}", channelInfoList.size());
            return;
        }

        // 配置不存在则从数据库初始化
        List<GaChannelDO> channelDOList = channelMapper.selectList(Wrappers.emptyWrapper());
        List<ChannelInfoNacosDO> channelInfoList = BeanUtil.copyToList(channelDOList, ChannelInfoNacosDO.class);

        // 设置更新时间
        channelInfoList.forEach(info -> info.setUpdatedAt(LocalDateTime.now()));

        // 发布到 Nacos
        publishConfig(channelInfoList);

        log.info("[ChannelConfig] 初始化渠道配置成功 | count: {}", channelInfoList.size());
    }

    /**
     * 刷新缓存
     *
     * @param channelInfoList 渠道配置列表
     */
    private void refreshCache(List<ChannelInfoNacosDO> channelInfoList) {
        CACHE.clear();
        channelInfoList.forEach(info -> CACHE.put(info.getAppKey(), info));
    }

    /**
     * 获取所有渠道配置（从 Nacos 获取）
     *
     * @return 渠道配置列表
     */
    public List<ChannelInfoNacosDO> getConfigFromNacos() {
        try {
            String configStr = nacosConfigComponent.getConfig(CHANNEL_CONFIG_DATA_ID, CHANNEL_CONFIG_GROUP);

            if (StrUtil.isBlank(configStr)) {
                BlinkException.throwBusinessException(CHANNEL_CONFIG_GET_FAILED);
            }

            return Optional.ofNullable(JacksonUtil.fromJsonToList(configStr, ChannelInfoNacosDO.class))
                    .orElseGet(ArrayList::new);
        } catch (Exception e) {
            log.error("[ChannelConfig] 获取配置失败 | error: {}", e.getMessage(), e);
            BlinkException.throwBusinessException(CHANNEL_CONFIG_GET_FAILED);
        }
        return new ArrayList<>();
    }

    /**
     * 发布配置到 Nacos（不加密）
     *
     * @param channelInfoList 渠道配置列表
     */
    public void publishConfig(List<ChannelInfoNacosDO> channelInfoList) {
        try {
            String json = JacksonUtil.toJson(channelInfoList);
            nacosConfigComponent.configPublisher(CHANNEL_CONFIG_DATA_ID, CHANNEL_CONFIG_GROUP, json);

            // 刷新本地缓存
            refreshCache(channelInfoList);

            log.info("[ChannelConfig] 发布配置成功 | count: {}", channelInfoList.size());
        } catch (Exception e) {
            log.error("[ChannelConfig] 发布配置失败 | error: {}", e.getMessage(), e);
            BlinkException.throwBusinessException(CHANNEL_CONFIG_PUBLISH_FAILED);
        }
    }

    /**
     * 添加渠道配置
     *
     * @param channelInfo 渠道信息
     */
    public void addChannelConfig(GaChannelDO channelInfo) {
        List<ChannelInfoNacosDO> channelInfoList = getConfigFromNacos();

        ChannelInfoNacosDO nacosInfo = BeanUtil.copyProperties(channelInfo, ChannelInfoNacosDO.class);
        nacosInfo.setUpdatedAt(LocalDateTime.now());

        channelInfoList.add(nacosInfo);
        publishConfig(channelInfoList);

        log.info("[ChannelConfig] 添加渠道配置成功 | appKey: {}", channelInfo.getAppKey());
    }

    /**
     * 修改渠道配置
     *
     * @param channelInfo 渠道信息
     */
    public void modifyChannelConfig(GaChannelDO channelInfo) {
        List<ChannelInfoNacosDO> channelInfoList = getConfigFromNacos();

        boolean found = false;
        for (ChannelInfoNacosDO info : channelInfoList) {
            if (info.getAppKey().equals(channelInfo.getAppKey())) {
                BeanUtil.copyProperties(channelInfo, info);
                info.setUpdatedAt(LocalDateTime.now());
                found = true;
                break;
            }
        }

        if (!found) {
            BlinkException.throwBusinessException(CHANNEL_CONFIG_NOT_EXIST);
        }

        publishConfig(channelInfoList);

        log.info("[ChannelConfig] 修改渠道配置成功 | appKey: {}", channelInfo.getAppKey());
    }

    /**
     * 删除渠道配置
     *
     * @param appKey 渠道标识
     */
    public void deleteChannelConfig(String appKey) {
        List<ChannelInfoNacosDO> channelInfoList = getConfigFromNacos();

        boolean removed = channelInfoList.removeIf(info -> info.getAppKey().equals(appKey));

        if (!removed) {
            log.warn("[ChannelConfig] 渠道配置不存在，无需删除 | appKey: {}", appKey);
            return;
        }

        publishConfig(channelInfoList);

        log.info("[ChannelConfig] 删除渠道配置成功 | appKey: {}", appKey);
    }

    /**
     * 获取渠道配置（从本地缓存）
     *
     * @param appKey 渠道标识
     * @return 渠道配置信息
     */
    public ChannelInfoNacosDO getChannelConfig(String appKey) {
        return CACHE.get(appKey);
    }

    /**
     * 获取所有渠道配置（从本地缓存）
     *
     * @return 渠道配置列表
     */
    public List<ChannelInfoNacosDO> getAllChannelConfig() {
        return new ArrayList<>(CACHE.values());
    }
}
