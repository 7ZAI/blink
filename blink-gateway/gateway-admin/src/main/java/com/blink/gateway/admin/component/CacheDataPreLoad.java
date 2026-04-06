package com.blink.gateway.admin.component;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存数据预热组件
 * 应用启动时加载渠道数据到 Redis
 *
 * @author binblink
 */
@Component
@Slf4j
public class CacheDataPreLoad implements CommandLineRunner {

    @Resource
    private GaChannelMapper channelMapper;

    @Resource
    private RedisClient redisClient;

    @Override
    public void run(String... args) throws Exception {
        Map<String, Object> map = cacheChannels();
        log.debug("[CacheDataPreLoad] 加载渠道数据预热 数据量：{}", map.size());
        redisClient.batchSetWithExpire(map, 30, TimeUnit.MINUTES);
    }

    /**
     * 缓存渠道数据
     * 使用共享常量 RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX 作为 key 前缀
     *
     * @return 渠道缓存 Map
     */
    private Map<String, Object> cacheChannels() {
        List<GaChannelDO> channelDOList = channelMapper.findAllChannels();
        List<ChannelInfoRedisDO> channelInfos = BeanUtil.copyToList(channelDOList, ChannelInfoRedisDO.class);

        return channelInfos.stream()
                .collect(Collectors.toMap(
                        c -> RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX + c.getAppKey(),
                        obj -> obj
                ));
    }
}