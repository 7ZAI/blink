package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryBlinkChannelRspDTO;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.entity.BlinkChannelDO;
import com.blink.base.mapper.BlinkChannelMapper;
import com.blink.base.producer.GateWayStreamMessageProducer;
import com.blink.base.service.BlinkChannelService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.CacheDoubleDelete;
import com.blink.framework.core.crypt.RSAUtils;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 对接渠道 服务实现类
 *
 * @author binblink
 * @since 2024-07-29
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class BlinkChannelServiceImpl implements BlinkChannelService {

    private final Logger logger = LoggerFactory.getLogger(BlinkChannelServiceImpl.class);

    @Resource
    private BlinkChannelMapper channelMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Resource
    private CacheComponent cacheComponent;


    /**
     * 保存 对接渠道
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void saveBlinkChannel(AddBlinkChannelReqDTO saveParam) throws BlinkException {

        BlinkChannelDO blinkChannelDO = getNewChanelDO(saveParam);

        var queryWrapper = new LambdaQueryWrapper<BlinkChannelDO>();
        queryWrapper.eq(BlinkChannelDO::getChannelName, blinkChannelDO.getChannelName());
        BlinkChannelDO channelCheck = channelMapper.selectOne(queryWrapper);

        if (Objects.nonNull(channelCheck)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.CHANNEL_NAME_ALREADY_EXIT);
        }

        channelMapper.insert(blinkChannelDO);
    }

    /**
     * 获取新的渠道对象
     */
    private BlinkChannelDO getNewChanelDO(AddBlinkChannelReqDTO reqBody) {

        var channel = new BlinkChannelDO();
        BeanUtils.copyProperties(reqBody, channel);

        channel.setEnable(CommonConstans.SWITCH_OPEN);
        channel.setChannelId(IdUtil.simpleUUID());
        channel.setAccessToken(IdUtil.simpleUUID());

        channel.setAppKey(SecureUtil.sha1().digestHex(RandomUtil.randomString(16)));
        channel.setAppSecret(SecureUtil.hmacSha256().digestBase64(RandomUtil.randomString(32), true));

        KeyPair keyPair = RSAUtils.generateKeyPair();
        KeyPair sysKeyPair = RSAUtils.generateKeyPair();

        String channelPrivateKey = RSAUtils.generatePrivateKeyToBase64(keyPair);
        String channelPublicKey = RSAUtils.generatePublicKeyToBase64(keyPair);
        String sysPrivateKey = RSAUtils.generatePrivateKeyToBase64(sysKeyPair);
        String sysPublicKey = RSAUtils.generatePublicKeyToBase64(sysKeyPair);

        channel.setChannelPrivatekey(channelPrivateKey);
        channel.setChannelPublickey(channelPublicKey);
        channel.setSystemPublickey(sysPublicKey);
        channel.setSystemPrivatekey(sysPrivateKey);


        return channel;
    }

    /**
     * 删除 对接渠道
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void deleteBlinkChannel(DeleteBlinkChannelReqDTO deleteParam) throws BlinkException {

        var deleteCacheKeys = new ArrayList<String>();
        //批量删除
        if (deleteParam.isBatchDelete()) {
            Optional<List<BlinkChannelDO>> optional = Optional.of(channelMapper.selectBatchIds(deleteParam.getIdList()));
            List<BlinkChannelDO> channels = optional.get();

            //正在启动的渠道不允许删除
            List<BlinkChannelDO> allowDeleteList = channels.stream()
                    .filter(ch -> !CommonConstans.SWITCH_OPEN.equals(ch.getEnable()))
                    .toList();
            List<String> filterIds = allowDeleteList.stream()
                    .map(BlinkChannelDO::getChannelId)
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(filterIds)) {
                channelMapper.deleteBatchIds(filterIds);
                //删除redis 缓存
                for (BlinkChannelDO chdo : allowDeleteList) {
                    String cacheKey = RedisKeyConstans.CHANNEL_INFO + chdo.getChannelId();
                    deleteCacheKeys.add(cacheKey);
                    redisClient.delete(cacheKey);
                    gateWayStreamMessageProducer.cacheOnChange(cacheKey);
                }
            }

            //部分成功
            if (filterIds.size() < deleteParam.getIdList().size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.CHANNEL_NOT_ALLOW_DELETE);
            }

        } else {

            Optional<BlinkChannelDO> optional = Optional.ofNullable(channelMapper.selectById(deleteParam.getDeleteId()));
            if (optional.isEmpty()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_NOT_EXIST);
            }

            BlinkChannelDO blinkChannelDO = optional.get();
            //正在启动的渠道不允许删除
            if (CommonConstans.SWITCH_OPEN.equals(blinkChannelDO.getEnable())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.CHANNEL_NOT_ALLOW_DELETE);
            }
            channelMapper.deleteById(deleteParam.getDeleteId());
            String cacheKey = RedisKeyConstans.CHANNEL_INFO + blinkChannelDO.getChannelId();
            deleteCacheKeys.add(cacheKey);
            redisClient.delete(cacheKey);
            gateWayStreamMessageProducer.cacheOnChange(cacheKey);
        }

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (String key : deleteCacheKeys) {
            redisClient.delete(key);
            gateWayStreamMessageProducer.cacheOnChange(key);
        }

    }

    /**
     * 更新 对接渠道
     * 仅能更改开关 名称 其他的秘钥重新生成有专门接口
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    @Override
//    @CacheDoubleDelete(keyPrefix = RedisKeyConstans.CHANNEL_INFO, fieldName = "channelId")
    public void modifyBlinkChannel(UpdateBlinkChannelReqDTO updateParam) throws BlinkException {
        String cacheKey = RedisKeyConstans.CHANNEL_INFO + updateParam.getChannelId();

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        BlinkChannelDO channel = channelMapper.selectById(updateParam.getChannelId());
        //数据不存在
        if (Objects.isNull(channel)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_NOT_EXIST);
        }

        BeanUtil.copyProperties(updateParam, channel);
        channelMapper.updateById(channel);

        //延迟删除
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);
    }

    /**
     * 查询 对接渠道 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    @Override
    public QueryBlinkChannelRspDTO getBlinkChannelList(QueryBlinkChannelReqDTO queryParam) throws BlinkException {

        var pageRsp = new QueryBlinkChannelRspDTO();
        QueryBlinkChannelRspDTO result = PageUtils.queryPage(queryParam, () -> channelMapper.findBlinkChannelList(queryParam), pageRsp);

        return result;
    }


    /**
     * 根据查询条件查询单个渠道信息
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    @Override
    public ChannelVO getChannel(QueryOneChannelReqDTO queryParam) throws BlinkException {
        BlinkChannelDO channelDO =  channelMapper.selectOne(new LambdaQueryWrapper<BlinkChannelDO>()
                .eq(StrUtil.isNotBlank(queryParam.getChannelId()), BlinkChannelDO::getChannelId, queryParam.getChannelId())
                .eq(StrUtil.isNotBlank(queryParam.getChannelName()), BlinkChannelDO::getChannelName, queryParam.getChannelName())
                .eq(StrUtil.isNotBlank(queryParam.getAppKey()), BlinkChannelDO::getAppKey, queryParam.getAppKey()));

        var channelVO = new ChannelVO();
        BeanUtils.copyProperties(channelDO,channelVO);

        return channelVO;
    }

    /**
     * 刷新渠道密钥对 重新生成密钥对
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    @Override
    public BlinkChannelDO refreshChannelKey(QueryOneChannelReqDTO queryParam) throws BlinkException {
        String cacheKey = RedisKeyConstans.CHANNEL_INFO + queryParam.getChannelId();

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        LambdaQueryWrapper<BlinkChannelDO> queryWrapper = new LambdaQueryWrapper<BlinkChannelDO>()
                .eq(StrUtil.isNotBlank(queryParam.getChannelName()), BlinkChannelDO::getChannelName, queryParam.getChannelName())
                .eq(StrUtil.isNotBlank(queryParam.getAppKey()), BlinkChannelDO::getAppKey, queryParam.getAppKey())
                .eq(StrUtil.isNotBlank(queryParam.getChannelId()), BlinkChannelDO::getChannelId, queryParam.getChannelId());

        BlinkChannelDO channel = channelMapper.selectOne(queryWrapper);
        //不存在
        if (Objects.isNull(channel)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.CHANNEL_NOT_EXIST);
        }

        KeyPair keyPair = RSAUtils.generateKeyPair();

        String publicKey = RSAUtils.generatePublicKeyToBase64(keyPair);
        String privateKey = RSAUtils.generatePrivateKeyToBase64(keyPair);

        channel.setChannelPublickey(publicKey);
        channel.setChannelPrivatekey(privateKey);


        if (channelMapper.updateById(channel) <= 0) {
            BlinkException.throwBusinessException();
        }

        //延迟删除
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        return channel;
    }

    /**
     * 刷新系统密钥对 重新生成密钥对
     *
     * @param queryParam
     * @return {@link BlinkChannelDO}
     * @throws Throwable
     */
    @Override
    public BlinkChannelDO refreshSystemKey(QueryOneChannelReqDTO queryParam) throws BlinkException {

        String cacheKey = RedisKeyConstans.CHANNEL_INFO + queryParam.getChannelId();

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        LambdaQueryWrapper<BlinkChannelDO> queryWrapper = new LambdaQueryWrapper<BlinkChannelDO>()
                .eq(StrUtil.isNotBlank(queryParam.getChannelName()), BlinkChannelDO::getChannelName, queryParam.getChannelName())
                .eq(StrUtil.isNotBlank(queryParam.getAppKey()), BlinkChannelDO::getAppKey, queryParam.getAppKey())
                .eq(StrUtil.isNotBlank(queryParam.getChannelId()), BlinkChannelDO::getChannelId, queryParam.getChannelId());

        BlinkChannelDO channel = channelMapper.selectOne(queryWrapper);
        //不存在
        if (Objects.isNull(channel)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.CHANNEL_NOT_EXIST);
        }

        KeyPair keyPair = RSAUtils.generateKeyPair();

        String publicKey = RSAUtils.generatePublicKeyToBase64(keyPair);
        String privateKey = RSAUtils.generatePrivateKeyToBase64(keyPair);

        channel.setSystemPublickey(publicKey);
        channel.setSystemPrivatekey(privateKey);


        if (channelMapper.updateById(channel) <= 0) {
            BlinkException.throwBusinessException();
        }

        //延迟删除
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        return channel;

    }

}
