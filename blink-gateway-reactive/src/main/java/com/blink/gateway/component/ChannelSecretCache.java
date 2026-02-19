package com.blink.gateway.component;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.ChannelSecretKey;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.jwt.JwtConfig;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.framework.common.utils.AESUtils;
import com.blink.framework.common.utils.EnvReaderUtil;
import com.blink.framework.common.utils.JacksonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.constant.GatewayConstant.SECRET_CONFIG_DATA_ID;
import static com.blink.gateway.constant.GatewayConstant.SECRET_CONFIG_GROUP;

/**
 * 获取各个渠道的密钥 并缓存
 *
 * @Author binblink
 * @Date 2026/2/5
 */
@Component
@Slf4j
public class ChannelSecretCache  {

    @Resource
    private NacosConfigManager nacosConfigManager;

    private static final String BLINK_SECRET_KEY = EnvReaderUtil.getEnv(SysConstant.BLINK_SECRET_KEY);

    private  Map<String,ChannelSecretKey> channelSecretConfigs;

    private Map<String,JwtProvider> jwtProviders;

    @PostConstruct
    public void init() throws BlinkException {
        String configStr = "";
        String json = "";
        try{
            configStr = nacosConfigManager.getConfigService().getConfig(SECRET_CONFIG_DATA_ID, SECRET_CONFIG_GROUP,50000);
        } catch (Exception e) {
            log.error("获取渠道密钥配置文件失败{}",e.getMessage(),e);
            throw new BlinkException(e,e.getMessage());
        }

        //为空抛异常
        if (StrUtil.isBlank(configStr)) {
            BlinkException.throwException("获取配置文件失败!");
        }

        try{
            json = AESUtils.decrypt(AESUtils.keyFromBase64(BLINK_SECRET_KEY), configStr);
        } catch (Exception e) {
            log.error("解密渠道密钥配置文件失败{}",e.getMessage(),e);
            throw new BlinkException(e,e.getMessage());
        }

        List<ChannelSecretKey> channelSecretConfigs = JacksonUtil.fromJsonToList(json, ChannelSecretKey.class);

        Map<String,ChannelSecretKey> secretKeyMap = new HashMap<>(channelSecretConfigs.size());
        Map<String,JwtProvider> jwtProviders = new HashMap<>(channelSecretConfigs.size());

        channelSecretConfigs.forEach(channelSecretKey -> {
            var jwtConfig = new JwtConfig();
            jwtConfig.setJwtSecret(channelSecretKey.getTokenSecret());
            var jwtProvider = new JwtProvider(jwtConfig);
            jwtProviders.put(channelSecretKey.getAppKey(),jwtProvider);
            secretKeyMap.put(channelSecretKey.getAppKey(),channelSecretKey);
        });

        this.jwtProviders = jwtProviders;
        this.channelSecretConfigs = secretKeyMap;
    }

    public Map<String, JwtProvider> getJwtProviders() {
        return jwtProviders;
    }

    public Map<String, ChannelSecretKey> getChannelSecretConfigs() {
        return channelSecretConfigs;
    }

}
