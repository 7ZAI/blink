package com.blink.log.util;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.log.sensitive.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志敏感数据脱敏工具类
 * <p>
 * 对日志中的敏感信息进行脱敏处理，
 * 支持密码、手机号、邮箱、身份证等常见敏感字段。
 * <p>
 * 脱敏规则统一使用 {@link SensitiveType} 枚举定义，
 * 与 {@link SensitiveUtils} 保持一致。
 *
 * @author binblink
 * @see SensitiveUtils
 * @see SensitiveType
 */
@Slf4j
public final class LogSensitiveUtils {

    private static final String MASK = "******";

    private LogSensitiveUtils() {
    }

    /**
     * 对对象进行脱敏处理并返回 JSON 字符串
     * <p>
     * 会自动识别并脱敏以下敏感字段：
     * <ul>
     *   <li>password/pwd/secret - 替换为 ******</li>
     *   <li>phone/mobile - 手机号脱敏（保留前3后4）</li>
     *   <li>email - 邮箱脱敏（保留前3字符）</li>
     *   <li>idcard/id_card - 身份证脱敏（保留前6后4）</li>
     * </ul>
     *
     * @param obj 待脱敏对象
     * @return 脱敏后的 JSON 字符串
     */
    public static String toSensitiveString(Object obj) {
        if (obj == null) {
            return "null";
        }

        // 对于简单类型，直接序列化（无需脱敏）
        if (isSimpleType(obj.getClass())) {
            try {
                return JacksonUtil.toJson(obj);
            } catch (Exception e) {
                log.debug("简单类型序列化失败: {}", e.getMessage());
                return "null";
            }
        }

        try {
            // 先转换为 Map，再进行脱敏处理
            Map<String, Object> map = JacksonUtil.toMap(obj);
            if (map != null && !map.isEmpty()) {
                // 脱敏敏感字段
                maskMapValues(map);
                return JacksonUtil.toJson(map);
            }
            // 如果无法转换为 Map，直接序列化
            return JacksonUtil.toJson(obj);
        } catch (Exception e) {
            log.debug("JSON序列化失败: {}", e.getMessage());
            // 降级：尝试直接序列化
            try {
                return JacksonUtil.toJson(obj);
            } catch (Exception ex) {
                return "null";
            }
        }
    }

    /**
     * 判断是否是简单类型（无需脱敏处理）
     */
    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz.isEnum() ||
                clazz == Character.class;
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
     * <p>
     * 根据字段名识别敏感字段并调用对应的脱敏方法
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
                    // 统一使用 SensitiveUtils 的脱敏方法
                    entry.setValue(SensitiveUtils.maskPhone(str));
                } else if (key.contains("email")) {
                    // 统一使用 SensitiveUtils 的脱敏方法
                    entry.setValue(SensitiveUtils.maskEmail(str));
                } else if (key.contains("idcard") || key.contains("id_card")) {
                    // 统一使用 SensitiveUtils 的脱敏方法（前6后4）
                    entry.setValue(SensitiveUtils.maskIdCard(str));
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
     * 保留前3后4，中间用*替代。
     * 示例：138****8888
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     * @see SensitiveUtils#maskPhone(String)
     */
    public static String maskPhone(String phone) {
        return SensitiveUtils.maskPhone(phone);
    }

    /**
     * 对邮箱脱敏
     * <p>
     * 前缀长度大于3时保留前3字符，否则保留前1字符。
     * 示例：abc****@qq.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     * @see SensitiveUtils#maskEmail(String)
     */
    public static String maskEmail(String email) {
        return SensitiveUtils.maskEmail(email);
    }

    /**
     * 对身份证号脱敏
     * <p>
     * 保留前6后4，中间用*替代。
     * 示例：110101********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     * @see SensitiveUtils#maskIdCard(String)
     */
    public static String maskIdCard(String idCard) {
        return SensitiveUtils.maskIdCard(idCard);
    }

    /**
     * 对字符串进行脱敏处理
     *
     * @param source     原始字符串
     * @param prefixKeep 保留前缀字符数，负数会被处理为0
     * @param suffixKeep 保留后缀字符数，负数会被处理为0
     * @return 脱敏后的字符串，如果前后缀保留长度之和大于等于字符串长度则返回原值
     * @see SensitiveUtils#mask(String, int, int)
     */
    public static String mask(String source, int prefixKeep, int suffixKeep) {
        return SensitiveUtils.mask(source, prefixKeep, suffixKeep);
    }

    /**
     * 对字符串进行脱敏处理
     *
     * @param source     原始字符串
     * @param prefixKeep 保留前缀字符数，负数会被处理为0
     * @param suffixKeep 保留后缀字符数，负数会被处理为0
     * @param maskChar   脱敏字符
     * @return 脱敏后的字符串，如果前后缀保留长度之和大于等于字符串长度则返回原值
     * @see SensitiveUtils#mask(String, int, int, char)
     */
    public static String mask(String source, int prefixKeep, int suffixKeep, char maskChar) {
        return SensitiveUtils.mask(source, prefixKeep, suffixKeep, maskChar);
    }
}
