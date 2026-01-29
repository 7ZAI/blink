package com.blink.framework.mq.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.entity.MqMsgSendDO;
import com.blink.framework.mq.mapper.MqMsgSendMapper;
import com.blink.framework.mq.service.MqMsgSendService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息发送记录表 服务实现类
 * </p>
 *
 * @author binblink
 * @since 2023-11-13
 */
@Service
public class MqMsgSendServiceImpl extends ServiceImpl<MqMsgSendMapper, MqMsgSendDO> implements MqMsgSendService {

    @Override
    public void insertMqMsgWhenSend(MqGenericDTO mqDto) throws Exception {


        MqMsgSendDO mqMsgSendDO = new MqMsgSendDO();

        mqMsgSendDO.setMqContext(JacksonUtil.toJson(mqDto.getBody()));

        BeanUtils.copyProperties(mqDto,mqMsgSendDO);

        LocalDateTime localDateTime = LocalDateTime.now();

        mqMsgSendDO.setMqContextClass(mqDto.getProducerBean());
        mqMsgSendDO.setSendTime(localDateTime);
        mqMsgSendDO.setSendSts(MqConstant.MQ_STS_NO_HANDLE);
        mqMsgSendDO.setLastSendTime(localDateTime);
        mqMsgSendDO.setMqMode(MqConstant.MQ_CONSUMER_MODE_SINGLE);

        save(mqMsgSendDO);

    }

    /**
     *
     * @param msgId
     * @param sts
     * @throws Exception
     */
    @Override
    public void updateMqMsgSts(String msgId,Integer sts) throws Exception {

        MqMsgSendDO mqMsgSendDO = getById(msgId);
        Integer originSts = mqMsgSendDO.getSendSts();

        //由于return可能在 confirm之前执行  防止同时更新时状态3 被覆盖 只能由初始状态更改
        if(originSts.equals(MqConstant.MQ_STS_NO_HANDLE)){

            LambdaUpdateWrapper<MqMsgSendDO> updateWrapper = new LambdaUpdateWrapper<>();

            updateWrapper.eq(MqMsgSendDO::getMsgId,msgId);
            //由于return 和confirm回调更新的事务可能同时执行  防止同时更新时状态3 被覆盖
            updateWrapper.eq(MqMsgSendDO::getSendSts,originSts);

            mqMsgSendDO.setSendSts(sts);
            update(mqMsgSendDO,updateWrapper);
        }


    }
}
