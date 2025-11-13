package com.blink.framework.common.data;

import com.blink.framework.common.constrant.ResponseMsgType;
import com.blink.framework.common.constrant.SysConstant;
import java.io.Serializable;

/**
 * ResponseMessageDTO 通用响应数据传输对象
 * @param <T>
 */
public class ResponseDTO<T>  implements Serializable {

    /**
     * 消息码
     */
    private String msgCode;

    /**
     * 消息
     */
    private String msgInfo;

    /**
     * 消息类型
     */
    private String msgType;

    /**
     * 业务传输对象;
     */
    private T body;


    public static ResponseDTO<EmptyBody> newSuccessInstance() {

        ResponseDTO<EmptyBody> rspDto = new ResponseDTO<>();
        rspDto.setMsgCode(SysConstant.SUCCESS_CODE);
        rspDto.setMsgType(ResponseMsgType.SUCCESS.getType());
        return rspDto;
    }

    public static <T> ResponseDTO<T> newSuccessInstance(T t) {

        ResponseDTO<T> rspDto = new ResponseDTO<>();
        rspDto.setMsgCode(SysConstant.SUCCESS_CODE);
        rspDto.setMsgType(ResponseMsgType.SUCCESS.getType());
        rspDto.setBody(t);

        return rspDto;
    }

    public ResponseDTO(){

    }

    public static ResponseDTO<EmptyBody> newFailInstance() {
        ResponseDTO<EmptyBody> rspDto = new ResponseDTO<>();
        rspDto.setMsgCode(SysConstant.FAIL_CODE);
        rspDto.setMsgType(ResponseMsgType.SYSTEM_ERR.getType());
        return rspDto;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgInfo() {
        return msgInfo;
    }

    public void setMsgInfo(String msgInfo) {
        this.msgInfo = msgInfo;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "ResponseMessageDTO{" +
                "msgCode='" + msgCode + '\'' +
                ", msgInfo='" + msgInfo + '\'' +
                ", msgType='" + msgType + '\'' +
                ", body=" + body +
                '}';
    }
}
