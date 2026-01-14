package com.blink.framework.redis.config;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.id.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

/**
 *  redis集群缓存配置
 *
 * @author binblink
 */
//@AutoConfiguration
@EnableConfigurationProperties({BlinkRedisProperties.class})
@Configuration
public class BlinkRedisAutoConfiguration {


    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class SyncBlockRedisConfig {
        /**
         * Object RedisTemplate
         */
        @Bean("blinkRedisTemplate")
        @ConditionalOnMissingBean(
                name = {"blinkRedisTemplate"}
        )
        public RedisTemplate<String, Object> blinkRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            GenericFastJsonRedisSerializer valueSerializer = new GenericFastJsonRedisSerializer(new String[]{"com.blink.", "org.springframework.security.core."});
            StringRedisSerializer keySerializer = new StringRedisSerializer();
            //Redis 的 Key 序列化
            redisTemplate.setKeySerializer(keySerializer);
            //Hash 中的 Field Key 序列化
            redisTemplate.setHashKeySerializer(keySerializer);

            //Redis 中的 Value 序列化
            redisTemplate.setValueSerializer(valueSerializer);
            //Hash 中的 Field Value 序列化
            redisTemplate.setHashValueSerializer(valueSerializer);

            return redisTemplate;
        }

        @Bean
        public RedisClient redisClient(@Qualifier("blinkRedisTemplate") RedisTemplate<String, Object> blinkRedisTemplate) {
            return new RedisClient(blinkRedisTemplate);
        }

        @Bean
        public SeqGenerator seqGenerator(RedisClient redisClient, BlinkRedisProperties blinkRedisProperties) {
            return new SeqGenerator(redisClient, blinkRedisProperties);
        }

        @Bean
        public CacheComponent cacheComponent(BlinkRedisProperties properties) {
            return new CacheComponent(properties.getEnableLocalCache());
        }

        @Bean
        public IdGenerator idGenerator() {
            return new IdGenerator();
        }
    }


    /*** * * * * * * * * * * * * * * * * * * *Reactive 版本 * * * * * * * * * * * * * * * * * * * * * * * * *   * */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveRedisConfig {

        @Bean("blinkReactiveRedisTemplate")
        @ConditionalOnMissingBean(
                name = {"blinkReactiveRedisTemplate"}
        )
        public ReactiveRedisTemplate<String, Object> blinkReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

            GenericFastJsonRedisSerializer valueSerializer = new GenericFastJsonRedisSerializer(new String[]{"com.blink.", "org.springframework.security.core."});
            StringRedisSerializer keySerializer = new StringRedisSerializer();
            // 更小更快可选：开启 JSONB（二进制 JSON）
            RedisSerializationContext<String, Object> context =
                    RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                            .value(valueSerializer)
                            .hashKey(keySerializer)
                            .hashValue(valueSerializer)
                            .build();

            return new ReactiveRedisTemplate<>(factory, context);

        }

        @Bean
        public ReactiveRedisClient reactiveRedisClient(@Qualifier("blinkReactiveRedisTemplate") ReactiveRedisTemplate<String, Object> blinkReactiveRedisTemplate) {
            return new ReactiveRedisClient(blinkReactiveRedisTemplate);
        }

        @Bean
        public ReactiveSeqGenerator sequenceGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties blinkRedisProperties) {
            return new ReactiveSeqGenerator(reactiveRedisClient, blinkRedisProperties);
        }

        @Bean
        public ReactiveIdGenerator reactiveIdGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties blinkRedisProperties) {
            return new ReactiveIdGenerator();
        }
    }

    @Bean
    @ConditionalOnProperty(name = "blink.redis.enableLocalCache",havingValue = "true")
    public Cache<String,Object> caffeineCache(){
        return  Caffeine.newBuilder()
                //最后一次写入1个小时候 失效
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .initialCapacity(200)
                .maximumSize(3000)
                .build();
    }



}
