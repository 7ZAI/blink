package com.blink.gateway.service;

import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.codec.Fastjson2Decoder;
import com.alibaba.fastjson2.support.spring6.http.codec.Fastjson2Encoder;
import com.blink.base.dto.req.QueryOneChannelReqDTO;
import com.blink.base.dto.req.QueryOneSysConfigReqDTO;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.framework.common.data.ChannelInfoRedisDO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.gateway.util.WebClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.GatewayConstant.*;

/**
 *  WebClient 调用内部服务
 * @Author binblink
 * @Date 2025/10/16
 */
@Service
public class BaseAppService {

    private final Logger logger = LoggerFactory.getLogger(BaseAppService.class);

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

        logger.info("调用远程服务获取 配置参数信息 key:{}}", configKey);

        if(configKey.contains(GATEWAY_CONFIG_KEY_PREFIX)){
            configKey = configKey.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
        }
        RequestDTO<QueryOneSysConfigReqDTO> requestDTO = new RequestDTO<>();
        QueryOneSysConfigReqDTO param = new QueryOneSysConfigReqDTO();
        param.setConfigKey(configKey);
        requestDTO.setBody(param);

        return webClientPost(GET_GATEWAY_CONFIG_URL,requestDTO,new SysConfigCacheDO(),new ParameterizedTypeReference<ResponseDTO<SysConfigVO>>(){}).cache();
    }


    /**
     * 根据appkey值获取单个渠道信息
     * @param appkey
     * @return
     */
    public Mono<ChannelInfoRedisDO> getChannelInfo(String appkey) {
        logger.info("调用远程服务获取 渠道信息 key:{}}", appkey);
        if(appkey.contains(BLINK_CHANNEL_PREFIX)){
            appkey = appkey.replace(BLINK_CHANNEL_PREFIX, "");
        }

        RequestDTO<QueryOneChannelReqDTO>  requestDTO = new RequestDTO<>();
        QueryOneChannelReqDTO param = new QueryOneChannelReqDTO();
        param.setAppKey(appkey);
        requestDTO.setBody(param);

        return webClientPost(GET_CHANNEL_URL,requestDTO,new ChannelInfoRedisDO(),new ParameterizedTypeReference<ResponseDTO<ChannelVO>>(){});

    }

    /**
     *  webclient post 请求模板代码
     * @param requestDTO 请求报文
     * @param r 实际返回值
     * @param v 接口返回值
     * @param url 请求url
     * @return Mono<R>
     */
    private <T,R,V extends ResponseDTO> Mono<R> webClientPost(String url,RequestDTO<T> requestDTO ,R r,ParameterizedTypeReference<V> v){

        return webClient.post()
                .uri(url)
                .bodyValue(requestDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    logger.error("客户端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("客户端请求错误"));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    logger.error("服务端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("base-app 服务异常"));
                })
                //带泛型的返回值 写法
                .bodyToMono(v)
                .map(respDTO -> {
                    BeanUtils.copyProperties(respDTO.getBody(), r);
                    return r;
                })
                .doOnSuccess(response -> logger.info("获取参数成功: {}", response))
                .doOnError(error -> logger.error("获取参数失败: {}", error.getMessage()));
    }

}
