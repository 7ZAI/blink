package com.blink.gateway.util;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @Author binblink
 * @Date 2024/8/16
 */
public class WebClientUtil {

    public static <T> T  getServiceClient(String baseUrl, Class<T> clazz){

        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        WebClientAdapter adapter = WebClientAdapter.forClient(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(adapter).build();

        return  factory.createClient(clazz);
    }



}
