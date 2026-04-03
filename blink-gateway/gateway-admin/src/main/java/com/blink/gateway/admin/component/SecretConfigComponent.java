package com.blink.gateway.admin.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.ChannelSecretKey;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.EnvReaderUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.common.utils.RSAUtils;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.NacosConfigConstant.*;

/**
 * 密钥配置管理组件
 * 负责管理渠道密钥配置，密钥存储在 Nacos 配置中心
 *
 * @author binblink
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = false)
public class SecretConfigComponent implements CommandLineRunner {

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private GaChannelMapper channelMapper;

    private static final String BLINK_SECRET_KEY = EnvReaderUtil.getEnv(SysConstant.BLINK_SECRET_KEY);

    /**
     * 密钥缓存
     */
    private final Map<String, ChannelSecretKey> CACHE = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {

        String configStr = nacosConfigComponent.getConfig(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP);

        // 存在则什么都不做
        if (StrUtil.isNotBlank(configStr)) {
            String json = AESUtils.decrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), configStr);
            List<ChannelSecretKey> channelSecretKeys = Optional.ofNullable(JacksonUtil.fromJsonToList(json, ChannelSecretKey.class)).orElseGet(Collections::emptyList);
            refreshCache(channelSecretKeys);

            log.info("[SecretConfig] 加载密钥配置成功 | count: {}", channelSecretKeys.size());

            return;
        }
        // 不存在则创建
        List<GaChannelDO> list = channelMapper.selectList(Wrappers.emptyWrapper());
        List<ChannelSecretKey> channelSecretKeys = BeanUtil.copyToList(list, ChannelSecretKey.class);

        channelSecretKeys.stream().parallel().forEach(this::refreshAllKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);

        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("[SecretConfig] 初始化密钥配置成功 | count: {}", channelSecretKeys.size());
    }

    /**
     * 加载进缓存
     *
     * @param channelSecretKeys 渠道密钥列表
     */
    private void refreshCache(List<ChannelSecretKey> channelSecretKeys) {
        channelSecretKeys.forEach(cs -> CACHE.put(cs.getAppKey(), cs));
    }

    /**
     * 获取渠道密钥
     *
     * @param appKey 渠道标识
     * @return 渠道密钥信息
     * @throws Exception 异常
     */
    public ChannelSecretKey getChannelSecretKey(String appKey) throws Exception {

        ChannelSecretKey channelSecretKey = CACHE.get(appKey);
        if (ObjectUtil.isNull(channelSecretKey)) {

            List<ChannelSecretKey> channelSecretKeyList = Optional.ofNullable(getConfigFromRedis()).orElseGet(ArrayList::new);
            Optional<ChannelSecretKey> optional = channelSecretKeyList.stream().filter(sk -> appKey.equals(sk.getAppKey()))
                    .findFirst();
            return optional.orElse(null);
        }
        return channelSecretKey;
    }


    /**
     * 获取配置
     *
     * @return 渠道密钥列表
     * @throws Exception 异常
     */
    public List<ChannelSecretKey> getConfigFromRedis() throws Exception {

        String configStr = nacosConfigComponent.getConfig(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP);
        // 为空抛异常
        if (StrUtil.isBlank(configStr)) {
            BlinkException.throwBusinessException(SECRET_CONFIG_GET_FAILED);
        }
        String json = AESUtils.decrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), configStr);
        return JacksonUtil.fromJsonToList(json, ChannelSecretKey.class);
    }

    /**
     * 删除单个渠道密钥配置
     *
     * @param appKey 渠道appKey
     * @throws Exception 异常
     */
    public void deleteChannelSecretConfig(String appKey) throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();
        boolean removed = channelSecretKeys.removeIf(sk -> sk.getAppKey().equals(appKey));

        if (!removed) {
            BlinkException.throwBusinessException(CHANNEL_SECRET_NOT_EXIST);
        }

        String json = JacksonUtil.toJson(channelSecretKeys);
        // 再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("[SecretConfig] 删除渠道密钥配置成功 | appKey: {}", appKey);

        refreshCache(channelSecretKeys);
    }

    /**
     * 添加新的渠道密钥配置
     *
     * @param channelInfo 渠道信息
     * @throws Exception 异常
     */
    public void addChannelSecretConfig(GaChannelDO channelInfo) throws Exception {
        ChannelSecretKey channelSecretKey = new ChannelSecretKey();
        BeanUtil.copyProperties(channelInfo, channelSecretKey);
        refreshAllKey(channelSecretKey);

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();

        channelSecretKeys.add(channelSecretKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        // 再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("[SecretConfig] 添加渠道密钥配置成功 | appKey: {}", channelSecretKey.getAppKey());

        refreshCache(channelSecretKeys);
    }

    /**
     * 刷新重新生成指定渠道的所有配置
     *
     * @param appKey 渠道标识
     * @throws Exception 异常
     */
    public void refreshChannelConfig(String appKey) throws Exception {

        refreshChannelSecretKeyConfig(appKey, this::refreshAllKey);

        log.info("[SecretConfig] 刷新渠道所有密钥成功 | appKey: {}", appKey);
    }

    /**
     * 刷新全部渠道的所有密钥
     *
     * @throws Exception 异常
     */
    public void refreshAllChannelConfigs() throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();

        if (CollUtil.isEmpty(channelSecretKeys)) {
            BlinkException.throwBusinessException(SECRET_CONFIG_EMPTY);
        }

        channelSecretKeys.stream().parallel().forEach(this::refreshAllKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        // 再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("[SecretConfig] 刷新所有渠道密钥配置成功 | count: {}", channelSecretKeys.size());
    }

    /**
     * 单项密钥刷新统一方法
     *
     * @param appKey           appkey
     * @param channelProcessor 传递刷新单项行为
     * @throws Exception 异常
     */
    public void refreshChannelSecretKeyConfig(String appKey, Consumer<ChannelSecretKey> channelProcessor) throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();
        ChannelSecretKey secretKey = channelSecretKeys.stream()
                .filter(sk -> appKey.equals(sk.getAppKey()))
                .findFirst()
                .orElse(null);

        if (ObjectUtil.isNull(secretKey)) {
            BlinkException.throwBusinessException(CHANNEL_SECRET_NOT_EXIST);
        }

        channelProcessor.accept(secretKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        // 再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        refreshCache(channelSecretKeys);
    }

    /**
     * 刷新渠道密钥对
     *
     * @param appKey 渠道标识
     * @throws Exception 异常
     */
    public void refreshChannelKeyConfig(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshChannelRSAKey);
    }

    /**
     * 刷新系统密钥对
     *
     * @param appKey 渠道标识
     * @throws Exception 异常
     */
    public void refreshSystemKeyConfig(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshSystemRSAKey);
    }

    /**
     * 刷新AppSecret
     *
     * @param appKey 渠道标识
     * @throws Exception 异常
     */
    public void refreshAppSecretKey(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshAppSecretKey);
    }

    /**
     * 刷新TokenSecret
     *
     * @param appKey 渠道标识
     * @throws Exception 异常
     */
    public void refreshTokenSecret(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshTokenSecret);
    }


    /**
     * 刷新渠道所有密钥
     *
     * @param secretKey 密钥信息
     */
    private void refreshAllKey(ChannelSecretKey secretKey) {

        refreshChannelRSAKey(secretKey);
        refreshSystemRSAKey(secretKey);
        refreshAppSecretKey(secretKey);
        refreshTokenSecret(secretKey);
    }

    /**
     * 刷新渠道RSA密钥对
     *
     * @param secretKey 密钥信息
     */
    private void refreshChannelRSAKey(ChannelSecretKey secretKey) {

        KeyPair keyPair = RSAUtils.generateKeyPair();
        String channelPrivateKey = RSAUtils.generatePrivateKeyToBase64(keyPair);
        String channelPublicKey = RSAUtils.generatePublicKeyToBase64(keyPair);

        secretKey.setChannelPrivateKey(channelPrivateKey);
        secretKey.setChannelPublicKey(channelPublicKey);
    }

    /**
     * 刷新系统RSA密钥对
     *
     * @param secretKey 密钥信息
     */
    private void refreshSystemRSAKey(ChannelSecretKey secretKey) {

        KeyPair sysKeyPair = RSAUtils.generateKeyPair();

        String sysPrivateKey = RSAUtils.generatePrivateKeyToBase64(sysKeyPair);
        String sysPublicKey = RSAUtils.generatePublicKeyToBase64(sysKeyPair);
        secretKey.setSystemPublickey(sysPublicKey);
        secretKey.setSystemPrivatekey(sysPrivateKey);
    }

    /**
     * 刷新appSecret密钥
     *
     * @param secretKey 密钥信息
     */
    private void refreshAppSecretKey(ChannelSecretKey secretKey) {
        secretKey.setAppSecret(SecureUtil.hmacSha256().digestBase64(RandomUtil.randomString(32), true));
    }

    /**
     * 刷新TokenSecret密钥
     *
     * @param secretKey 密钥信息
     */
    private void refreshTokenSecret(ChannelSecretKey secretKey) {
        try {
            secretKey.setTokenSecret(AESUtils.encodeToBase64(AESUtils.generateRandomKey(256).getEncoded()));
        } catch (Exception e) {
            log.error("[SecretConfig] 刷新TokenSecret失败 | error: {}", e.getMessage(), e);
            throw new BlinkException("刷新TokenSecret失败: " + e.getMessage(), e, REFRESH_SECRET_FAILED);
        }
    }
}