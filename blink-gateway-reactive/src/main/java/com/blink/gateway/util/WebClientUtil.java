package com.blink.gateway.util;

import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.codec.Fastjson2Decoder;
import com.alibaba.fastjson2.support.spring6.http.codec.Fastjson2Encoder;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @Author binblink
 */
@Slf4j
public class WebClientUtil {

    /**
     * webclient post 请求模板代码
     *
     * @param requestDTO 请求报文
     * @param r          实际返回值
     * @param v          接口返回值
     * @param url        请求url
     * @return Mono<R>
     */
    public static <T, R, V extends ResponseDTO> Mono<R> webClientPost(WebClient webClient, String url, RequestDTO<T> requestDTO, R r, ParameterizedTypeReference<V> v) {
        log.info("=====>开始发送请求 url:{}, 请求体：{}", url, requestDTO);
        return webClient.post()
                .uri(url)
                .bodyValue(requestDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("客户端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("客户端请求错误"));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("服务端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("base-app 服务异常"));
                })
                //带泛型的返回值 写法
                .bodyToMono(v)
                .map(respDTO -> {
                    BeanUtils.copyProperties(respDTO.getBody(), r);
                    return r;
                })
                .doOnSuccess(response -> log.info("获取参数成功: {}", response))
                .doOnError(error -> log.error("获取参数失败: {}", error.getMessage()));
    }

    /**
     * webclient post 请求模板代码
     * 返回值带有元数据
     * @param requestDTO 请求报文
     * @param v          接口返回值
     * @param url        请求url
     * @return Mono<V> ResponseDTO<?>
     */
    public static <T, V extends ResponseDTO> Mono<V> webClientPost(WebClient webClient, String url, RequestDTO<T> requestDTO, ParameterizedTypeReference<V> v) {
        log.info("=====>开始发送请求 url:{}, 请求体：{}", url, requestDTO);
        return webClient.post()
                .uri(url)
                .bodyValue(requestDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("客户端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("客户端请求错误"));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("服务端错误: {}", response.statusCode());
                    return Mono.error(new RuntimeException("base-app 服务异常"));
                })
                //带泛型的返回值 写法
                .bodyToMono(v)
                .doOnSuccess(response -> log.info("获取参数成功: {}", response))
                .doOnError(error -> log.error("获取参数失败: {}", error.getMessage()));
    }



    /**
     * 通过WebClient.Builder获取WebClient
     *
     * @param webClientBuilder
     * @param baseUrl
     * @return WebClient
     */
    public static WebClient getWebClient(WebClient.Builder webClientBuilder, String baseUrl) {
        // 1️ 配置 fastjson2 的行为
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        // 支持非 ISO 格式日期
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");

        // 2️ 传入一个空的 ObjectMapper（只是为了满足构造函数要求）
        ObjectMapper dummyMapper = new ObjectMapper();
        // 3️ 创建 fastjson2 的编解码器
        Fastjson2Decoder decoder = new Fastjson2Decoder(dummyMapper, fastJsonConfig, MediaType.APPLICATION_JSON);
        Fastjson2Encoder encoder = new Fastjson2Encoder(dummyMapper, fastJsonConfig, MediaType.APPLICATION_JSON);

        return webClientBuilder
                // 服务名或直接URL
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                }).build();

    }


}
