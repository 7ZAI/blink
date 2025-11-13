package com.blink.framework.common.exception;

public interface ErrorCode {

    /**
     * 获取错误码
     * @return
     */
    String getCode();

    /**
     * 获取错误描述
     * @return
     */
    String getDesc();
}
