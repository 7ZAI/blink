package com.blink.log.util;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.log.annotation.SensitiveField;
import com.blink.log.sensitive.SensitiveType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;


/**
 * 敏感数据脱敏工具类
 * 
 * @author binblink
 */
@Slf4j
public final class SensitiveUtils {

    private static final String MASK = "******";

    private SensitiveUtils() {
    }

    /**
     * 对字符串进行脱敏处理
     *
     * @param source 原始字符串
     * @param type   脱敏类型
     * @return 脱敏后的字符串
     */
    public static String mask(String source, SensitiveType type) {
        if (StrUtil.isBlank(source)) {
            return source;
        }

        if (type == SensitiveType.PASSWORD) {
            return MASK;
        }

        return mask(source, type.getPrefixKeep(), type.getSuffixKeep());
    }

    /**
     * 对字符串进行脱敏处理（自定义保留位数）
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

    /**
     * 对手机号脱敏
     * 138****8888
     */
    public static String maskPhone(String phone) {
        return mask(phone, SensitiveType.PHONE);
    }

    /**
     * 对身份证号脱敏
     * 110***********1234
     */
    public static String maskIdCard(String idCard) {
        return mask(idCard, SensitiveType.ID_CARD);
    }

    /**
     * 对银行卡号脱敏
     * 6222****1234
     */
    public static String maskBankCard(String bankCard) {
        return mask(bankCard, SensitiveType.BANK_CARD);
    }

    /**
     * 对邮箱脱敏
     * abc****@qq.com
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
     * 对姓名脱敏
     * 张*、张**
     */
    public static String maskName(String name) {
        return mask(name, SensitiveType.NAME);
    }

    /**
     * 对地址脱敏
     * 北京市海淀****
     */
    public static String maskAddress(String address) {
        return mask(address, SensitiveType.ADDRESS);
    }

    /**
     * 对对象进行脱敏处理并返回有效的 JSON 字符串
     * 使用 Jackson 直接序列化，让 Jackson 处理对象转换
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
            // Jackson 会自动处理字段访问和敏感注解（如果配置了）
            String json = JacksonUtil.toJson(obj);
            return json;
        } catch (Exception e) {
            log.warn("JSON序列化失败，尝试简化处理: {}", e.getMessage());
            // 如果序列化失败，尝试转换为 Map 再序列化
            try {
                Map<String, Object> map = JacksonUtil.toMap(obj);
                if (map != null) {
                    // 脱敏敏感字段
                    maskMapValues(map);
                    return JacksonUtil.toJson(map);
                }
            } catch (Exception ex) {
                log.warn("Map转换也失败: {}", ex.getMessage());
            }
            // 最终 fallback 返回 null
            return "null";
        }
    }

    /**
     * 递归脱敏 Map 中的值
     */
    @SuppressWarnings("unchecked")
    private static void maskMapValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            // 如果是字符串，检查是否是敏感字段（简单检查字段名）
            if (value instanceof String str) {
                String key = entry.getKey().toLowerCase();
                if (key.contains("password") || key.contains("pwd") || key.contains("secret")) {
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
     * 深度脱敏对象
     */
    @SuppressWarnings("unchecked")
    private static Object deepMask(Object obj) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();

        if (isSimpleType(clazz)) {
            return obj;
        }

        if (obj instanceof String) {
            return obj;
        }

        if (obj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(SensitiveUtils::deepMask)
                    .toList();
        }

        if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k, deepMask(v)));
            return result;
        }

        if (clazz.isArray()) {
            Object[] array = (Object[]) obj;
            Object[] result = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = deepMask(array[i]);
            }
            return result;
        }

        return maskObjectFields(obj, clazz);
    }

    /**
     * 对对象字段进行脱敏
     */
    private static Object maskObjectFields(Object obj, Class<?> clazz) {
        try {
            Object copy = clazz.getDeclaredConstructor().newInstance();

            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();

                for (Field field : fields) {
                    // 跳过静态字段、final字段和serialVersionUID字段
                    if (shouldSkipField(field)) {
                        continue;
                    }

                    field.setAccessible(true);

                    Object value = field.get(obj);
                    Object maskedValue = processFieldValue(field, value);

                    field.set(copy, maskedValue);
                }

                currentClass = currentClass.getSuperclass();
            }

            return copy;
        } catch (Exception e) {
            log.debug("对象字段脱敏失败: {}", e.getMessage());
            return obj;
        }
    }

    /**
     * 判断是否应该跳过该字段
     * 跳过静态字段、final字段和serialVersionUID字段
     */
    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        
        // 跳过静态字段
        if (java.lang.reflect.Modifier.isStatic(modifiers)) {
            return true;
        }
        
        // 跳过final字段
        if (java.lang.reflect.Modifier.isFinal(modifiers)) {
            return true;
        }
        
        // 跳过serialVersionUID字段
        if ("serialVersionUID".equals(field.getName())) {
            return true;
        }
        
        return false;
    }

    /**
     * 处理字段值
     */
    private static Object processFieldValue(Field field, Object value) {
        if (value == null) {
            return null;
        }

        SensitiveField annotation =
                field.getAnnotation(SensitiveField.class);

        if (annotation != null && value instanceof String str) {
            return maskStringField(str, annotation);
        }

        return deepMask(value);
    }

    /**
     * 对字符串字段进行脱敏
     */
    private static String maskStringField(String value,
            SensitiveField annotation) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        SensitiveType type = annotation.type();

        if (type == SensitiveType.CUSTOM) {
            return mask(value, annotation.prefixKeep(), annotation.suffixKeep(), annotation.maskChar());
        }

        if (type == SensitiveType.EMAIL) {
            return maskEmail(value);
        }

        return mask(value, type);
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz.isEnum() ||
                clazz == Character.class;
    }
}
