package com.blink.framework.redis.id;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 注意区分 reactive响应式 和 sync同步版 的生成方法
 * 注意：lua脚本规则是 所有的生成的序列号 超过设置的最大值后 重新从0开始
 * 超量设计 上限远大于实际可能到达的值
 * 所以使用时请结合业务谨慎 考虑会不会重复 谨慎使用手动设置maxValue的api
 *
 * @author binblink
 */
public class IdGenerator {
    /**
     * 同步生成器
     */
    @Autowired
    private SeqGenerator seqGenerator;


    public Long generateId(String key) {
        return seqGenerator.generateSeq(key, Long.valueOf(IdGeneratorConstant.DEFAULT_KEY_MAX_VALUE));
    }

    public Long generateId(String key, Long maxValue) {
        return seqGenerator.generateSeq(key, maxValue);
    }


    public String generateId(String key, int length) {
        return IdStrUtils.stringFillAuto(generateId(key, IdStrUtils.getMaxValue(IdStrUtils.getMaxIdGenLength(length))).toString(), IdGeneratorConstant.DEFAULT_SEQUENCE_PAD_CHAR, length);
    }

    public String generateIdWithDate(String key, String prefix, int length) {

        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generateId(key, length);
    }

    public String generateIdWithDate(String key, int length) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generateId(key, length);
    }


    public String generateIdWithDateTime(String key, String prefix, int length) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generateId(key, length);
    }

    public String generateIdWithDateTime(String key, int length) {
        return IdStrUtils.getDateTimeString() + generateId(key, length);
    }


    public String generateCommonId(String key) {
        return generateIdWithDate(key, IdGeneratorConstant.DEFAULT_LENGTH);
    }


    public String generateMsgId() {
        return generateIdWithDateTime(IdGeneratorConstant.MSG_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

    public String generateRequestId() {
        return generateIdWithDateTime(IdGeneratorConstant.REQ_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

    public String generateMqMsgId() {
        return generateIdWithDateTime(IdGeneratorConstant.MQ_MSG_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }

    public String generateTraceId() {
        return generateIdWithDateTime(IdGeneratorConstant.TRANCE_ID_KEY, IdGeneratorConstant.DEFAULT_LENGTH);
    }


}
