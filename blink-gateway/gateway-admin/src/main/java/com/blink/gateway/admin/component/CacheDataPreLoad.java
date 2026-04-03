package com.blink.gateway.admin.component;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.core.annotation.PreHeatData;
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
 * @Author binblink
 * @Date 2026/3/10
 */
@Component
@Slf4j
public class CacheDataPreLoad implements CommandLineRunner {

    @Resource
    private GaChannelMapper channelMapper;

    @Resource
    private RedisClient redisClient;

    private  final  String CHANNEL_INFO = "blink:channel:";


    /**
     * Callback used to run the bean.
     *
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    @Override
    public void run(String... args) throws Exception {
        Map<String, Object> map =  cacheChannels();
        log.debug("加载渠道数据预热 数据量：{}",map.size());
        redisClient.batchSetWithExpire(map, 30, TimeUnit.MINUTES);
    }

    private Map<String, Object> cacheChannels() {

        List<GaChannelDO> channelDOList = channelMapper.findAllChannels();
        List<ChannelInfoRedisDO> channelInfos = BeanUtil.copyToList(channelDOList, ChannelInfoRedisDO.class);

        return channelInfos.stream()
                .collect(Collectors.toMap(c -> CHANNEL_INFO + c.getAppKey(), obj -> obj));
    }



}
