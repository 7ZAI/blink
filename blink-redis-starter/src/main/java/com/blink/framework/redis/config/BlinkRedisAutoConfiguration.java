package com.blink.framework.redis.config;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.id.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
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
 *  redis集群缓存配置
 *
 * @author binblink
 */
@AutoConfiguration
@EnableConfigurationProperties({BlinkRedisProperties.class})
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

    private static ObjectMapper objectMapperForRedis(){
        ObjectMapper mapper = new ObjectMapper();
        // ========== 基础配置 ==========
        // 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 空对象不抛异常
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略null值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

//        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
//                // 允许所有以 Object 为基类的类型嵌入类型信息
//                .allowIfBaseType(Object.class)
//                .build();
//
//        // 参数1：类型验证器
//        // 参数2：类型嵌入策略（NON_FINAL：非 final 类都嵌入类型信息，常用）
//        // 参数3：类型信息的属性名（默认 "@class"，存入 Redis 时会携带该字段）
//        mapper.activateDefaultTypingAsProperty(
//                typeValidator,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                "@class"
//        );
        // ========== 时间配置 ==========
        // 禁用时间戳格式，使用ISO格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建Java 8时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 定义时间格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // 注册序列化和反序列化器
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        // 注册时间模块
        mapper.registerModule(javaTimeModule);

        // ========== 自定义模块 ==========
        SimpleModule customModule = new SimpleModule();
        // 处理Long类型，防止前端精度丢失（超过16位转为字符串）
        customModule.addSerializer(Long.class, ToStringSerializer.instance);
        customModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        customModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        mapper.registerModule(customModule);

        // ========== 其他配置 ==========
        // 设置日期格式（传统Date类型）
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 设置时区
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        return mapper;
    }



    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class SyncBlockRedisConfig {
        /**
         * Object RedisTemplate
         */
        @Bean("blinkRedisTemplate")
        @ConditionalOnMissingBean( name = {"blinkRedisTemplate"} )
        public RedisTemplate<String, Object> blinkRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            // 序列化 value
            GenericJackson2JsonRedisSerializer jsonSerializer =  new GenericJackson2JsonRedisSerializer(getObjectMapper());
            StringRedisSerializer keySerializer = new StringRedisSerializer();
            //Redis 的 Key 序列化
            redisTemplate.setKeySerializer(keySerializer);
            //Hash 中的 Field Key 序列化
            redisTemplate.setHashKeySerializer(keySerializer);

            //Redis 中的 Value 序列化
            redisTemplate.setValueSerializer(jsonSerializer);
            //Hash 中的 Field Value 序列化
            redisTemplate.setHashValueSerializer(jsonSerializer);
            //默认序列化
            redisTemplate.setDefaultSerializer(jsonSerializer);


            redisTemplate.afterPropertiesSet();

            return redisTemplate;
        }

        /**
         * 专门给 stream操作用的
         * @param factory
         * @return
         */
        @Bean("streamRedisTemplate")
        @ConditionalOnMissingBean( name = {"streamRedisTemplate"})
        public RedisTemplate<String, Object> streamRedisTemplate(RedisConnectionFactory factory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);

            //这里一定要设为 String，不要用 Json 或 GenericJackson
            template.setHashValueSerializer(stringSerializer);
            template.setValueSerializer(stringSerializer);

            template.afterPropertiesSet();
            return template;
        }

        @Bean
        public RedisClient redisClient(@Qualifier("blinkRedisTemplate") RedisTemplate<String, Object> blinkRedisTemplate,@Qualifier("streamRedisTemplate") RedisTemplate<String, Object> streamRedisTemplate) {
            return new RedisClient(blinkRedisTemplate,streamRedisTemplate);
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
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveRedisConfig {

        @Bean("blinkReactiveRedisTemplate")
        @ConditionalOnMissingBean( name = {"blinkReactiveRedisTemplate"} )
        public ReactiveRedisTemplate<String, Object> blinkReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

            StringRedisSerializer keySerializer = new StringRedisSerializer();
            // 序列化 value
            GenericJackson2JsonRedisSerializer jsonSerializer =  new GenericJackson2JsonRedisSerializer(getObjectMapper());

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

        /**
         * 专门给 stream操作用的
         * @param factory
         * @return
         */
        @Bean("streamReactiveRedisTemplate")
        @ConditionalOnMissingBean(
                name = {"streamReactiveRedisTemplate"}
        )
        public ReactiveRedisTemplate<String, Object> streamReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            // 序列化 value
            GenericJackson2JsonRedisSerializer jsonSerializer =  new GenericJackson2JsonRedisSerializer(getObjectMapper());

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
            return new ReactiveRedisClient(blinkReactiveRedisTemplate,streamReactiveRedisTemplate);
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
