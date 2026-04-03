package com.blink.framework.redis.config;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.aop.RedisCacheUpdateAspect;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.config.prop.BlinkRedisProperties;
import com.blink.framework.redis.config.prop.DistributedLockProperties;
import com.blink.framework.redis.id.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Redis auto configuration for Blink Redis Starter.
 * <p>
 * This configuration provides:
 * <ul>
 *   <li>RedisTemplate configuration with custom serializers</li>
 *   <li>RedisClient for synchronous operations</li>
 *   <li>ReactiveRedisClient for reactive operations</li>
 *   <li>Sequence generator for distributed ID generation</li>
 *   <li>Distributed lock support (optional, requires Redisson)</li>
 * </ul>
 * </p>
 *
 * @author binblink
 */
@AutoConfiguration
@EnableConfigurationProperties({BlinkRedisProperties.class, DistributedLockProperties.class})
@Slf4j
public class BlinkRedisAutoConfiguration {

    private static volatile ObjectMapper objectMapperForRedis;

    public static ObjectMapper getObjectMapper() {
        if (objectMapperForRedis == null) {
            synchronized (JacksonUtil.class) {
                if (objectMapperForRedis == null) {
                    objectMapperForRedis = objectMapperForRedis();
                }
            }
        }
        return objectMapperForRedis;
    }

