package com.blink.gateway.constant;

/**
 * 远程调用url
 * @author binblink
 */
public interface RemoteServerUrl {

    /**
     * 系统config 请求url
     */
    String GET_GATEWAY_CONFIG_URL = "/sysConfig/getOneConfig";

    /**
     * 渠道信息 请求url
     */
    String GET_CHANNEL_URL = "/channel/getChannel";


    /**
     * 渠道信息 请求url
     */
    String GET_ERR_MSG_URL = "/internal/error/msg/getMsg";
}
