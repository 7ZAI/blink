package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.CommonConstants;
import com.blink.base.constants.RedisKeyConstants;
import com.blink.base.dto.rsp.ConfigGroupRsp;
import com.blink.base.dto.req.AddSysConfigReq;
import com.blink.base.dto.req.DeleteSysConfigReq;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.base.dto.req.QuerySysConfigReq;
import com.blink.base.dto.req.UpdateSysConfigReq;
import com.blink.base.dto.rsp.QuerySysConfigRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.entity.SysConfigDO;
import com.blink.base.entity.SysConfigGroupDO;
import com.blink.base.mapper.SysConfigGroupMapper;
import com.blink.base.mapper.SysConfigMapper;
import com.blink.base.service.SysConfigService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.blink.base.constants.BaseErrCodeConstant.CONFIG_NAME_REPEAT;
import static com.blink.base.constants.BaseErrCodeConstant.CONFIG_NOT_EXIST;
import static com.blink.base.constants.RedisKeyConstants.BLINK_PREFIX;

/**
 * 参数配置表 服务实现类
 *
 * @author blink
 * @since 2025-09-05
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class SysConfigServiceImpl implements SysConfigService {


    @Resource
    private SysConfigMapper sysConfigMapper;

    @Resource
    private SysConfigGroupMapper sysConfigGroupMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private ConfigCacheAsyncService configCacheAsyncService;

    /**
     * 保存 参数配置表
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void saveSysConfig(AddSysConfigReq saveParam) throws BlinkException {

        var sysConfigDO = new SysConfigDO();
        BeanUtil.copyProperties(saveParam, sysConfigDO);

        SysConfigDO keyName = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigDO>().eq(SysConfigDO::getConfigKey, sysConfigDO.getConfigKey()));

        if (Objects.nonNull(keyName)) {
            BlinkException.throwBusinessException(CONFIG_NAME_REPEAT);
        }

        sysConfigMapper.insert(sysConfigDO);
        log.info("[SysConfig] 新增参数配置成功 | id: {}, configKey: {}, configName: {}",
                sysConfigDO.getId(), sysConfigDO.getConfigKey(), sysConfigDO.getConfigName());
    }


    /**
     * 删除 参数配置表
     *
     * @param deleteParam
     * @throws BlinkException
     */
    @Override
    public void deleteSysConfig(DeleteSysConfigReq deleteParam) throws BlinkException {

        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            sysConfigMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysConfig] 批量删除参数配置成功 | ids: {}", deleteParam.getIdList());
        } else {
            sysConfigMapper.deleteById(deleteParam.getDeleteId());
            log.info("[SysConfig] 删除参数配置成功 | id: {}", deleteParam.getDeleteId());
        }
    }

    /**
     * 更新 参数配置表
     *
     * @param updateParam
     * @throws BlinkException
     */
    @Override
    public void modifySysConfig(UpdateSysConfigReq updateParam) throws BlinkException {

        SysConfigDO oldOne = sysConfigMapper.selectById(updateParam.getId());
        var sysConfigDO = new SysConfigDO();

        //参数不存在
        if (Objects.isNull(oldOne)) {
            BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
        }
        String rawConfigKey = updateParam.getConfigKey();

        //传递的key与数据库中的不符合
        if (!oldOne.getConfigKey().equals(rawConfigKey)) {
            BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
        }

        // 统一构建缓存 Key: 去掉可能的前缀，再统一添加
        String cacheKey = buildCacheKey(rawConfigKey);

        // 立即删除缓存（Redis + 本地）
        configCacheAsyncService.deleteConfigCache(cacheKey);

        BeanUtil.copyProperties(updateParam, sysConfigDO);
        sysConfigMapper.updateById(sysConfigDO);
        log.info("[SysConfig] 更新参数配置成功 | id: {}, configKey: {}, configValue: {}",
                sysConfigDO.getId(), sysConfigDO.getConfigKey(), sysConfigDO.getConfigValue());

        //如果前端系统需要的配置项
        if (isSystemConfigChange(cacheKey)) {
            //删除redis中保存的系统配置项
            redisClient.delete(RedisKeyConstants.SYSTEM_CONFIG);
            //清除本地缓存
            cacheComponent.clearLocalCache(RedisKeyConstants.SYSTEM_CONFIG);
        }

        // 异步延迟双删，避免阻塞主线程（通过独立 Service 调用，确保 @Async 生效）
        configCacheAsyncService.asyncDelayedDelete(cacheKey);
    }

    /**
     * 统一构建缓存 Key
     * <p>
     * 确保 Key 格式统一: BLINK_PREFIX + rawKey
     * 先去掉可能存在的前缀，再统一添加
     * </p>
     *
     * @param configKey 配置 Key（可能带前缀或不带前缀）
     * @return 统一格式的缓存 Key
     */
    private String buildCacheKey(String configKey) {
        String rawKey = configKey.replaceAll(BLINK_PREFIX, "");
        return BLINK_PREFIX + rawKey;
    }

    /**
     * 是否为前端需要的系统配置项进行了修改
     *
     * @param cacheKey 修改的参数（已带 BLINK_PREFIX）
     * @return boolean
     */
    private boolean isSystemConfigChange(String cacheKey) {
        // 使用 switch 模式简化判断，包含所有前端配置项
        return CommonConstants.SysConfigKeys.LOGIN_CAPTCHA_ENABLED.equals(cacheKey)
            || CommonConstants.SysConfigKeys.SYSTEM_TITLE.equals(cacheKey)
            || CommonConstants.SysConfigKeys.SYSTEM_LOGO.equals(cacheKey)
            || CommonConstants.SysConfigKeys.SYSTEM_FOOTER.equals(cacheKey)
            || CommonConstants.SysConfigKeys.USER_DEFAULT_AVATAR.equals(cacheKey);
    }


    /**
     * 查询 参数配置表 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysConfigRsp getSysConfigList(QuerySysConfigReq queryParam) throws BlinkException {

        List<Integer> groupIds = queryParam.getGroupIds();
        //查询所有 子组id
        if (queryParam.getFindAllChild()) {
            groupIds = sysConfigGroupMapper.findAllSonIdByParentId(groupIds.get(0));
            queryParam.setGroupIds(groupIds);
        }

        var pageRsp = new QuerySysConfigRsp();
        QuerySysConfigRsp result = PageUtils.queryPage(queryParam, () -> sysConfigMapper.findSysConfigList(queryParam), pageRsp);
        return result;
    }

    /**
     * 根据分组Id 查询该分组下所有子配置
     *
     * @param gid
     * @return
     * @throws BlinkException
     */
    @Override
    public List<SysConfigDO> getSysConfigsByGroupId(Integer gid) throws BlinkException {

        List<Integer> groupIds = sysConfigGroupMapper.findAllSonIdByParentId(gid);
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfigDO>().in(SysConfigDO::getGroupId, groupIds).eq(SysConfigDO::getStatus, true));
    }

    /**
     * 根据key或者id查询单个配置
     *
     * @param param
     * @return
     * @throws BlinkException
     */
    @Override
    public SysConfigVO getOneConfigFromDataBase(QueryOneSysConfigReq param) throws BlinkException {


        SysConfigDO result = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigDO>()
                .eq(StrUtil.isNotBlank(param.getConfigKey()), SysConfigDO::getConfigKey, param.getConfigKey())
                .eq(Objects.nonNull(param.getId()), SysConfigDO::getId, param.getId()));
        SysConfigVO vo = new SysConfigVO();
        BeanUtils.copyProperties(result, vo);
        return vo;
    }

    /**
     * 根据查询条件查询
     * 缓存或者数据库获取单个参数配置
     *
     * @param queryParam
     * @return SysConfigVO
     * @throws BlinkException
     */
    @Override
    public SysConfigVO getOneConfigFromCacheOrDataBase(QueryOneSysConfigReq queryParam) throws BlinkException {

        // 统一构建缓存 Key: 去掉可能的前缀，再统一添加
        String cacheKey = buildCacheKey(queryParam.getConfigKey());
        // 数据库查询使用的原始 Key（不带前缀）
        String dataKey = cacheKey.replaceAll(BLINK_PREFIX, "");

        Object cachedObject = cacheComponent.getFromCacheOrDB(cacheKey, () -> sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigDO>()
                .eq(StrUtil.isNotBlank(dataKey), SysConfigDO::getConfigKey, dataKey)
                .eq(Objects.nonNull(queryParam.getId()), SysConfigDO::getId, queryParam.getId())));

        var vo = new SysConfigVO();
        if (cachedObject != null) {
            if (cachedObject instanceof SysConfigDO configDO) {
                BeanUtils.copyProperties(configDO, vo);
            } else {
                vo = JacksonUtil.convert(cachedObject, SysConfigVO.class);
            }
        }
        return vo;
    }


    /**
     * 根据配置key获取布尔类型的配置值
     *
     * @param configKey    配置key
     * @param defaultValue 默认值
     * @return 配置的布尔值
     * @throws BlinkException
     */
    @Override
    public Boolean getBooleanConfig(String configKey, Boolean defaultValue) throws BlinkException {
        try {
            QueryOneSysConfigReq param = new QueryOneSysConfigReq();
            param.setConfigKey(configKey);
            SysConfigVO config = getOneConfigFromCacheOrDataBase(param);
            if (config != null && config.getConfigValue() != null) {
                return Boolean.parseBoolean(config.getConfigValue());
            }
        } catch (Exception e) {
            log.error("获取布尔配置失败, configKey: {}", configKey, e);
        }
        return defaultValue;
    }

    /**
     * 根据配置key获取整数类型的配置值
     *
     * @param configKey    配置key
     * @param defaultValue 默认值
     * @return 配置的整数值
     * @throws BlinkException
     */
    @Override
    public Integer getIntegerConfig(String configKey, Integer defaultValue) throws BlinkException {
        try {
            QueryOneSysConfigReq param = new QueryOneSysConfigReq();
            param.setConfigKey(configKey);
            SysConfigVO config = getOneConfigFromCacheOrDataBase(param);
            if (config != null && config.getConfigValue() != null) {
                return Integer.parseInt(config.getConfigValue());
            }
        } catch (Exception e) {
            log.error("获取整数配置失败, configKey: {}", configKey, e);
        }
        return defaultValue;
    }

    /**
     * 根据分组键名查询配置
     *
     * @param groupKey 分组键名
     * @return 分组配置响应
     * @throws BlinkException
     */
    @Override
    public ConfigGroupRsp getConfigsByGroupKey(String groupKey) throws BlinkException {
        SysConfigGroupDO group = sysConfigGroupMapper.selectOne(
                new LambdaQueryWrapper<SysConfigGroupDO>()
                        .eq(SysConfigGroupDO::getGroupKey, groupKey)
        );

        if (Objects.isNull(group)) {
            return null;
        }

        List<Integer> groupIds = sysConfigGroupMapper.findAllSonIdByParentId(group.getId());

        List<SysConfigDO> configDOs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfigDO>()
                        .in(SysConfigDO::getGroupId, groupIds)
                        .eq(SysConfigDO::getStatus, false)
                        .orderByAsc(SysConfigDO::getId)
        );

        List<SysConfigVO> configVOs = new ArrayList<>();
        for (SysConfigDO configDO : configDOs) {
            SysConfigVO vo = new SysConfigVO();
            BeanUtils.copyProperties(configDO, vo);
            configVOs.add(vo);
        }

        ConfigGroupRsp rsp = new ConfigGroupRsp();
        rsp.setGroupId(group.getId());
        rsp.setGroupKey(group.getGroupKey());
        rsp.setGroupName(group.getGroupName());
        rsp.setConfigs(configVOs);

        return rsp;
    }

    /**
     * 批量更新配置值
     *
     * @param configs 配置列表
     * @throws BlinkException
     */
    @Override
    public void batchUpdateConfigs(List<UpdateSysConfigReq> configs) throws BlinkException {

        List<SysConfigDO> paramlist = BeanUtil.copyToList(configs, SysConfigDO.class);
        List<Integer> ids = paramlist.stream().map(SysConfigDO::getId).collect(Collectors.toList());
        List<SysConfigDO> dataList = sysConfigMapper.selectByIds(ids);
        //存在不存在的参数 传递的key与数据库中的不符合
        if (paramlist.size() != dataList.size()) {
            BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
        }

        sysConfigMapper.updateById(paramlist);

        // 统一构建缓存 Key 列表
        List<String> cacheKeys = paramlist.stream()
                .map(conf -> buildCacheKey(conf.getConfigKey()))
                .collect(Collectors.toList());

        // 立即删除所有单项缓存（Redis + 本地）
        configCacheAsyncService.deleteConfigCaches(cacheKeys);

        //是否存在前端配置项
        List<String> systemConfigKeys = cacheKeys.stream().filter(this::isSystemConfigChange).toList();

        if (!systemConfigKeys.isEmpty()) {
            //删除redis中保存的系统配置项聚合缓存
            redisClient.delete(RedisKeyConstants.SYSTEM_CONFIG);
            //清除本地聚合缓存
            cacheComponent.clearLocalCache(RedisKeyConstants.SYSTEM_CONFIG);
            log.info("[SysConfig] 清除系统配置聚合缓存 | keys: {}", systemConfigKeys);
        }

        // 异步延迟双删（通过独立 Service 调用，确保 @Async 生效）
        configCacheAsyncService.asyncDelayedDeleteBatch(cacheKeys);

        log.info("[SysConfig] 批量更新参数配置成功 | count: {}, ids: {}", paramlist.size(), ids);
    }
}
