package com.blink.framework.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jackson JSON工具类
 * 提供全面的JSON序列化/反序列化功能
 */
@Slf4j
public class JacksonUtil {
    
    // ================== 单例配置 ==================
    
    private static volatile ObjectMapper defaultMapper;
    
    private JacksonUtil() {
        // 工具类，防止实例化
    }
    
    /**
     * 获取默认的ObjectMapper（线程安全，单例）
     */
    public static ObjectMapper getDefaultMapper() {
        if (defaultMapper == null) {
            synchronized (JacksonUtil.class) {
                if (defaultMapper == null) {
                    defaultMapper = createDefaultObjectMapper();
                }
            }
        }
        return defaultMapper;
    }
    
    /**
     * 创建默认的ObjectMapper配置
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ========== 基础配置 ==========
        // 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 空对象不抛异常
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略null值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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


        // ========== 关键：启用安全的多态类型处理 ==========
        // 创建类型验证器，限制允许反序列化的类型
//        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
//                // 允许项目内的包
//                .allowIfSubType("com.blink.")
//                // 允许Java标准库中的常见类型
//                .allowIfSubType("java.util.ArrayList")
//                .allowIfSubType("java.util.LinkedHashMap")
//                .allowIfSubType("java.util.HashMap")
//                .allowIfSubType("java.util.HashSet")
//                // 允许数组
//                .allowIfSubTypeIsArray()
//                // 允许基础类型
//                .allowIfBaseType(String.class)
//                .allowIfBaseType(Integer.class)
//                .allowIfBaseType(Long.class)
//                .allowIfBaseType(Boolean.class)
//                .allowIfBaseType(Double.class)
//                .allowIfBaseType(Character.class)
//                .build();
//
//        // 启用默认类型，解决LinkedHashMap转换问题
//        mapper.activateDefaultTyping(ptv,ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY );

        return mapper;
    }
    
    // ================== 基础转换方法 ==================
    
    /**
     * 对象转JSON字符串（最常用）
     */
    public static String toJson(Object obj) {
        return toJson(obj, getDefaultMapper());
    }
    
