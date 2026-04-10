package com.blink.gateway.admin.dubbo;

import cn.hutool.core.bean.BeanUtil;
import com.blink.gateway.base.dto.vo.SysConfigVO;
import com.blink.gateway.base.service.SysConfigService;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.service.ChannelService;
import com.blink.gateway.admin.service.MessageAckService;
import com.blink.gateway.dto.req.MessageAckReq;
import com.blink.gateway.dto.req.QueryChannelConfigReq;
import com.blink.gateway.dto.req.QueryOneChannelReq;
import com.blink.gateway.dto.rsp.MessageAckRsp;
import com.blink.gateway.dto.vo.ChannelVO;
import com.blink.gateway.dubbo.service.GatewayAdminDubboService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.blink.framework.core.data.CoreConstant.IO_THREADPOOL;
import static com.blink.gateway.admin.constants.ErrCodeConstant.CHANNEL_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.CONFIG_NOT_EXIST;

/**
 * Dubbo 基础服务实现类
 * <p>
 * 作为 Dubbo Provider，为其他服务提供基础数据查询能力。
 * 本类仅做服务包装和对象转换，不涉及具体业务逻辑实现。
 * </p>
 *
 * @author blink
 * @since 1.0.0
 */
@Slf4j
@DubboService(interfaceClass = GatewayAdminDubboService.class)
@Service
public class GatewayAdminDubboServiceImpl implements GatewayAdminDubboService {

    /**
     * IO密集型线程池，用于 Dubbo 异步调用（可选注入）
     */
    private Executor ioThreadPool;

    @Resource
    private ChannelService channelService;

    @Resource
    private SysConfigService configService;

    @Resource
    private MessageAckService messageAckService;

    /**
     * 尝试获取 IO 线程池 Bean，如果不存在则使用默认 ForkJoinPool
     */
    @Autowired
    public void setIoThreadPool(ApplicationContext applicationContext) {
        try {
            this.ioThreadPool = applicationContext.getBean(IO_THREADPOOL, Executor.class);
        } catch (Exception e) {
            log.info("IO线程池未配置，将使用默认 ForkJoinPool");
        }
    }


    /*
     * 根据appKey获取渠道信息
     * <p>
     * 查询渠道详情，用于网关鉴权和渠道验证
     * </p>
     *
     * @param reqDto 请求参数，包含appKey
     * @return ResponseDTO<ChannelInfoRedisDO> 渠道信息响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<ChannelInfoRedisDO> getChannelInfo(RequestDTO<QueryOneChannelReq> reqDto) {
        try {
            QueryOneChannelReq req = reqDto.getBody();
            ResponseDTO<ChannelVO> rspDto   = channelService.getChannel(req);
            //为空
            if(Objects.isNull(rspDto)){
                BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
            }
            ChannelVO channelVO = rspDto.getBody();
            if (Objects.isNull(channelVO)) {
                BlinkException.throwBusinessException(CHANNEL_NOT_EXIST);
            }

            // 使用 BeanUtil 进行对象属性拷贝
            ChannelInfoRedisDO channelInfo = BeanUtil.copyProperties(channelVO, ChannelInfoRedisDO.class);

            return ResponseDTO.newSuccessInstance(channelInfo);
        } catch (BlinkException e) {
            log.warn("获取渠道信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取渠道信息异常", e);
            throw new BlinkException("获取渠道信息异常: " + e.getMessage(), e, "GATE0018");
        }
    }

    /**
     * 根据配置key获取配置信息
     *
     * @param reqDto 请求参数，包含configKey
     * @return ResponseDTO<SysConfigCacheDO> 配置信息响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<SysConfigCacheDO> getChannelConfig(RequestDTO<QueryChannelConfigReq> reqDto) {
        try {
            QueryChannelConfigReq req = reqDto.getBody();
            SysConfigVO configVO = configService.getChannelConfig(req);

            if (Objects.isNull(configVO) || Objects.isNull(configVO.getConfigKey())) {
                BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
            }

            // 使用 BeanUtil 进行对象属性拷贝
            SysConfigCacheDO configCache = BeanUtil.copyProperties(configVO, SysConfigCacheDO.class);

            return ResponseDTO.newSuccessInstance(configCache);
        } catch (BlinkException e) {
            log.warn("获取配置信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取配置信息异常", e);
            throw new BlinkException("获取配置信息异常: " + e.getMessage(), e, "GATE0019");
        }
    }

    // ==================== 异步方法实现 ====================

    @Override
    public CompletableFuture<ResponseDTO<ChannelInfoRedisDO>> getChannelInfoAsync(RequestDTO<QueryOneChannelReq> reqDto) {
        // 如果线程池存在则使用，否则使用默认的 ForkJoinPool
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getChannelInfo(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getChannelInfo(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<SysConfigCacheDO>> getChannelConfigAsync(RequestDTO<QueryChannelConfigReq> reqDto) {
        // 如果线程池存在则使用，否则使用默认的 ForkJoinPool
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getChannelConfig(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getChannelConfig(reqDto));
    }

    // ==================== 消息 ACK 确认 ====================

    @Override
    public CompletableFuture<ResponseDTO<MessageAckRsp>> ackMessageAsync(RequestDTO<MessageAckReq> reqDto) {
        MessageAckReq req = reqDto.getBody();

        // ACK 调用失败不影响主流程，仅记录日志
        CompletableFuture<ResponseDTO<MessageAckRsp>> future = CompletableFuture.supplyAsync(() -> {
            try {
                MessageAckRsp rsp = messageAckService.ackMessage(req);
                return ResponseDTO.newSuccessInstance(rsp);
            } catch (Exception e) {
                log.error("[DubboACK] 处理 ACK 确认异常 | streamId: {}, msgId: {}, error: {}",
                        req.getStreamId(), req.getMsgId(), e.getMessage(), e);
                MessageAckRsp failRsp = new MessageAckRsp();
                failRsp.setAcked(false);
                failRsp.setMessage("ACK 处理异常: " + e.getMessage());
                return ResponseDTO.newSuccessInstance(failRsp);
            }
        }, ioThreadPool != null ? ioThreadPool : ForkJoinPool.commonPool());

        // 异步执行，失败仅记录日志，不抛出异常影响调用方
        future.whenComplete((rsp, ex) -> {
            if (ex != null) {
                log.error("[DubboACK] ACK 异步调用异常 | streamId: {}, msgId: {}", req.getStreamId(), req.getMsgId(), ex);
            } else if (rsp != null && rsp.getBody() != null) {
                log.debug("[DubboACK] ACK 完成 | result: {}", rsp.getBody().getAcked());
            }
        });

        return future;
    }

}