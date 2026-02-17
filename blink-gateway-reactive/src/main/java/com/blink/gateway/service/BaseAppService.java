package com.blink.gateway.service;


import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.constant.RemoteServerUrl;
import com.blink.gateway.util.WebClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.RedisConstans.*;


/**
 *  WebClient 调用内部服务
 * @Author binblink
 * @Date 2025/10/16
 */
@Service
@Slf4j
public class BaseAppService {


    private final WebClient webClient;

    private final String BASE_URL = "http://base-app/base";


    public BaseAppService(WebClient.Builder webClientBuilder) {
        this.webClient =  WebClientUtil.getWebClient(webClientBuilder,BASE_URL);
    }


    /**
     * 根据配置key值获取单个配置参数信息
     * @param configKey
     * @return
     */
    public Mono<SysConfigCacheDO> getOneConfig(String configKey) {

        log.info("调用远程服务获取 配置参数信息 key:{}", configKey);

        if(configKey.contains(GATEWAY_CONFIG_KEY_PREFIX)){
            configKey = configKey.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
        }
        RequestDTO<QueryOneSysConfigReq> requestDTO = new RequestDTO<>();
        var param = new QueryOneSysConfigReq();
        param.setConfigKey(configKey);
        requestDTO.setBody(param);

        return WebClientUtil.webClientPost(webClient,RemoteServerUrl.GET_GATEWAY_CONFIG_URL,requestDTO,new SysConfigCacheDO(),new ParameterizedTypeReference<ResponseDTO<SysConfigVO>>(){}).cache();
    }


    /**
     * 根据appkey值获取单个渠道信息
     * @param appkey
     * @return
     */
    public Mono<ChannelInfoRedisDO> getChannelInfo(String appkey) {
        log.info("调用远程服务获取 渠道信息 key:{}", appkey);
        if(appkey.contains(BLINK_CHANNEL_PREFIX)){
            appkey = appkey.replace(BLINK_CHANNEL_PREFIX, "");
        }

        RequestDTO<QueryOneChannelReq>  requestDTO = new RequestDTO<>();
        QueryOneChannelReq param = new QueryOneChannelReq();
        param.setAppKey(appkey);
        requestDTO.setBody(param);

        return WebClientUtil.webClientPost(webClient,RemoteServerUrl.GET_CHANNEL_URL,requestDTO,new ChannelInfoRedisDO(),new ParameterizedTypeReference<ResponseDTO<ChannelVO>>(){});

    }

    /**
     * 获取错误提示信息
     *
     * @param code 错误码
     * @param local 语言
     * @return Mono<QueryErrMsgRspDTO>
     */
    public Mono<QueryErrMsgRsp> getErrorMsgInfo(String code, String local) {

        log.info("调用远程服务获取 错误提示信息 code:{},language:{}", code,local);

        var  requestDTO = new RequestDTO<QueryErrMsgReq>();
        var param = new QueryErrMsgReq();
        param.setCode(code);
        param.setLocal(local);
        requestDTO.setBody(param);

        return WebClientUtil.webClientPost(webClient,RemoteServerUrl.GET_ERR_MSG_URL,requestDTO,new QueryErrMsgRsp(),new ParameterizedTypeReference<ResponseDTO<QueryErrMsgRsp>>(){});

    }


    /**
     * 获取用户权限标识
     *
     * @param userId 用户id
     * @return Mono<QueryErrMsgRspDTO>
     */
    public Mono<QueryUserPermissionRsp> getUserPermissions(Integer userId) {

        log.info("调用远程服务获取用户权限标识 userId:{}", userId);

        var  requestDTO = new RequestDTO<QueryUserPermissionReq>();
        var param = new QueryUserPermissionReq();
        param.setUserId(userId);
        requestDTO.setBody(param);

        return WebClientUtil.webClientPost(webClient,RemoteServerUrl.GET_USER_PERMISSION_URL,requestDTO,new QueryUserPermissionRsp(),new ParameterizedTypeReference<ResponseDTO<QueryUserPermissionRsp>>(){});

    }

    /**
     * 获取所有接口权限
     *
     * @return Mono<QueryErrMsgRspDTO>
     */
    public Mono<GetAllApiPermissionsRsp> getAllApiPermissions() {

        log.info("调用远程服务获取所有接口权限");
        var  requestDTO = new RequestDTO<GetAllApiPermissionsReq>();
        var param = new GetAllApiPermissionsReq();
        requestDTO.setBody(param);

        return WebClientUtil.webClientPost(webClient,RemoteServerUrl.GET_USER_PERMISSION_URL,requestDTO,new GetAllApiPermissionsRsp(),new ParameterizedTypeReference<ResponseDTO<GetAllApiPermissionsRsp>>(){});

    }



}
