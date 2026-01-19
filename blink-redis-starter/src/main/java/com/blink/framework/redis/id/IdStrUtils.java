package com.blink.framework.redis.id;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author binblink
 */
public class IdStrUtils {


    public static String stringFillAuto(String toString, char paddingChar, int length) {
        int len = length - toString.length();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                toString = paddingChar + toString;
            }
        }
        return toString;
    }

    public static Long getMaxValue(int length) {
        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sbf.append("9");
        }
        return Long.valueOf(sbf.toString());
    }

    public static String getDateTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    public static int getMaxIdGenLength(int length) {
        int idGenLength = length;
        if (length > IdGeneratorConstant.DEFAULT_MAX_LENGTH || length <= 0) {
            idGenLength = IdGeneratorConstant.DEFAULT_MAX_LENGTH;
        }
        return idGenLength;
    }
}
