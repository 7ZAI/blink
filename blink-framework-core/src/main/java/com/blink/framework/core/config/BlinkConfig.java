package com.blink.framework.core.config;

import com.blink.framework.core.aop.RedisCacheUpdateAspect;
import com.blink.framework.core.component.RedisCachePreHeatRunner;
import com.blink.framework.core.mapper.SysDataDictMapper;
import com.blink.framework.core.mapper.SysMsgInfoMapper;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.redis.component.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author binblink
 * @Date 2025/11/11
 */
@Configuration
public class BlinkConfig {


//    @Bean
//    @ConditionalOnBean(RedisClient.class)
//    public RedisCacheUpdateAspect redisCacheUpdateAspect(RedisClient redisClient){
//        return new RedisCacheUpdateAspect(redisClient);
//    }
//
//    @Bean
//    @ConditionalOnBean(RedisClient.class)
//    public RedisCachePreHeatRunner redisCachePreHeatRunner(CacheComponent cacheComponent, SysMsgInfoMapper msgInfoMapper, SysDataDictMapper dataDictMapper){
//
//        return new RedisCachePreHeatRunner(cacheComponent,msgInfoMapper,dataDictMapper);
//    }


}
