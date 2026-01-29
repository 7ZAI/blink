package com.blink.framework.openfeign.config;


import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;


@Configuration
@EnableFeignClients(basePackages = "com.blink.**.client")
public class BlinkFeignConfig {

    // 注入Spring MVC的HttpMessageConverters工厂
//    @Bean
//    public Encoder feignEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
//        return new SpringEncoder(messageConverters);
//    }
//
//    @Bean
//    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
//        return new SpringDecoder(messageConverters);
//    }

    /**
     * 为openfeign 配置请求参数转换器
     * @param converters
     * @return
     */
//    @Bean
//    @Order(99)
//    @ConditionalOnMissingBean(HttpMessageConverter.class)
//    public HttpMessageConverters httpMessageConverters(List<HttpMessageConverter<?>> converters){
//
//        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
//        //自定义配置...
//        FastJsonConfig config = new FastJsonConfig();
//        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
//        config.setReaderFeatures(JSONReader.Feature.FieldBased, JSONReader.Feature.SupportArrayToBean);
//        config.setWriterFeatures(JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.PrettyFormat);
//        converter.setFastJsonConfig(config);
//        converter.setDefaultCharset(StandardCharsets.UTF_8);
//        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
//        converters.add(converter);
//        return new HttpMessageConverters(converters);
//    }





}