    /**
     * 对象转JSON字符串（使用指定Mapper）
     */
    public static String toJson(Object obj, ObjectMapper mapper) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            throw new RuntimeException("JSON序列化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 对象转JSON字符串（美化格式）
     */
    public static String toPrettyJson(Object obj) {
        try {
            return getDefaultMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转美化JSON失败", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return getDefaultMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败, JSON: {}, 目标类: {}", json, clazz, e);
            throw new RuntimeException("JSON反序列化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * JSON字符串转对象（支持复杂泛型）
     * 示例：List<User> list = fromJson(json, new TypeReference<List<User>>() {});
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return getDefaultMapper().readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON转复杂对象失败", e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }
    
    /**
     * JSON字符串转List
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = getDefaultMapper().getTypeFactory()
                    .constructCollectionType(List.class, clazz);
            return getDefaultMapper().readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败", e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }
    
    /**
     * JSON字符串转Map
     */
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            JavaType javaType = getDefaultMapper().getTypeFactory()
                    .constructMapType(Map.class, keyClass, valueClass);
            return getDefaultMapper().readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败", e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }
    
    // ================== Map相关转换 ==================
    
    /**
     * 对象转Map
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        return getDefaultMapper().convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Map转对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return getDefaultMapper().convertValue(map, clazz);
    }
    
    /**
     * 深度转换：Map转对象（支持嵌套）
     */
    public static <T> T deepConvert(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        // 先转为JSON字符串，再转为对象，确保嵌套对象能正确转换
        String json = toJson(map);
        return fromJson(json, clazz);
    }
    
    // ================== 类型安全转换 ==================
    
    /**
     * 安全转换：不会抛异常，失败返回null
     */
    public static <T> T safeFromJson(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.debug("安全转换失败，返回null", e);
            return null;
        }
    }
    
    /**
     * 安全转换：带默认值
     */
    public static <T> T safeFromJson(String json, Class<T> clazz, T defaultValue) {
        try {
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.debug("安全转换失败，返回默认值", e);
            return defaultValue;
        }
    }
    
    // ================== 特殊格式处理 ==================
    
    /**
     * 处理包含转义字符的JSON字符串
     * 示例：输入 "\"{\\\"name\\\":\\\"张三\\\"}\"" 输出 {"name":"张三"}
     */
    public static String unescapeJson(String escapedJson) {
        if (!StringUtils.hasText(escapedJson)) {
            return escapedJson;
        }
        
        String cleaned = escapedJson.trim();
        
        // 去除外层引号
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        // 反转义字符
        cleaned = cleaned.replace("\\\"", "\"")
                         .replace("\\\\", "\\")
                         .replace("\\n", "\n")
                         .replace("\\t", "\t")
                         .replace("\\r", "\r");
        
        return cleaned;
    }
    
    /**
     * 解析混乱的JSON字符串（自动清理转义字符）
     */
    public static <T> T parseMessyJson(String messyJson, Class<T> clazz) {
        if (!StringUtils.hasText(messyJson)) {
            return null;
        }
        
        String cleaned = unescapeJson(messyJson);
        return fromJson(cleaned, clazz);
    }
    
    /**
     * 将不规范JSON转为标准JSON
     */
    public static String normalizeJson(String json) {
        try {
            // 尝试解析并重新序列化
            JsonNode node = getDefaultMapper().readTree(json);
            return toJson(node);
        } catch (Exception e) {
            // 如果失败，尝试清理
            return unescapeJson(json);
        }
    }
    
    // ================== 节点操作 ==================
    
    /**
     * 获取JSON节点（用于动态操作JSON）
     */
    public static JsonNode readTree(String json) {
        try {
            return getDefaultMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("解析JSON节点失败", e);
            throw new RuntimeException("JSON解析失败", e);
        }
    }
    
    /**
     * 从JSON中提取字段值
     */
    public static String extractString(String json, String fieldPath) {
        try {
            JsonNode node = readTree(json);
            JsonNode target = node.at(fieldPath); // 支持JSON Path，如 "/user/name"
            return target.asText();
        } catch (Exception e) {
            log.debug("提取字段失败: {}", fieldPath, e);
            return null;
        }
    }
    
    /**
     * 修改JSON中的字段值
     */
    public static String updateJsonField(String json, String fieldPath, Object newValue) {
        try {
            JsonNode root = readTree(json);
            ((com.fasterxml.jackson.databind.node.ObjectNode) root.at(fieldPath.substring(0, fieldPath.lastIndexOf('/'))))
                    .put(fieldPath.substring(fieldPath.lastIndexOf('/') + 1), toJson(newValue));
            return toJson(root);
        } catch (Exception e) {
            log.error("更新JSON字段失败", e);
            return json;
        }
    }
    
    // ================== 高级功能 ==================
    
    /**
     * 对象转换（支持不同类型间的转换）
     * 如：Map转User对象，User转UserDTO等
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return getDefaultMapper().convertValue(source, targetClass);
    }
    
    /**
     * 对象转换（支持复杂泛型）
     */
    public static <T> T convert(Object source, TypeReference<T> typeReference) {
        if (source == null) {
            return null;
        }
        return getDefaultMapper().convertValue(source, typeReference);
    }
    
    /**
     * 复制对象属性（深度拷贝）
     */
    public static <T> T deepCopy(T source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        String json = toJson(source);
        return fromJson(json, clazz);
    }
    
    /**
     * 合并两个对象（target会覆盖source中同名字段的值）
     */
    public static <T> T merge(T source, T target, Class<T> clazz) {
        if (source == null) return target;
        if (target == null) return source;
        
        Map<String, Object> sourceMap = toMap(source);
        Map<String, Object> targetMap = toMap(target);
        
        // 合并Map（target覆盖source）
        sourceMap.putAll(targetMap);
        
        return fromMap(sourceMap, clazz);
    }
    
    // ================== 验证与判断 ==================
    
    /**
     * 判断字符串是否是有效的JSON
     */
    public static boolean isValidJson(String json) {
        if (!StringUtils.hasText(json)) {
            return false;
        }
        try {
            getDefaultMapper().readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断字符串是否是JSON对象
     */
    public static boolean isJsonObject(String json) {
        if (!isValidJson(json)) {
            return false;
        }
        try {
            JsonNode node = getDefaultMapper().readTree(json);
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断字符串是否是JSON数组
     */
    public static boolean isJsonArray(String json) {
        if (!isValidJson(json)) {
            return false;
        }
        try {
            JsonNode node = getDefaultMapper().readTree(json);
            return node.isArray();
        } catch (Exception e) {
            return false;
        }
    }
    
    // ================== 批量操作 ==================
    
    /**
     * 批量转换：List<Object> 转 JSON字符串
     */
    public static String batchToJson(List<?> objects) {
        if (objects == null || objects.isEmpty()) {
            return "[]";
        }
        return toJson(objects);
    }
    
    /**
     * 批量转换：JSON数组字符串 转 List<Map>
     */
//    public static List<Map<String, Object>> batchJsonToMapList(String jsonArray) {
//        return safeFromJson(jsonArray, new TypeReference<List<Map<String, Object>>>() {});
//    }
    
    /**
     * 批量转换：List<Map> 转 List<Object>
     */
    public static <T> List<T> batchMapToObject(List<Map<String, Object>> mapList, Class<T> clazz) {
        if (mapList == null || mapList.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<T> result = new ArrayList<>(mapList.size());
        for (Map<String, Object> map : mapList) {
            result.add(fromMap(map, clazz));
        }
        return result;
    }
    
    // ================== 性能优化版本 ==================
    
    /**
     * 高性能转换：使用预编译的TypeReference
     */
    public static class FastConverter {
        private static final Map<Class<?>, TypeReference<?>> TYPE_CACHE = new ConcurrentHashMap<>();
        
        @SuppressWarnings("unchecked")
        public static <T> T fastFromJson(String json, Class<T> clazz) {
            try {
                TypeReference<T> typeRef = (TypeReference<T>) TYPE_CACHE.computeIfAbsent(
                    clazz, k -> new TypeReference<T>() {}
                );
                return getDefaultMapper().readValue(json, typeRef);
            } catch (Exception e) {
                log.error("快速转换失败", e);
                return null;
            }
        }
    }
    

}