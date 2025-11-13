package com.blink.framework.redis.config;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.id.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author binblink
 * * redis集群缓存配置
 */
@Configuration
@EnableConfigurationProperties({BlinkRedisProperties.class})
public class BlinkRedisConfig {


    @Configuration
    @ConditionalOnProperty(name = "blink.redis.mode", havingValue = "sync")
    public static class SyncBlockRedisConfig {
        /**
         * Object RedisTemplate
         */
        @Bean
        @ConditionalOnMissingBean
        public RedisTemplate<String, Object> lettuceRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            GenericFastJsonRedisSerializer valueSerializer = new GenericFastJsonRedisSerializer(new String[]{"com.blink.", "org.springframework.security.core."});
            StringRedisSerializer keySerializer = new StringRedisSerializer();
//        redisTemplate.setDefaultSerializer(valueSerializer);
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
        public RedisClient redisClient(RedisTemplate<String, Object> lettuceRedisTemplate) {
            return new RedisClient(lettuceRedisTemplate);
        }

        @Bean
        public SeqGenerator idGenerator(RedisClient redisClient, BlinkRedisProperties blinkRedisProperties) {
            return new SeqGenerator(redisClient, blinkRedisProperties);
        }

        @Bean
        public CacheComponent cacheComponent(BlinkRedisProperties properties) {
            return new CacheComponent(properties.getEnableLocalCache());
        }

        @Bean
        public IdGenerator syncIdGenerator() {
            return new IdGenerator();
        }
    }


    /*** * * * * * * * * * * * * * * * * * * *Reactive 版本 * * * * * * * * * * * * * * * * * * * * * * * * *   * */
    @Configuration
    @ConditionalOnProperty(name = "blink.redis.mode", havingValue = "reactive")
    public static class ReactiveRedisConfig {

        @Bean
        @ConditionalOnMissingBean
        public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

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

            // key 序列化用 String
//        StringRedisSerializer keySerializer = new StringRedisSerializer();
//
//        // value 序列化用 Jackson
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
//
//        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);
//
//        RedisSerializationContext<String, Object> context = RedisSerializationContext
//                .<String, Object>newSerializationContext(keySerializer)
//                .value(valueSerializer)
//                .hashKey(keySerializer)
//                .hashValue(valueSerializer)
//                .build();
//
//        return new ReactiveRedisTemplate<>(factory, context);
        }

        @Bean
        public ReactiveRedisClient reactiveRedisClient(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
            return new ReactiveRedisClient(reactiveRedisTemplate);
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



}
