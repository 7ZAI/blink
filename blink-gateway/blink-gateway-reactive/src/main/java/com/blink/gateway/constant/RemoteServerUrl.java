package com.blink.gateway.constant;

/**
 * 远程调用url
 *
 * @author binblink
 */
public interface RemoteServerUrl {

    /**
     * 系统config 请求url
     */
    String GET_GATEWAY_CONFIG_URL = "/sysConfig/getConfigFromCache";

    /**
     * 渠道信息 请求url
     */
    String GET_CHANNEL_URL = "/channel/getChannel";


    /**
     * 渠道信息 请求url
     */
    String GET_ERR_MSG_URL = "/internal/error/msg/getMsg";

    /**
     * 用户权限信息 请求url
     */
    String GET_USER_PERMISSION_URL = "/sysPermission/internal/getPermissions";

    /**
     * 获取所有接口权限标识
     */
    String GET_ALL_API_PERMISSION ="/sysPermission/internal/getAllApiPermission";

}
