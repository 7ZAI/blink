package com.blink.framework.mq.service;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.mq.entity.MqMsgReceDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息消费记录表 服务类
 * </p>
 *
 * @author binblink
 * @since 2023-11-13
 */
@Service
public interface MqMsgReceService extends IService<MqMsgReceDO> {

    /**
     * 保存消息 在mq消息接收时
     * @throws Exception
     */
    void insertMqMsgWhenReceive(MqGenericDTO mqDto) throws BlinkException;

    /**
     * 更新 mq 接收消息状态
     * @throws Exception
     */
    void updateMqReceiveMsgSts(MqGenericDTO mqDto,Integer sts) throws BlinkException;

}
