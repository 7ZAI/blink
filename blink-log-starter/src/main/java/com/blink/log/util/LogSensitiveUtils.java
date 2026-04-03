package com.blink.log.util;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志敏感数据脱敏工具类
 * <p>
 * 对日志中的敏感信息进行脱敏处理，
 * 支持密码、手机号、邮箱、身份证等常见敏感字段。
 *
 * @author binblink
 */
@Slf4j
public final class LogSensitiveUtils {

    private static final String MASK = "******";

    private LogSensitiveUtils() {
    }

    /**
     * 对对象进行脱敏处理并返回 JSON 字符串
     *
     * @param obj 待脱敏对象
     * @return 脱敏后的 JSON 字符串
     */
    public static String toSensitiveString(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            // 直接使用 Jackson 序列化为 JSON
            String json = JacksonUtil.toJson(obj);
            return json;
        } catch (Exception e) {
            log.debug("JSON序列化失败: {}", e.getMessage());
            try {
                // 如果序列化失败，尝试转换为 Map 再序列化
                Map<String, Object> map = JacksonUtil.toMap(obj);
                if (map != null) {
                    // 脱敏敏感字段
                    maskMapValues(map);
                    return JacksonUtil.toJson(map);
                }
            } catch (Exception ex) {
                log.debug("Map转换失败: {}", ex.getMessage());
            }
            return "null";
        }
    }

    /**
     * 对对象进行脱敏处理并返回 JSON 字符串，支持安全截断
     * <p>
     * 当 JSON 字符串超过最大长度时，不会直接截断（可能导致 JSON 格式不正确），
     * 而是返回一个包含截断提示的 JSON 对象。
     *
     * @param obj       待脱敏对象
     * @param maxLength 最大长度，小于等于 0 表示不限制
     * @return 脱敏后的 JSON 字符串
     */
    public static String toSensitiveString(Object obj, int maxLength) {
        String json = toSensitiveString(obj);

        // 不限制或长度未超过，直接返回
        if (maxLength <= 0 || json.length() <= maxLength) {
            return json;
        }

        // JSON 过长，返回安全的截断提示对象
        Map<String, Object> truncatedInfo = new HashMap<>();
        truncatedInfo.put("_truncated", true);
        truncatedInfo.put("_originalLength", json.length());
        truncatedInfo.put("_maxLength", maxLength);
        truncatedInfo.put("_preview", StrUtil.sub(json, 0, Math.min(200, maxLength - 100)));

        try {
            return JacksonUtil.toJson(truncatedInfo);
        } catch (Exception e) {
            log.debug("生成截断提示JSON失败: {}", e.getMessage());
            return "{\"_truncated\":true,\"_originalLength\":" + json.length() + "}";
        }
    }

    /**
     * 递归脱敏 Map 中的值
     *
     * @param map 待脱敏的Map
     */
    @SuppressWarnings("unchecked")
    private static void maskMapValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            // 如果是字符串，检查是否是敏感字段
            if (value instanceof String str) {
                String key = entry.getKey().toLowerCase();
                if (isPasswordField(key)) {
                    entry.setValue(MASK);
                } else if (key.contains("phone") || key.contains("mobile")) {
                    entry.setValue(maskPhone(str));
                } else if (key.contains("email")) {
                    entry.setValue(maskEmail(str));
                } else if (key.contains("idcard") || key.contains("id_card")) {
                    entry.setValue(maskIdCard(str));
                }
            } else if (value instanceof Map) {
                maskMapValues((Map<String, Object>) value);
            }
        }
    }

    /**
     * 判断是否是密码字段
     *
     * @param key 字段名
     * @return true-是密码字段 false-不是
     */
    private static boolean isPasswordField(String key) {
        return key.contains("password") || key.contains("pwd") || key.contains("secret");
    }

    /**
     * 对手机号脱敏
     * <p>
     * 示例：138****8888
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 对邮箱脱敏
     * <p>
     * 示例：abc****@qq.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);

        if (prefix.length() <= 3) {
            return mask(prefix, 1, 0) + suffix;
        }

        return mask(prefix, 3, 0) + suffix;
    }

    /**
     * 对身份证号脱敏
     * <p>
     * 示例：110***********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 对字符串进行脱敏处理
     *
     * @param source     原始字符串
     * @param prefixKeep 保留前缀字符数
     * @param suffixKeep 保留后缀字符数
     * @return 脱敏后的字符串
     */
    public static String mask(String source, int prefixKeep, int suffixKeep) {
        return mask(source, prefixKeep, suffixKeep, '*');
    }

    /**
     * 对字符串进行脱敏处理
     *
     * @param source     原始字符串
     * @param prefixKeep 保留前缀字符数
     * @param suffixKeep 保留后缀字符数
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串
     */
    public static String mask(String source, int prefixKeep, int suffixKeep, char maskChar) {
        if (StrUtil.isBlank(source)) {
            return source;
        }

        int length = source.length();

        if (prefixKeep + suffixKeep >= length) {
            return source;
        }

        if (prefixKeep < 0) {
            prefixKeep = 0;
        }
        if (suffixKeep < 0) {
            suffixKeep = 0;
        }

        StringBuilder sb = new StringBuilder();

        if (prefixKeep > 0) {
            sb.append(source, 0, prefixKeep);
        }

        int maskLength = length - prefixKeep - suffixKeep;
        sb.append(String.valueOf(maskChar).repeat(maskLength));

        if (suffixKeep > 0) {
            sb.append(source, length - suffixKeep, length);
        }

        return sb.toString();
    }
}