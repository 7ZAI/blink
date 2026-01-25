package com.blink.framework.core.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.alibaba.fastjson2.support.spring6.webservlet.view.FastJsonJsonView;

import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import com.blink.framework.core.interceptor.BlinkRequestContextInterceptor;
import com.blink.framework.core.interceptor.LogMdcInterceptor;
import com.blink.framework.core.mapper.SysChannelMapper;
import com.blink.framework.redis.component.CacheComponent;
import jakarta.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 自动配置类
 *
 * @author binblink
 */

@EnableWebMvc
public class BlinkWebMvcConfigurer implements WebMvcConfigurer {

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private SysChannelMapper channelMapper;

    @Resource
    private BlinkWebAppConfigProperties properties;

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        FastJsonJsonView fastJsonJsonView = new FastJsonJsonView();
        //自定义配置...
        //FastJsonConfig config = new FastJsonConfig();
        //config.set...
        //fastJsonJsonView.setFastJsonConfig(config);
        registry.enableContentNegotiation(fastJsonJsonView);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        if(properties.getEnableContextHolder()){
            // 应用于所有路径
            registry.addInterceptor(new BlinkRequestContextInterceptor()).addPathPatterns("/**");
        }

        registry.addInterceptor(new LogMdcInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        //自定义配置...
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setReaderFeatures(JSONReader.Feature.FieldBased, JSONReader.Feature.SupportArrayToBean);
        config.setWriterFeatures(JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.PrettyFormat);
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
        converters.add(0, converter);
    }

//    /**
//     * 配置请求加密解密过滤器
//     *
//     * @param aesAadEnable 是否启用AES GCM模式中的 AAD参与加密解密 默认不开启
//     * @return cryptoFilter
//     */
//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    public CryptoFilter cryptoFilter(@Value("${blink.core.crypt.aesAadEnable:false}") Boolean aesAadEnable) {
//        //BlinkHybridEncrypted 不纳入容器管理 因为 仅仅是作为默认实现 当外部引用需要自定义加解密方式
//        //方便外部配置new CryptoFilter() 直接覆盖该配置
//        HttpServletCrypto httpServletCrypto = new BlinkHybridEncrypted(cacheComponent, channelMapper, aesAadEnable);
//        return new CryptoFilter(httpServletCrypto);
//    }

}