package com.blink.framework.mq.aop;

import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.MqGenericDTO;
import com.blink.framework.mq.constant.MqConstant;
import com.blink.framework.mq.service.MqMsgReceService;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReceiveMqMessageAspect {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveMqMessageAspect.class);

    @Resource
    private MqMsgReceService mqMsgReceService;

    @Pointcut("execution(* com.blink..*.*(..)) && @annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public void pointCut(){

    }

    @Before("pointCut()")
    public void receiveMessage(JoinPoint jp ) throws Throwable {

        Object[] args = jp.getArgs();

        if(args.length <= 0){
            throw new Exception("代理方法无参数！");
        }

        Object param = args[0];
        MqGenericDTO dto = (MqGenericDTO) param;

        //TODO 临时系统id 以后会有设计一个上下文类来保存系统环境 包含系统id信息
        dto.setReceiver("0000");

        logger.info("mq 消费端入参报文： {} " , dto);

        mqMsgReceService.insertMqMsgWhenReceive(dto);

    }

    /**
     * 成功返回 说明消费成功
     * @param jp
     */
    @AfterReturning("pointCut()")
    public void updateSuccess(JoinPoint jp){

        Object[] args = jp.getArgs();

        Object param = args[0];

        MqGenericDTO dto = (MqGenericDTO) param;

        dto.setReceiver(BlinkRequestContextHolder.getAppName());

        mqMsgReceService.updateMqReceiveMsgSts(dto, MqConstant.MQ_STS_SUCCESS);
    }

    @AfterThrowing(value = "pointCut()",throwing = "ex")
    public void failConsume(JoinPoint jp,Exception ex){


        Object[] args = jp.getArgs();

        Object param = args[0];

        MqGenericDTO dto = (MqGenericDTO) param;

        dto.setReceiver(BlinkRequestContextHolder.getAppName());

        logger.info("mq 消息消费失败 ！ 错误信息： {} " ,ex);

        mqMsgReceService.updateMqReceiveMsgSts(dto, MqConstant.MQ_STS_FAIL);
    }


}
