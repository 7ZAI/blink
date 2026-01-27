package com.blink.gateway.constant;

/**
 * 错误码表
 * @author binblink
 */
public interface GateWayErrMsgCode {

    /**
     * 认证失败 token过期失效
     *
     */
    String UNAUTHORIZED = "AUTH0001";


    /**
     * 无访问权限
     *
     */
    String ACCESSDENIED = "AUTH0002";

    /**
     * 非法请求
     */
    String ILLEGAL_REQUEST = "ILLEGAL01";

    /**
     * 无法获取渠道信息
     */
    String FAILED_TO_GET_CHANNEL = "ILLEGAL02";

    /**
     * 渠道已关闭
     */
    String CHANNEL_CLOSED = "ILLEGAL03";


    /**
     * 禁止访问
     *
     */
    String FORBIDDEN = "HTTP403";


    /**
     * 请求过多
     *
     */
    String TOO_MANY_REQUESTS = "HTTP429";
}
