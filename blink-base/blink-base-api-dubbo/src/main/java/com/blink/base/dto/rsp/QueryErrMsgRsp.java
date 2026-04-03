package com.blink.base.dto.rsp;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取错误消息 出参
 *
 * @Author binblink
 */
@Data
public class QueryErrMsgRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误消息ID
     */
    private Integer msgId;
    /**
     * 错误码
     */
    private String msgCode;
    /**
     * 错误码
     */
    private String msgInfo;
    /**
     * 错误类型
     */
    private String msgType;
    /**
     * 错误码
     */
    private String msgLang;
}
