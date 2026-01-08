package com.blink.framework.redis.serializer;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * lua 脚本返回的Long 类型序列化值和 fastjson2序列化的值存在差异
 * 所以需要自定义一个Long类型 序列化器接收执行lua 脚本的结果
 */
public class LongRedisSerializer implements RedisSerializer<Long> {

    private final Charset charset;

    public LongRedisSerializer() {
        this(StandardCharsets.UTF_8);
    }

    public LongRedisSerializer(Charset charset) {
        Assert.notNull(charset, "Charset must not be null!");
        this.charset = charset;
    }

    @Override
    public byte[] serialize(Long t) throws SerializationException {
        return String.valueOf(t).getBytes(charset);
    }

    @Override
    public Long deserialize(byte[] bytes) throws SerializationException {
        return Long.valueOf(new String(bytes, charset));
    }


    /**
     * 执行结果 76 在 ASCii 码表中代表 L 字符
     *
     * 51,52,50,51,52,50,51,76
     * ----------------------------------------------
     * 51,52,50,51,52,50,51
     * @param args
    //     */
//    public static void main(String[] args) {
//        Long m = 3423423L;
//
//        byte[] bytes = new GenericFastJsonRedisSerializer().serialize(m);
//        byte[] byte1 = new StringRedisSerializer().serialize(String.valueOf(m));
//        StringJoiner sj = new StringJoiner(",");
//        for(byte b : bytes){
//            sj.add(String.valueOf(b));
//        }
//        System.out.println(sj.toString());
//        System.out.println("----------------------------------------------");
//        StringJoiner sj2 = new StringJoiner(",");
//        for(byte b2 : byte1){
//            sj2.add(String.valueOf(b2));
//        }
//        System.out.println(sj2.toString());
//    }
}