    private static ObjectMapper objectMapperForRedis() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaTimeModule javaTimeModule = new JavaTimeModule();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        mapper.registerModule(javaTimeModule);

        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(Long.class, ToStringSerializer.instance);
        customModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        customModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        mapper.registerModule(customModule);

        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        return mapper;
    }



    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class SyncBlockRedisConfig {

        /**
         * 注解@CacheDoubleDelete 延迟双删 redis缓存切面
         *
         * @return
         */
        @Bean
        @ConditionalOnMissingBean
        public RedisCacheUpdateAspect redisCacheUpdateAspect() {
            return new RedisCacheUpdateAspect();
        }

        @Bean("blinkRedisTemplate")
        @ConditionalOnMissingBean(name = {"blinkRedisTemplate"})
        public RedisTemplate<String, Object> blinkRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(getObjectMapper());
            StringRedisSerializer keySerializer = new StringRedisSerializer();
            redisTemplate.setKeySerializer(keySerializer);
            redisTemplate.setHashKeySerializer(keySerializer);
            redisTemplate.setValueSerializer(jsonSerializer);
            redisTemplate.setHashValueSerializer(jsonSerializer);
            redisTemplate.setDefaultSerializer(jsonSerializer);

            redisTemplate.afterPropertiesSet();

            return redisTemplate;
        }

        @Bean("streamRedisTemplate")
        @ConditionalOnMissingBean(name = {"streamRedisTemplate"})
        public RedisTemplate<String, Object> streamRedisTemplate(RedisConnectionFactory factory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(stringSerializer);
            template.setValueSerializer(stringSerializer);

            template.afterPropertiesSet();
            return template;
        }

        @Bean
        public RedisClient redisClient(@Qualifier("blinkRedisTemplate") RedisTemplate<String, Object> blinkRedisTemplate,
                                       @Qualifier("streamRedisTemplate") RedisTemplate<String, Object> streamRedisTemplate) {
            return new RedisClient(blinkRedisTemplate, streamRedisTemplate);
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

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveRedisConfig {

        @Bean("blinkReactiveRedisTemplate")
        @ConditionalOnMissingBean(name = {"blinkReactiveRedisTemplate"})
        public ReactiveRedisTemplate<String, Object> blinkReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

            StringRedisSerializer keySerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(getObjectMapper());

            RedisSerializationContext<String, Object> context =
                    RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                            .value(jsonSerializer)
                            .hashValue(jsonSerializer)
                            .hashKey(keySerializer)
                            .string(keySerializer)
                            .key(keySerializer)
                            .build();

            return new ReactiveRedisTemplate<>(factory, context);

        }

        @Bean("streamReactiveRedisTemplate")
        @ConditionalOnMissingBean(name = {"streamReactiveRedisTemplate"})
        public ReactiveRedisTemplate<String, Object> streamReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(getObjectMapper());

            RedisSerializationContext<String, Object> context =
                    RedisSerializationContext.<String, Object>newSerializationContext(stringSerializer)
                            .value(jsonSerializer)
                            .hashValue(stringSerializer)
                            .hashKey(stringSerializer)
                            .string(stringSerializer)
                            .key(stringSerializer)
                            .build();

            return new ReactiveRedisTemplate<>(factory, context);
        }

        @Bean
        public ReactiveRedisClient reactiveRedisClient(@Qualifier("blinkReactiveRedisTemplate") ReactiveRedisTemplate<String, Object> blinkReactiveRedisTemplate,
                                                       @Qualifier("streamReactiveRedisTemplate") ReactiveRedisTemplate<String, Object> streamReactiveRedisTemplate) {
            return new ReactiveRedisClient(blinkReactiveRedisTemplate, streamReactiveRedisTemplate);
        }

        @Bean
        public ReactiveSeqGenerator sequenceGenerator(ReactiveRedisClient reactiveRedisClient, BlinkRedisProperties blinkRedisProperties) {
            return new ReactiveSeqGenerator(reactiveRedisClient, blinkRedisProperties);
        }

        @Bean
        public ReactiveIdGenerator reactiveIdGenerator(ReactiveSeqGenerator sequenceGenerator) {
            return new ReactiveIdGenerator(sequenceGenerator);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "blink.redis.enableLocalCache", havingValue = "true")
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .initialCapacity(200)
                .maximumSize(3000)
                .build();
    }

    // ==================== 分布式锁配置 ====================
    // 注意：分布式锁配置放在嵌套静态类中，避免外层类加载时触发 RedissonClient 类加载
    // 只有当 RedissonClient 在 classpath 中存在时，这个嵌套类才会被加载

    /**
     * 分布式锁自动配置。
     * <p>
     * 该配置类只有在以下条件全部满足时才会加载：
     * <ul>
     *   <li>RedissonClient 类存在于 classpath 中</li>
     *   <li>配置 blink.redis.distributed-lock.enabled=true</li>
     * </ul>
     * </p>
     */
    @ConditionalOnClass(name = "org.redisson.api.RedissonClient")
    @ConditionalOnProperty(prefix = "blink.redis.distributed-lock", name = "enabled", havingValue = "true")
    public static class DistributedLockAutoConfiguration {

        /**
         * 创建 DistributedLockClient Bean。
         *
         * @param redissonClient Redisson 客户端实例（由 Redisson Starter 自动配置）
         * @param properties     分布式锁配置属性
         * @return DistributedLockClient 实例
         */
        @Bean
        @ConditionalOnMissingBean
        public com.blink.framework.redis.lock.DistributedLockClient distributedLockClient(
                org.redisson.api.RedissonClient redissonClient,
                DistributedLockProperties properties) {
            log.info("[分布式锁] 初始化 DistributedLockClient | keyPrefix: {}", properties.getKeyPrefix());
            return new com.blink.framework.redis.lock.DistributedLockClient(redissonClient, properties);
        }

        /**
         * 创建 DistributedLockAspect Bean，支持 @DistributedLock 注解。
         *
         * @param lockClient 分布式锁客户端
         * @param properties 分布式锁配置属性
         * @return DistributedLockAspect 实例
         */
        @Bean
        @ConditionalOnMissingBean
        public com.blink.framework.redis.aop.DistributedLockAspect distributedLockAspect(
                com.blink.framework.redis.lock.DistributedLockClient lockClient,
                DistributedLockProperties properties) {
            log.info("[分布式锁] 初始化 DistributedLockAspect，支持 @DistributedLock 注解");
            return new com.blink.framework.redis.aop.DistributedLockAspect(lockClient, properties);
        }
    }
}
