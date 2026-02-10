package com.blink.base.component;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.base.entity.SysConfigDO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.entity.SysUserDO;
import com.blink.base.mapper.BlinkChannelMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.UserAuthService;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.core.annotation.PreHeatData;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.blink.base.constans.CommonConstans.GATEWAY_CONFIG_GROUP_ID;

/**
 * 缓存数据预热
 *
 * @author binblink
 */
@Component
@PreHeatData(method = "initCachingData")
@Slf4j
public class CachePreHeating {

    @Resource
    private BlinkChannelMapper channelMapper;

    @Resource
    private SysPermissionMapper permissionMapper;

    @Resource
    private SysConfigService configService;

    @Resource
    private RedisClient redisClient;

    @Resource
    private UserAuthService userAuthService;

    public void initCachingData() {

        log.info("-----------------------initCachingData---------------------------------");

        Map<String, Object> channelMap = cacheChannels();
        log.debug("PreHeating Channels  data to Redis:{}", channelMap);

        Map<String, Object> permissionMap = cachePermissions();
        log.debug("PreHeating Permissions  data to Redis:{}", permissionMap);

        Map<String, Object> gatewayConfigMap = cacheGatewayConfigs();
        log.debug("PreHeating GatewayConfigs  data to Redis:{}", gatewayConfigMap);




        Map<String, Object> totalMap = new HashMap<>(channelMap.size() + permissionMap.size() + gatewayConfigMap.size());

        totalMap.putAll(channelMap);
        totalMap.putAll(permissionMap);
        totalMap.putAll(gatewayConfigMap);

        autoLogInChannelUser(channelMap);

        redisClient.batchSetWithExpire(totalMap, 30, TimeUnit.MINUTES);
    }

    /**
     * 渠道绑定用户 自动登入 token永不过期
     * @param channelMap
     */
    private void autoLogInChannelUser(Map<String, Object> channelMap){
        channelMap.forEach((k, v) -> {
            ChannelInfoRedisDO channel = (ChannelInfoRedisDO) v;
            String userId = channel.getRelaUserId();
            cacheChannelUserInfo(userId, channel.getAccessToken());
        });
    }


    private void cacheChannelUserInfo(String userId,String token){

        var sysUserDO = new SysUserDO();
        sysUserDO.setUserId(Integer.parseInt(userId));
        SysLoginRspDTO loginUserInfo = userAuthService.getLoginUserInfo(sysUserDO, token);

        SysUserVO userInfo = loginUserInfo.getUserInfo();
        var userInfoRedis = new UserInfoRedisDO();
        BeanUtil.copyProperties(userInfo, userInfoRedis);

        userInfoRedis.setLoginDateTime(userInfo.getLastLoginTime());
        userInfoRedis.setPermissions(loginUserInfo.getPermissions());
        userInfoRedis.setToken(loginUserInfo.getToken());

        //存用户登入凭证和存用户信息 和其他普通用户不同 渠道绑定的用户永不过期
        redisClient.set(RedisKeyConstans.USER_TOKEN + userInfoRedis.getToken(), userInfoRedis);
        //存用户信息 和其他普通用户不同 渠道绑定的用户永不过期
        redisClient.set(RedisKeyConstans.USER_INFO + userInfo.getUserId(), userInfoRedis);
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

//    private

    private Map<String, Object> cacheGatewayConfigs() {

        List<SysConfigDO> gatewayConfigs = configService.getSysConfigsByGroupId(GATEWAY_CONFIG_GROUP_ID);

        Map<String, Object> map = new HashMap<>(gatewayConfigs.size());
        //参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
        for (SysConfigDO conf : gatewayConfigs) {
            SysConfigCacheDO obj = new SysConfigCacheDO();
            BeanUtil.copyProperties(conf, obj);
            map.put(RedisKeyConstans.GATEWAY_CONFIG_PREFIX + obj.getConfigKey(), obj);
        }
        return map;
    }


}
