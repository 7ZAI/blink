package com.blink.gateway.admin.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 过滤器配置类
 * 用于 JSON 序列化/反序列化 filters 字段
 *
 * @author binblink
 * @since 2026-04-11
 */
@Getter
@Setter
public class FilterConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 过滤器名称
     * 如 StripPrefix、AddRequestHeader、RewritePath 等
     */
    private String name;

    /**
     * 过滤器参数
     * key-value 形式的参数配置
     */
    private Map<String, String> args = new LinkedHashMap<>();

    public FilterConfig() {
    }

    public FilterConfig(String name) {
        this.name = name;
    }

    public FilterConfig(String name, Map<String, String> args) {
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