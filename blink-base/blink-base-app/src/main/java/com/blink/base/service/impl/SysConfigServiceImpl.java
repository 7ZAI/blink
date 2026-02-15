package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysConfigRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.entity.SysConfigDO;
import com.blink.base.mapper.SysConfigGroupMapper;
import com.blink.base.mapper.SysConfigMapper;
import com.blink.base.producer.GateWayStreamMessageProducer;
import com.blink.base.service.SysConfigService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.blink.base.constans.BaseErrCodeConstant.CONFIG_NAME_REPEAT;
import static com.blink.base.constans.BaseErrCodeConstant.CONFIG_NOT_EXIST;
import static com.blink.base.constans.RedisKeyConstans.GATEWAY_CONFIG_PREFIX;

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
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

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
        //TODO 删除缓存
    }


    /**
     * 删除 参数配置表
     *
     * @param deleteParam
     * @throws BlinkException
     */
    @Override
    public void deleteSysConfig(DeleteSysConfigReq deleteParam) throws BlinkException {

        if (deleteParam.isBatchDelete()) {
            sysConfigMapper.deleteBatchIds(deleteParam.getIdList());
        } else {
            sysConfigMapper.deleteById(deleteParam.getDeleteId());
        }
        //TODO 删除缓存
    }

    /**
     * 更新 参数配置表
     *
     * @param updateParam
     * @throws BlinkException
     */
    @Override
    public void modifySysConfig(UpdateSysConfigReq updateParam) throws BlinkException {

        String key = updateParam.getConfigKey();

        String cacheKey = GATEWAY_CONFIG_PREFIX + key;

        Object configCahce = redisClient.get(cacheKey);

        redisClient.delete(cacheKey);

        //属于 gateway 配置
        if (Objects.nonNull(configCahce)) {
            gateWayStreamMessageProducer.cacheOnChange(cacheKey);
        }

        var sysConfigDO = new SysConfigDO();
        SysConfigDO oldOne = sysConfigMapper.selectById(updateParam.getId());

        //参数不存在
        if (Objects.isNull(oldOne)) {
            BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
        }

        BeanUtil.copyProperties(updateParam, sysConfigDO);

        sysConfigMapper.updateById(sysConfigDO);

        //延迟删除 延迟时间 > 请求时间 + redis 设置值的时间 也就是getOneConfig()接口花费时间
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        redisClient.delete(GATEWAY_CONFIG_PREFIX + key);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);
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

//        if(StrUtil.isNotBlank(param.getConfigKey())){
//            redisClient.get()
//        }

//        cacheComponent.getFromCacheOrDB()

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

        //TODO 判断是否属于gateway的config参数

        String cacheKey = GATEWAY_CONFIG_PREFIX + queryParam.getConfigKey();

        SysConfigDO configDO = (SysConfigDO) cacheComponent.getFromCacheOrDB(cacheKey, () -> sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfigDO>()
                .eq(StrUtil.isNotBlank(queryParam.getConfigKey()), SysConfigDO::getConfigKey, queryParam.getConfigKey())
                .eq(Objects.nonNull(queryParam.getId()), SysConfigDO::getId, queryParam.getId())));

        var vo = new SysConfigVO();
        BeanUtils.copyProperties(configDO, vo);
        return vo;
    }


    private void gatewayConfigChange() {

    }
}
