package com.blink.framework.mq.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.entity.MqMsgReceDO;
import com.blink.framework.mq.mapper.MqMsgReceMapper;
import com.blink.framework.mq.service.MqMsgReceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>
 * 消息消费记录表 服务实现类
 * </p>
 *
 * @author binblink
 * @since 2023-11-13
 */
@Service
public class MqMsgReceServiceImpl extends ServiceImpl<MqMsgReceMapper, MqMsgReceDO> implements MqMsgReceService {


    private final Logger logger = LoggerFactory.getLogger(MqMsgReceServiceImpl.class);

    @Override
    public void insertMqMsgWhenReceive(MqGenericDTO mqDto) throws BlinkException {



        QueryWrapper<MqMsgReceDO> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(MqMsgReceDO::getMsgId,mqDto.getMsgId())
                        .eq(MqMsgReceDO::getReceiveId,mqDto.getReceiver());

        MqMsgReceDO mqMsgReceDO = getOne(queryWrapper);

        LocalDateTime localDateTime = LocalDateTime.now();

        //已存在记录
        if(Objects.nonNull(mqMsgReceDO)){
            //业务幂等 抛出业务异常
            if(MqConstant.MQ_STS_SUCCESS.equals(mqMsgReceDO.getReceiveSts())){

                logger.warn("此条消息为重复消息，消息: {}", mqMsgReceDO);

                throw new BlinkException(BlinkErrorCodeEnum.RABBITMQ_RECEIVE_CODE_ERROR.getCode());
            }

            return;
        }

        mqMsgReceDO = new MqMsgReceDO();

        BeanUtils.copyProperties(mqDto,mqMsgReceDO);



        try {
            mqMsgReceDO.setMqContext(JSON.toJSONString(mqDto.getBody()));
        } catch (Exception e) {
            throw new BlinkException(e,e.getMessage());
        }


        mqMsgReceDO.setReceiveTime(localDateTime);
        mqMsgReceDO.setReceiveSts(MqConstant.MQ_STS_NO_HANDLE);
        mqMsgReceDO.setReceiveId("0000");

        mqMsgReceDO.setSendSys(mqDto.getSender());
        mqMsgReceDO.setReceiveSys(mqDto.getReceiver());
        mqMsgReceDO.setMqMode(MqConstant.MQ_CONSUMER_MODE_SINGLE);

        save(mqMsgReceDO);
    }


    @Override
    public void updateMqReceiveMsgSts(MqGenericDTO mqDto, Integer sts) throws BlinkException {
        QueryWrapper<MqMsgReceDO> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(MqMsgReceDO::getMsgId,mqDto.getMsgId())
                .eq(MqMsgReceDO::getReceiveId,mqDto.getReceiver());

        MqMsgReceDO mqMsgReceDO = getOne(queryWrapper);

        //健壮性代码 防止万一
        if(Objects.isNull(mqMsgReceDO)){
            logger.warn("要更新的消息不存在！ msgId: {},receiveId: {}",mqDto.getMsgId(),mqDto.getReceiver());
            return;
        }

        mqMsgReceDO.setConsumerTimes(mqMsgReceDO.getConsumerTimes() + 1);
        mqMsgReceDO.setReceiveSts(sts);

        UpdateWrapper<MqMsgReceDO> updateWrapper = new UpdateWrapper<>();

        updateWrapper.lambda().eq(MqMsgReceDO::getMsgId,mqDto.getMsgId())
                .eq(MqMsgReceDO::getReceiveId,mqDto.getReceiver());

        update(mqMsgReceDO,updateWrapper);
    }
}
