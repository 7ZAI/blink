package com.blink.redis;

import com.blink.framework.common.data.DictCacheDO;
import com.blink.framework.redis.component.RedisClient;
import com.blink.framework.redis.entity.MessageType;
import com.blink.framework.redis.mq.RedisStreamProducer;
import com.blink.framework.redis.mq.StreamMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author binblink
 */
@SpringBootTest(classes = TestApplicationConfig.class)
@ActiveProfiles("test")
public class RedisTest {

    @Resource
    private RedisClient redisClient;

    @Test
    void testStream(){
        DictCacheDO dictCacheDO = new DictCacheDO();
        dictCacheDO.setDictName("das");
        dictCacheDO.setDataPattern("353453");
        StreamMessage<DictCacheDO> msg = StreamMessage.of("test", MessageType.EVENT, dictCacheDO);
        RedisStreamProducer streamProducer = new RedisStreamProducer(redisClient);

    }
}
