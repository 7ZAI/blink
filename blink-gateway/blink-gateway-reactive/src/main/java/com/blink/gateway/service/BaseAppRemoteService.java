package com.blink.gateway.service;

import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.SysConfigCacheDO;
import reactor.core.publisher.Mono;

/**
 * BaseApp远程服务接口
 * 支持多种实现方式：WebClient(HTTP)、Dubbo(RPC)
 *
 * @Author binblink
 */
public interface BaseAppRemoteService {

    /**
     * 根据配置key值获取单个配置参数信息
     *
     * @param configKey 配置key
     * @return Mono<SysConfigCacheDO>
     */
    Mono<SysConfigCacheDO> getOneConfig(String configKey);



    /**
     * 获取错误提示信息
     *
     * @param code  错误码
     * @param local 语言
     * @return Mono<QueryErrMsgRsp>
     */
    Mono<QueryErrMsgRsp> getErrorMsgInfo(String code, String local);

    /**
     * 获取用户权限标识
     *
     * @param userId 用户id
     * @return Mono<QueryUserPermissionRsp>
     */
    Mono<QueryUserPermissionRsp> getUserPermissionsByUerId(Integer userId);

    /**
     * 获取请求路径对应的权限标识
     *
     * @param requestPath 请求路径
     * @return Mono<QueryUserPermissionRsp>
     */
    Mono<QueryUserPermissionRsp> getUserPermissionsByPath(String requestPath);

    /**
     * 获取所有接口权限
     *
     * @return Mono<GetAllApiPermissionsRsp>
     */
    Mono<GetAllApiPermissionsRsp> getAllApiPermissions();
}
