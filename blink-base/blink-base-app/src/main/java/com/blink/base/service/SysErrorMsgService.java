package com.blink.base.service;


import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 错误消息服务类
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
public interface SysErrorMsgService {

    /**
     * 根据错误码和语言查询单个错误码消息
     *
     * @param queryParam 查询参数，包含错误码和语言
     * @return QueryErrMsgRsp 错误消息响应
     * @throws BlinkException 当错误消息不存在时抛出业务异常
     */
    QueryErrMsgRsp getErrorMsg(QueryErrMsgReq queryParam) throws BlinkException;
}
