package com.blink.gateway.util;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.utils.JacksonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
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
    public static <T, R, V extends ResponseDTO<?>> Mono<R> webClientPost(WebClient webClient, String url, RequestDTO<T> requestDTO, R r, ParameterizedTypeReference<V> v) {
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
     *
     * @param requestDTO 请求报文
     * @param v          接口返回值
     * @param url        请求url
     * @return Mono<V> ResponseDTO<?>
     */
    public static <T, V extends ResponseDTO<?>> Mono<V> webClientPost(WebClient webClient, String url, RequestDTO<T> requestDTO, ParameterizedTypeReference<V> v) {

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
     * 直接发送json (测试加密解密使用)
     *
     * @param webClient
     * @param url
     * @param json
     * @return 获取响应json
     */
    public static Mono<ApiResponse> webClientPost(WebClient webClient, String url, String json) {

        log.info("=====>开始发送请求 url:{}, 请求体：{}", url, json);

        return webClient.post()
                .uri(url)
                .bodyValue(json)
                .exchangeToMono(clientResponse -> {
                    // 获取响应头
                    HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                    HttpStatus httpStatus = (HttpStatus) clientResponse.statusCode();
                    return clientResponse.bodyToMono(String.class)
                            .map(body -> new ApiResponse(httpStatus, headers, body));
                })
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


        Jackson2JsonDecoder jackson2JsonDecoder = new Jackson2JsonDecoder(JacksonUtil.getDefaultMapper());
        Jackson2JsonEncoder jackson2JsonEncoder = new Jackson2JsonEncoder(JacksonUtil.getDefaultMapper());

        return webClientBuilder
                // 服务名或直接URL
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder);
                    configurer.defaultCodecs().jackson2JsonEncoder(jackson2JsonEncoder);
                })
                .build();

    }

    public static class ApiResponse {

        private HttpStatus statusCode;
        private HttpHeaders headers;
        private String body;

        public ApiResponse(HttpStatus statusCode, HttpHeaders headers, String body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        public HttpStatus getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(HttpStatus statusCode) {
            this.statusCode = statusCode;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public void setHeaders(HttpHeaders headers) {
            this.headers = headers;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @Override
        public String toString() {
            return "ApiResponse{" +
                    "statusCode=" + statusCode +
                    ", headers=" + headers +
                    ", body='" + body + '\'' +
                    '}';
        }
    }


}
