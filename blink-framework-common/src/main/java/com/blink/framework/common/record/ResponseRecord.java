package com.blink.framework.common.record;

import com.blink.framework.common.constrant.ResponseMsgType;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.EmptyBody;

import java.io.Serializable;

/**
 * ResponseMessageDTO 通用响应数据传输对象
 * record 版本
 * @param <T>
 */
public record ResponseRecord<T>(

    /**
     * 消息码
     */
    String msgCode,

    /**
     * 消息
     */
    String msgInfo,

    /**
     * 消息类型
     */
    String msgType,

    /**
     * 业务传输对象;
     */
    T body

) implements Serializable {
    public static ResponseRecord<EmptyBody> newSuccessInstance() {
        return new ResponseRecord<>(
                SysConstant.SUCCESS_CODE,
                null,
                ResponseMsgType.SUCCESS.getType(),
                null
        );
    }

    public static <T> ResponseRecord<T> newSuccessInstance(T t) {
        return new ResponseRecord<>(
                SysConstant.SUCCESS_CODE,
                null,
                ResponseMsgType.SUCCESS.getType(),
                t
        );
    }

    public static ResponseRecord<EmptyBody> newFailInstance() {
        return new ResponseRecord<>(
                SysConstant.FAIL_CODE,
                null,
                ResponseMsgType.SYSTEM_ERR.getType(),
                null
        );
    }

    @Override
    public String toString() {
        return "GeneralResponseDTO{" +
                "msgCode='" + msgCode + '\'' +
                ", msgInfo='" + msgInfo + '\'' +
                ", msgType='" + msgType + '\'' +
                ", body=" + body +
                '}';
    }
}