package com.blink.base.component;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.base.entity.SysConfigDO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.mapper.BlinkChannelMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.service.SysConfigService;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.core.annotation.PreHeatData;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.blink.base.constans.CommonConstans.GATEWAY_CONFIG_GROUP_ID;

@Component
@PreHeatData(method = "initCachingData")
public class CachePreHeating {

    private final Logger logger = LoggerFactory.getLogger(CachePreHeating.class);
    @Resource
    private BlinkChannelMapper channelMapper;

    @Resource
    private SysPermissionMapper permissionMapper;

    @Resource
    private SysConfigService configService;

    @Resource
    private RedisClient redisClient;

    public void initCachingData() {

        logger.info("-----------------------initCachingData---------------------------------");

        Map<String, Object> channelMap = cacheChannels();
        logger.info("PreHeating Channels  data to Redis:{}", channelMap);

        Map<String, Object> permissionMap = cachePermissions();
        logger.info("PreHeating Permissions  data to Redis:{}", permissionMap);

        Map<String, Object> gatewayConfigMap = cacheGatewayConfigs();
        logger.info("PreHeating GatewayConfigs  data to Redis:{}", gatewayConfigMap);


        Map<String, Object> totalMap = new HashMap<>(channelMap.size() + permissionMap.size() + gatewayConfigMap.size());

        totalMap.putAll(channelMap);
        totalMap.putAll(permissionMap);
        totalMap.putAll(gatewayConfigMap);

        redisClient.batchSetWithExpire(totalMap,30, TimeUnit.MINUTES);
    }


    private Map<String, Object> cacheChannels() {

        List<BlinkChannelDO> channelDOList = channelMapper.findAllChannels();
        List<ChannelInfoRedisDO> channelInfos = BeanUtil.copyToList(channelDOList, ChannelInfoRedisDO.class);

        return channelInfos.stream()
                .collect(Collectors.toMap(c -> RedisKeyConstans.CHANNEL_INFO + c.getAppKey(), obj -> obj));
    }

    private Map<String, Object> cachePermissions() {

        List<SysPermissionDO> permissionList = permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                .eq(SysPermissionDO::getStatus, 0));
        return permissionList.stream()
                .collect(Collectors.toMap(p -> RedisKeyConstans.URL_PERMISSION + p.getUrl(), SysPermissionDO::getAcIdentity));
    }

    private Map<String, Object> cacheGatewayConfigs() {

        List<SysConfigDO> gatewayConfigs = configService.getSysConfigsByGroupId(GATEWAY_CONFIG_GROUP_ID);

        Map<String, Object> map = new HashMap<>(gatewayConfigs.size());
        //参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
        for(SysConfigDO conf:gatewayConfigs){
            SysConfigCacheDO obj = new  SysConfigCacheDO();
            BeanUtil.copyProperties(conf,obj);
            map.put(RedisKeyConstans.GATEWAY_CONFIG_PREFIX + obj.getConfigKey(),obj);
        }
        return map;
    }


}
