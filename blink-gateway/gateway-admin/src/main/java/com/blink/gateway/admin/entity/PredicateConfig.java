package com.blink.gateway.admin.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 断言配置类
 * 用于 JSON 序列化/反序列化 predicates 字段
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
public class PredicateConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 断言名称
     * 如 Path、Method、Header、Query 等
     */
    private String name;

    /**
     * 断言参数
     * key-value 形式的参数配置
     */
    private Map<String, String> args = new LinkedHashMap<>();

    public PredicateConfig() {
    }

    public PredicateConfig(String name) {
        this.name = name;
    }

    public PredicateConfig(String name, Map<String, String> args) {
        this.name = name;
        this.args = args;
    }

    /**
     * 添加参数
     *
     * @param key   参数键
     * @param value 参数值
     */
    public void addArg(String key, String value) {
        this.args.put(key, value);
    }
}