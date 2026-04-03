package com.blink.framework.core.exception;

/**
 * 错误信息提供者接口
 * 用于获取错误码对应的多语言错误信息
 *
 * @author binblink
 */
public interface ErrMsgProvider {

    /**
     * 根据错误码和语言获取错误信息
     *
     * @param msgCode 错误码
     * @param lang    语言代码（如 zh_cn, en_us）
     * @return 错误信息，如果不存在返回默认系统错误消息
     */
    String getErrMsg(String msgCode, String lang);
}