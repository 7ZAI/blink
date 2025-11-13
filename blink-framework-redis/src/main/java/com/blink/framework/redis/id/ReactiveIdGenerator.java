package com.blink.framework.redis.id;

import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

/**
 * @Author binblink
 * @Date 2025/8/21
 */
public class ReactiveIdGenerator {

    /**
     * 响应式生成器
     */
    @Autowired
    private  ReactiveSeqGenerator reactiveSeqGenerator;

    public  Mono<Long> generateId(String key) {
        return reactiveSeqGenerator.nextId(key, IdGeneratorConstant.DEFAULT_KEY_MAX_VALUE);
    }

    public  Mono<Long> generateId(String key, Long maxValue) {
        return reactiveSeqGenerator.nextId(key, String.valueOf(maxValue));
    }

    public  Mono<String> generateId(String key, String prefix) {
        return generateId(key).map(seq -> prefix + seq);
    }

    public  Mono<String> generateId(String key, int length) {
        return generateId(key, IdStrUtils.getMaxValue(length)).map(seq -> IdStrUtils.stringFillAuto(String.valueOf(seq), IdGeneratorConstant.DEFAULT_SEQUENCE_PAD_CHAR, length));
    }


    public  Mono<String> generateId(String key, String prefix, int length) {

        return generateId(key, IdStrUtils.getMaxValue(IdStrUtils.getMaxIdGenLength(length))).map(seq -> prefix + IdStrUtils.stringFillAuto(String.valueOf(seq), IdGeneratorConstant.DEFAULT_SEQUENCE_PAD_CHAR, length));

    }

    public  Mono<String> generateIdWithDateTime(String key, int length) {
        return generateId(key, length).map(seq -> IdStrUtils.getDateTimeString() + seq);
    }

    public  Mono<String> generateMsgId() {
        return generateIdWithDateTime(IdGeneratorConstant.MSG_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }


    public  Mono<String> generateRequestId() {
        return generateIdWithDateTime(IdGeneratorConstant.REQ_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

    public  Mono<String> generateMqMsgId() {
        return generateIdWithDateTime(IdGeneratorConstant.MQ_MSG_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

    public  Mono<String> generateTraceId() {
        return generateIdWithDateTime(IdGeneratorConstant.TRANCE_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

}
