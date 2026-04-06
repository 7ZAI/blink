package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.ChannelSecretKey;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.jwt.JwtConfig;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.component.SecretConfigComponent;
import com.blink.gateway.admin.constants.ConfigValueConstant;
import com.blink.gateway.admin.constants.ErrCodeConstant;
import com.blink.gateway.admin.dto.req.AddChannelReq;
import com.blink.gateway.admin.dto.req.DeleteChannelReq;
import com.blink.gateway.admin.dto.req.GetChannelSecretReq;
import com.blink.gateway.admin.dto.req.IssueChannelTokenReq;
import com.blink.gateway.admin.dto.req.QueryChannelReq;
import com.blink.gateway.admin.dto.req.RefreshChannelKeyReq;
import com.blink.gateway.admin.dto.req.UpdateChannelReq;
import com.blink.gateway.admin.dto.rsp.ChannelSecretRsp;
import com.blink.gateway.admin.dto.rsp.ChannelTokenRsp;
import com.blink.gateway.admin.dto.rsp.QueryChannelRsp;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.ChannelAsyncSyncService;
import com.blink.gateway.admin.service.ChannelSecretSyncService;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dto.vo.ChannelVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.blink.gateway.admin.constants.ErrCodeConstant.CHANNEL_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.CONFIG_PUSH_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_NOT_EXIST;

