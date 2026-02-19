package com.blink.base.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.base.mapper.BlinkChannelMapper;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.ChannelSecretKey;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.EnvReaderUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.common.utils.RSAUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.blink.base.constans.CommonConstans.SECRET_CONFIG_DATA_ID;
import static com.blink.base.constans.CommonConstans.SECRET_CONFIG_GROUP;

/**
 * 往nacos添加密钥配置文件
 * 密钥不再入库
 *
 * @Author binblink
 */
@Component
@Slf4j
public class SecretConfigComponent implements CommandLineRunner {

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private BlinkChannelMapper channelMapper;

    private static final String BLINK_SECRET_KEY = EnvReaderUtil.getEnv(SysConstant.BLINK_SECRET_KEY);

    //缓存
    private final Map<String, ChannelSecretKey> CACHE = new ConcurrentHashMap<String, ChannelSecretKey>();

    @Override
    public void run(String... args) throws Exception {

        String configStr = nacosConfigComponent.getConfig(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP);

        //测试
//        refreshAllChannelConfigs();
        //存在则什么都不做
        if (StrUtil.isNotBlank(configStr)) {
            String json = AESUtils.decrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), configStr);
            List<ChannelSecretKey> channelSecretKeys = Optional.ofNullable(JacksonUtil.fromJsonToList(json, ChannelSecretKey.class)).orElseGet(Collections::emptyList);
            refreshCache(channelSecretKeys);
            return;
        }
        //不存在 则创建
        List<BlinkChannelDO> list = channelMapper.selectList(Wrappers.emptyWrapper());
        List<ChannelSecretKey> channelSecretKeys = BeanUtil.copyToList(list, ChannelSecretKey.class);

        channelSecretKeys.stream().parallel().forEach(this::refreshAllKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);

//        log.debug("channelSecretConfig配置文件加密后：" + configStr);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);
    }

    //加载进缓存
    private void refreshCache(List<ChannelSecretKey> channelSecretKeys) {
        channelSecretKeys.forEach(cs -> CACHE.put(cs.getAppKey(), cs));
    }

    public ChannelSecretKey getChannelSecretKey(String appKey) throws Exception {

        ChannelSecretKey channelSecretKey = CACHE.get(appKey);
        if (Objects.isNull(channelSecretKey)) {

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
     * @throws Exception
     */
    public List<ChannelSecretKey> getConfigFromRedis() throws Exception {

        String configStr = nacosConfigComponent.getConfig(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP);
        //为空抛异常
        if (StrUtil.isBlank(configStr)) {
            BlinkException.throwException("获取配置文件失败!");
        }
        String json = AESUtils.decrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), configStr);
        return JacksonUtil.fromJsonToList(json, ChannelSecretKey.class);
    }

    /**
     * 删除单个渠道密钥配置
     *
     * @param appKey 渠道appKey
     * @throws Exception
     */
    public void deleteChannelSecretConfig(String appKey) throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();
        boolean removed = channelSecretKeys.removeIf(sk -> sk.getAppKey().equals(appKey));

        if (!removed) {
            BlinkException.throwException("该渠道不存在：appkey" + appKey);
        }

        String json = JacksonUtil.toJson(channelSecretKeys);
        //再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("从配置文件中删除appkey为{}的渠道配置 成功！", appKey);

        refreshCache(channelSecretKeys);
    }

    /**
     * 添加新的渠道密钥配置
     *
     * @param channelInfo
     */
    public void addChannelSecretConfig(BlinkChannelDO channelInfo) throws Exception {
        ChannelSecretKey channelSecretKey = new ChannelSecretKey();
        BeanUtil.copyProperties(channelInfo, channelSecretKey);
        refreshAllKey(channelSecretKey);

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();

        channelSecretKeys.add(channelSecretKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        //再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        log.info("添加新的渠道密钥配置成功！appkey:{}", channelSecretKey.getAppKey());

        refreshCache(channelSecretKeys);

    }

    /**
     * 刷新 重新生成所有配置
     *
     * @param appKey
     * @throws Exception
     */
    public void refreshChannelConfig(String appKey) throws Exception {

        refreshChannelSecretKeyConfig(appKey, this::refreshAllKey);
        log.info("刷新单个渠道所有密钥成功！appkey为{}", appKey);

    }

    /**
     * 刷新全部渠道的所有密钥
     *
     * @throws Exception
     */
    public void refreshAllChannelConfigs() throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();

        if (channelSecretKeys.isEmpty()) {
            BlinkException.throwException("该配置文件为空不存在");
        }

        channelSecretKeys.stream().parallel().forEach(this::refreshAllKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        //再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);
        log.info("刷新渠道密钥配置文件中成功！");

    }

    /**
     * 单项密钥刷新统一方法
     *
     * @param appKey           appkey
     * @param channelProcessor 传递刷新单项行为
     * @throws Exception
     */
    public void refreshChannelSecretKeyConfig(String appKey, Consumer<ChannelSecretKey> channelProcessor) throws Exception {

        List<ChannelSecretKey> channelSecretKeys = getConfigFromRedis();
        ChannelSecretKey secretKey = channelSecretKeys.stream()
                .filter(sk -> appKey.equals(sk.getAppKey()))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(secretKey)) {
            BlinkException.throwException("该渠道不存在：appkey" + appKey);
        }

        channelProcessor.accept(secretKey);

        String json = JacksonUtil.toJson(channelSecretKeys);
        //再次加密写回配置中心
        String configStr = AESUtils.encrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), json);
        nacosConfigComponent.configPublisher(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP, configStr);

        refreshCache(channelSecretKeys);
    }

    /**
     * 刷新渠道密钥对
     *
     * @param appKey
     */
    public void refreshChannelKeyConfig(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshChannelRSAKey);
    }

    /**
     * 刷新系统密钥对
     *
     * @param appKey
     */
    public void refreshSystemKeyConfig(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshSystemRSAKey);
    }

    /**
     * 刷新AppSecret
     *
     * @param appKey
     */
    public void refreshAppSecretKey(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshAppSecretKey);
    }

    /**
     * 刷新TokenSecret
     *
     * @param appKey
     */
    public void refreshTokenSecret(String appKey) throws Exception {
        refreshChannelSecretKeyConfig(appKey, this::refreshTokenSecret);
    }


    /**
     * 刷新渠道所有密钥
     *
     * @param secretKey
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
     * @param secretKey
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
     * @param secretKey
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
     * @param secretKey
     */
    private void refreshAppSecretKey(ChannelSecretKey secretKey) {
        secretKey.setAppSecret(SecureUtil.hmacSha256().digestBase64(RandomUtil.randomString(32), true));
    }

    /**
     * 刷新TokenSecret密钥
     *
     * @param secretKey
     */
    private void refreshTokenSecret(ChannelSecretKey secretKey) {
        try {
            secretKey.setTokenSecret(AESUtils.encodeToBase64(AESUtils.generateRandomKey(256).getEncoded()));
        } catch (Exception e) {
            throw new BlinkException(e, e.getMessage());
        }
    }

}
