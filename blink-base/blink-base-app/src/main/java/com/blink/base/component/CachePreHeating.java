package com.blink.base.component;

import cn.hutool.core.bean.BeanUtil;
import com.blink.base.constants.RedisKeyConstans;
import com.blink.base.dto.req.GetAllApiPermissionsReq;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysConfigDO;
import com.blink.base.entity.SysFieldConstraintDO;
import com.blink.base.entity.SysMsgInfoDO;
import com.blink.base.mapper.SysFieldConstraintMapper;
import com.blink.base.mapper.SysMsgInfoMapper;
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.common.data.FieldConstraintCacheDO;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.SysPermissionService;
import com.blink.framework.common.data.DictCacheDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.blink.base.constants.CommonConstans.GATEWAY_CONFIG_GROUP_ID;

/**
 * 缓存数据预热组件
 * 应用启动时预加载字典、消息、权限等数据到Redis缓存
 *
 * @author binblink
 */
@Component
@Slf4j
public class CachePreHeating  {

    @Resource
    private SysPermissionService permissionService;

    @Resource
    private SysConfigService configService;

    @Resource
    private RedisClient redisClient;

//    @Resource
//    private UserAuthService userAuthService;

    @Resource
    private SysFieldConstraintMapper sysFieldConstraintMapper;

    @Resource
    private SysMsgInfoMapper sysMsgInfoMapper;

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private BlinkWebAppConfigProperties configProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        BlinkWebAppConfigProperties.PreCache cacheConfig = configProperties.getPreCache();

        if (!cacheConfig.getEnable()) {
            return;
        }
        // 预热权限数据
        preHeatingPermissions();

        // 预热字段约束数据
        if (cacheConfig.getDictionary()) {
            preHeatingFieldConstraintCache();
        }

        // 预热消息数据
        if (cacheConfig.getErrMsgInfo()) {
            preHeatingMsgInfoCache();
        }
    }

    /**
     * 预热权限数据到Redis
     */
    private void preHeatingPermissions() {
        log.info("-----------------------preHeatingPermissions---------------------------------");

        Map<String, Object> permissionMap = cachePermissions();
        log.debug("PreHeating Permissions data to Redis: {}", permissionMap);

        Map<String, Object> totalMap = new HashMap<>(permissionMap.size());
        totalMap.putAll(permissionMap);

        redisClient.batchSetWithExpire(totalMap, 30, TimeUnit.MINUTES);
    }

    /**
     * 缓存url 对应的权限标识
     *
     * @return 权限映射Map
     */
    private Map<String, Object> cachePermissions() {
        GetAllApiPermissionsRsp rsp = permissionService.getAllApiPermission(new GetAllApiPermissionsReq());
        List<SysPermissionVO> permissionList = rsp.getPermissionList();

        if(permissionList == null || permissionList.isEmpty()) {
            return new HashMap<>();
        }
        return permissionList.stream()
                .collect(Collectors.toMap(p -> RedisKeyConstans.URL_PERMISSION + p.getUrl(), SysPermissionVO::getAcIdentity));
    }

    /**
     * 缓存网关配置数据
     *
     * @return 网关配置映射Map
     */
    private Map<String, Object> cacheGatewayConfigs() {
        List<SysConfigDO> gatewayConfigs = configService.getSysConfigsByGroupId(GATEWAY_CONFIG_GROUP_ID);

        Map<String, Object> map = new HashMap<>(gatewayConfigs.size());
        // 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
        for (SysConfigDO conf : gatewayConfigs) {
            SysConfigCacheDO obj = new SysConfigCacheDO();
            BeanUtil.copyProperties(conf, obj);
            map.put(RedisKeyConstans.GATEWAY_CONFIG_PREFIX + obj.getConfigKey(), obj);
        }
        return map;
    }

    /**
     * 初始化字段约束缓存
     */
    private void preHeatingFieldConstraintCache() {
        log.info("-----------------------preHeatingFieldConstraintCache---------------------------------");

        cacheComponent.loadCacheFromDB(RedisCacheKeyConstant.FIELD_CONSTRAINT_KEY_PREFIX, () -> {
            List<SysFieldConstraintDO> constraintList = sysFieldConstraintMapper.findAllFieldConstraints();
            List<FieldConstraintCacheDO> cacheList = BeanUtil.copyToList(constraintList, FieldConstraintCacheDO.class);

            return cacheList.stream().collect(Collectors.toMap(
                    constraint -> RedisCacheKeyConstant.FIELD_CONSTRAINT_KEY_PREFIX + constraint.getConstraintName(),
                    constraint -> constraint
            ));
        });
    }

    /**
     * 初始化消息缓存
     */
    private void preHeatingMsgInfoCache() {
        log.info("-----------------------preHeatingMsgInfoCache---------------------------------");

        cacheComponent.loadCacheFromDB(CoreConstant.MSG_INFO_KEY_PREFIX, () -> {
            List<SysMsgInfoDO> msgInfoList = sysMsgInfoMapper.findAllMsgInfo();

            return msgInfoList.stream().collect(Collectors.toMap(
                    sysMsgInfoDO -> CoreConstant.MSG_INFO_KEY_PREFIX + sysMsgInfoDO.getMsgLang() + ":" + sysMsgInfoDO.getMsgCode(),
                    SysMsgInfoDO::getMsgInfo
            ));
        });
    }

    /**
     * 刷新字段约束缓存
     */
    public void refreshFieldConstraintCache() {
        preHeatingFieldConstraintCache();
    }

    /**
     * 刷新消息缓存
     */
    public void refreshMsgInfoCache() {
        preHeatingMsgInfoCache();
    }

    /**
     * 刷新权限缓存
     */
    public void refreshPermissionsCache() {
        preHeatingPermissions();
    }
}