/**
 * 渠道管理服务实现类
 *
 * @author binblink
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class ChannelServiceImpl implements ChannelService {

    @Resource
    private GaChannelMapper channelMapper;

    @Resource
    private RedisClient redisClient;

    @Resource
    private GateWayStreamMessageProducer gateWayStreamMessageProducer;

    @Resource
    private SecretConfigComponent secretConfigComponent;

    @Resource
    private ChannelAsyncSyncService channelAsyncSyncService;

    @Resource
    private ChannelSecretSyncService channelSecretSyncService;

    @Override
    public ResponseDTO<QueryChannelRsp> getChannelList(QueryChannelReq req) throws BlinkException {
        QueryChannelRsp pageRsp = new QueryChannelRsp();

        // 使用 LambdaQueryWrapper 构建动态查询条件
        LambdaQueryWrapper<GaChannelDO> queryWrapper = new LambdaQueryWrapper<GaChannelDO>()
                .like(StrUtil.isNotBlank(req.getChannelName()), GaChannelDO::getChannelName, req.getChannelName())
                .eq(req.getEnable() != null, GaChannelDO::getEnable, req.getEnable());
        // 使用 PageUtils 执行分页查询
        return ResponseDTO.newSuccessInstance(PageUtils.queryPage(req, () -> channelMapper.selectList(queryWrapper), pageRsp));
    }

    @Override
    public ResponseDTO<ChannelVO> getChannel(QueryOneChannelReq req) throws BlinkException {
        GaChannelDO channelDO = channelMapper.selectOne(
                new LambdaQueryWrapper<GaChannelDO>()
                        .eq(StrUtil.isNotBlank(req.getChannelId()), GaChannelDO::getChannelId, req.getChannelId())
                        .eq(StrUtil.isNotBlank(req.getChannelName()), GaChannelDO::getChannelName, req.getChannelName())
        );

        ChannelVO channelVO = new ChannelVO();
        if (ObjectUtil.isNotNull(channelDO)) {
            BeanUtils.copyProperties(channelDO, channelVO);
        }

        return ResponseDTO.newSuccessInstance(channelVO);
    }

    @Override
    public ResponseDTO<ChannelSecretRsp> getChannelSecret(GetChannelSecretReq req) throws BlinkException {
        // 先查询渠道获取 appKey
        GaChannelDO channelDO = channelMapper.selectById(req.getChannelId());
        if (ObjectUtil.isNull(channelDO)) {
            BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
        }

        ChannelSecretRsp rsp = new ChannelSecretRsp();
        String secretField = req.getSecretField();

        try {
            // 从 Nacos 配置中心获取密钥信息
            ChannelSecretKey secretKey = secretConfigComponent.getChannelSecretKey(channelDO.getAppKey());
            if (ObjectUtil.isNull(secretKey)) {
                BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
            }

            // 根据请求的字段类型返回对应的密钥值
            String secretValue = switch (secretField) {
                case "appSecret" -> secretKey.getAppSecret();
                case "systemPublickey" -> secretKey.getSystemPublickey();
                case "systemPrivatekey" -> secretKey.getSystemPrivatekey();
                case "channelPublickey" -> secretKey.getChannelPublicKey();
                case "channelPrivatekey" -> secretKey.getChannelPrivateKey();
                default -> null;
            };

            if (StrUtil.isBlank(secretValue)) {
                BlinkException.throwBusinessException(DATA_NOT_EXIST);
            }

            rsp.setSecretValue(secretValue);
            log.info("[Channel] 获取渠道密钥成功 | channelId: {}, field: {}", req.getChannelId(), secretField);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Channel] 获取渠道密钥失败 | channelId: {}, field: {}, error: {}", req.getChannelId(), secretField, e.getMessage(), e);
            throw new BlinkException("获取渠道密钥失败: " + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<EmptyBody> saveChannel(AddChannelReq req) throws BlinkException {
        GaChannelDO blinkChannelDO = createNewChannelDO(req);

        // 检查渠道名是否已存在
        LambdaQueryWrapper<GaChannelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GaChannelDO::getChannelName, blinkChannelDO.getChannelName());
        GaChannelDO channelCheck = channelMapper.selectOne(queryWrapper);

        if (ObjectUtil.isNotNull(channelCheck)) {
            BlinkException.throwBusinessException(ErrCodeConstant.CHANNEL_NAME_ALREADY_EXIT);
        }

        channelMapper.insert(blinkChannelDO);

        // 异步添加渠道密钥配置到 Nacos
        channelSecretSyncService.addChannelSecretConfigAsync(blinkChannelDO);

        // 构建渠道信息对象用于同步
        ChannelInfoRedisDO channelInfo = BeanUtil.copyProperties(blinkChannelDO, ChannelInfoRedisDO.class);

        // 异步同步渠道信息到网关（新增缓存，operator="A"）
        channelAsyncSyncService.syncAddChannel(blinkChannelDO.getAppKey(), channelInfo);

        log.info("[Channel] 新增渠道成功 | channelId: {}, channelName: {}, appKey: {}",
                blinkChannelDO.getChannelId(), blinkChannelDO.getChannelName(), blinkChannelDO.getAppKey());

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<ChannelVO> modifyChannel(UpdateChannelReq req) throws BlinkException {
        GaChannelDO channel = channelMapper.selectById(req.getChannelId());
        if (ObjectUtil.isNull(channel)) {
            BlinkException.throwBusinessException(DATA_NOT_EXIST);
        }

        // 更新渠道信息
        BeanUtil.copyProperties(req, channel);
        channelMapper.updateById(channel);

        // 构建渠道信息对象用于同步
        ChannelInfoRedisDO channelInfo = BeanUtil.copyProperties(channel, ChannelInfoRedisDO.class);

        // 异步同步渠道信息到网关（直接更新缓存，operator="M"，不再先删除 Redis 缓存）
        channelAsyncSyncService.syncModifyChannel(channel.getAppKey(), channelInfo);

        log.info("[Channel] 更新渠道成功 | channelId: {}, appKey: {}", channel.getChannelId(), channel.getAppKey());

        ChannelVO channelVO = new ChannelVO();
        BeanUtils.copyProperties(channel, channelVO);
        return ResponseDTO.newSuccessInstance(channelVO);
    }

    @Override
    public ResponseDTO<EmptyBody> deleteChannel(DeleteChannelReq req) throws BlinkException {
        Optional<GaChannelDO> optional = Optional.ofNullable(channelMapper.selectById(req.getChannelId()));
        if (optional.isEmpty()) {
            BlinkException.throwBusinessException(DATA_NOT_EXIST);
        }

        GaChannelDO blinkChannelDO = optional.get();
        // 正在启用的渠道不允许删除
        if (ConfigValueConstant.SWITCH_OPEN.equals(blinkChannelDO.getEnable())) {
            BlinkException.throwBusinessException(ErrCodeConstant.CHANNEL_NOT_ALLOW_DELETE);
        }

        channelMapper.deleteById(req.getChannelId());

        // 删除 Redis 缓存
        String cacheKey = RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX + blinkChannelDO.getAppKey();
        redisClient.delete(cacheKey);

        // 异步同步删除通知到 gateway-reactive（operator="D"）
        channelAsyncSyncService.syncDeleteChannel(blinkChannelDO.getAppKey());

        // 异步删除渠道密钥配置
        channelSecretSyncService.deleteChannelSecretConfigAsync(blinkChannelDO.getAppKey());

        log.info("[Channel] 删除渠道成功 | channelId: {}, appKey: {}", req.getChannelId(), blinkChannelDO.getAppKey());

        return ResponseDTO.newSuccessInstance();
    }

    @Override
    public ResponseDTO<GaChannelDO> refreshChannelKey(RefreshChannelKeyReq req) throws BlinkException {
        LambdaQueryWrapper<GaChannelDO> queryWrapper = new LambdaQueryWrapper<GaChannelDO>()
                .eq(StrUtil.isNotBlank(req.getChannelId()), GaChannelDO::getChannelId, req.getChannelId());

        GaChannelDO channel = channelMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(channel)) {
            BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
        }

        try {
            // 刷新配置中心密钥配置
            secretConfigComponent.refreshChannelKeyConfig(channel.getAppKey());
        } catch (Exception e) {
            log.error("[Channel] 刷新渠道密钥失败 | channelId: {}, error: {}", channel.getChannelId(), e.getMessage(), e);
            throw new BlinkException("刷新渠道密钥失败: " + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }

        // 发送删除通知，让 gateway-reactive 重新获取最新配置
        String cacheKey = RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX + channel.getAppKey();
        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        log.info("[Channel] 刷新渠道密钥成功 | channelId: {}, appKey: {}", channel.getChannelId(), channel.getAppKey());

        return ResponseDTO.newSuccessInstance(channel);
    }

    @Override
    public ResponseDTO<GaChannelDO> refreshSystemKey(RefreshChannelKeyReq req) throws BlinkException {
        LambdaQueryWrapper<GaChannelDO> queryWrapper = new LambdaQueryWrapper<GaChannelDO>()
                .eq(StrUtil.isNotBlank(req.getChannelId()), GaChannelDO::getChannelId, req.getChannelId());

        GaChannelDO channel = channelMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(channel)) {
            BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
        }

        try {
            // 刷新配置中心密钥配置
            secretConfigComponent.refreshSystemKeyConfig(channel.getAppKey());
        } catch (Exception e) {
            log.error("[Channel] 刷新系统密钥失败 | channelId: {}, error: {}", channel.getChannelId(), e.getMessage(), e);
            throw new BlinkException("刷新系统密钥失败: " + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }

        // 发送删除通知，让 gateway-reactive 重新获取最新配置
        String cacheKey = RedisCacheKeyConstant.CHANNEL_CACHE_PREFIX + channel.getAppKey();
        redisClient.delete(cacheKey);
        gateWayStreamMessageProducer.cacheOnChange(cacheKey);

        log.info("[Channel] 刷新系统密钥成功 | channelId: {}, appKey: {}", channel.getChannelId(), channel.getAppKey());

        return ResponseDTO.newSuccessInstance(channel);
    }

    @Override
    public ResponseDTO<ChannelTokenRsp> issueChannelToken(IssueChannelTokenReq req) throws BlinkException {
        ChannelTokenRsp rsp = new ChannelTokenRsp();
        String appKey = req.getAppKey();

        GaChannelDO channel = channelMapper.selectOne(new LambdaQueryWrapper<GaChannelDO>()
                .eq(GaChannelDO::getAppKey, appKey));

        if (ObjectUtil.isNull(channel)) {
            BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
        }

        try {
            // 获取密钥
            ChannelSecretKey channelSecretKey = secretConfigComponent.getChannelSecretKey(appKey);
            if (ObjectUtil.isNull(channelSecretKey)) {
                BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
            }

            // 验证私钥
            String appSecret = req.getAppSecret();

            if (!channelSecretKey.getAppSecret().equals(appSecret)) {
                BlinkException.throwBusinessException(ErrCodeConstant.ERR_APP_SECRET);
            }

            JwtProvider jwtProvider = getJwtProvider(channelSecretKey);
            LocalDateTime now = LocalDateTime.now();
            // 生成jwt token
            String token = jwtProvider.generateAccessToken(channel.getRelaUserId(), null, null);
            rsp.setToken(token);
            rsp.setExpiresIn(ConfigValueConstant.LONG_MINUTES_15_OF_MILL);
            rsp.setExpireTime(now.plusMinutes(ConfigValueConstant.LONG_MINUTES_15));

            log.info("[Channel] 签发渠道Token成功 | appKey: {}", appKey);

        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Channel] 签发Token失败 | appKey: {}, error: {}", appKey, e.getMessage(), e);
            throw new BlinkException("签发Token失败: " + e.getMessage(), e, CONFIG_PUSH_FAILED);
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取JWT提供者
     *
     * @param channelSecretKey 渠道密钥
     * @return JWT提供者
     */
    private static JwtProvider getJwtProvider(ChannelSecretKey channelSecretKey) {

        JwtProvider jwtProvider = new JwtProvider();

        JwtConfig jwtConfig = new JwtConfig(channelSecretKey.getTokenSecret());
        jwtConfig.setAudience(channelSecretKey.getChannelName());
        jwtConfig.setIssuer("gateway-admin");
        // 15分钟过期 短期token 后续参数化配置
        jwtConfig.setAccessTokenExpiration(ConfigValueConstant.LONG_MINUTES_15_OF_MILL);

        jwtProvider.setJwtConfig(jwtConfig);
        return jwtProvider;
    }

    /**
     * 创建新的渠道对象
     *
     * @param req 新增渠道请求参数
     * @return 渠道实体对象
     */
    private GaChannelDO createNewChannelDO(AddChannelReq req) {
        GaChannelDO channel = new GaChannelDO();
        BeanUtils.copyProperties(req, channel);

        channel.setEnable(ConfigValueConstant.SWITCH_OPEN);
        channel.setChannelId(IdUtil.simpleUUID());
        channel.setAccessToken(IdUtil.simpleUUID());
        channel.setAppKey(SecureUtil.sha1().digestHex(RandomUtil.randomString(16)));

        return channel;
    }
}