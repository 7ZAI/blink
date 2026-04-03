package com.blink.framework.mq.service;

import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.entity.MqMsgSendDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息发送记录表 服务类
 * </p>
 *
 * @author binblink
 * @since 2023-11-13
 */
@Service
public interface MqMsgSendService extends IService<MqMsgSendDO> {


    /**
     * 保存消息 在mq消息发送时
     * @throws Exception
     */
    void insertMqMsgWhenSend(MqGenericDTO mqDto) throws Exception;


    /**
     * 更新mq消息状态
     * @throws Exception
     */
    void updateMqMsgSts(String msgId,Integer sts) throws Exception;

}
