package com.blink.framework.test.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.util.List;

/**
 * JSON 测试辅助工具
 * 用于测试中的 JSON 序列化/反序列化和断言
 *
 * @author binblink
 * @since 2026-04-16
 */
public class JsonTestHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 对象转格式化 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return 格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    /**
     * JSON 字符串转 JsonNode（用于灵活解析）
     *
     * @param json JSON 字符串
     * @return JsonNode 对象
     */
    public static JsonNode parseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    /**
     * 使用 JsonPath 提取值
     *
     * @param json JSON 字符串
     * @param path JsonPath 表达式
     * @return 提取的值
     */
    public static <T> T extract(String json, String path) {
        ReadContext context = JsonPath.parse(json);
        return context.read(path);
    }

    /**
     * 使用 JsonPath 提取列表
     *
     * @param json JSON 字符串
     * @param path JsonPath 表达式
     * @return 提取的列表
     */
    public static <T> List<T> extractList(String json, String path) {
        ReadContext context = JsonPath.parse(json);
        return context.read(path, List.class);
    }

    /**
     * 提取 JSON 中的 msgCode（Blink 项目常用）
     *
     * @param json JSON 字符串
     * @return msgCode 值
     */
    public static String extractMsgCode(String json) {
        return extract(json, "$.msgCode");
    }

    /**
     * 提取 JSON 中的 msg（Blink 项目常用）
     *
     * @param json JSON 字符串
     * @return msg 值
     */
    public static String extractMsg(String json) {
        return extract(json, "$.msg");
    }

    /**
     * 提取 JSON 中的 body（Blink 项目常用）
     *
     * @param json JSON 字符串
     * @return body JsonNode
     */
    public static JsonNode extractBody(String json) {
        return parseJson(json).get("body");
    }

    /**
     * 比较 JSON 结构是否相等（忽略字段顺序）
     *
     * @param json1 第一个 JSON
     * @param json2 第二个 JSON
     * @return 是否相等
     */
    public static boolean jsonEquals(String json1, String json2) {
        try {
            JsonNode node1 = MAPPER.readTree(json1);
            JsonNode node2 = MAPPER.readTree(json2);
            return node1.equals(node2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 JSON 是否包含指定字段
     *
     * @param json  JSON 字符串
     * @param field 字段名
     * @return 是否包含
     */
    public static boolean hasField(String json, String field) {
        JsonNode node = parseJson(json);
        return node.has(field);
    }

    /**
     * 检查 JSON body 中是否包含指定字段
     *
     * @param json  JSON 字符串
     * @param field 字段名
     * @return 是否包含
     */
    public static boolean bodyHasField(String json, String field) {
        JsonNode body = extractBody(json);
        return body != null && body.has(field);
    }
}